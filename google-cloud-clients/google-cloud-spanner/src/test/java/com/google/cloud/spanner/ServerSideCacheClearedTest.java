/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.SessionPool.AutoClosingReadContext;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerClient.ListSessionsPagedResponse;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;

@RunWith(Parameterized.class)
public class ServerSideCacheClearedTest {
  private static final long RUN_TIME_MILLIS = 5L * 60L * 1000L;
  private static final double SESSION_CACHE_INVALIDATED_CHANCE = 0.01D;
//  private static final float TPS = 2.0f;
  private static final int TIME_FACTOR = 10;
  private static final int PROCESSES = 100;
  private static final int FIXED_EXECUTION_TIME = 100;
  private static final int RANDOM_EXECUTION_TIME = 100;
  @Rule public ExpectedException expected = ExpectedException.none();

  @Parameter(0)
  public boolean useLifo;

  @Parameter(1)
  public float tps;

  @Parameters(name = "useLifo = {0}, tps = {1}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();
    params.add(new Object[] {false, 0.2f});
    params.add(new Object[] {false, 0.5f});
    params.add(new Object[] {false, 1.0f});
    params.add(new Object[] {false, 1.5f});
    params.add(new Object[] {false, 2.0f});
    params.add(new Object[] {true, 0.2f});
    params.add(new Object[] {true, 0.5f});
    params.add(new Object[] {true, 1.0f});
    params.add(new Object[] {true, 1.5f});
    params.add(new Object[] {true, 2.0f});
    return params;
  }

  private static final Statement SELECT1AND2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");
  private static final ResultSetMetadata SELECT1AND2_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1AND2_METADATA)
          .build();
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static Spanner spanner;
  private static SpannerClient spannerClient;
  private static DatabaseClient client;
  private AtomicInteger numberOfTransactions = new AtomicInteger();

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.setExecuteStreamingSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(FIXED_EXECUTION_TIME / TIME_FACTOR, RANDOM_EXECUTION_TIME / TIME_FACTOR));

    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .directExecutor()
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);

    SpannerSettings settings =
        SpannerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    spannerClient = SpannerClient.create(settings);
  }

  @AfterClass
  public static void stopServer() {
    spannerClient.close();
    server.shutdown();
  }

  @Before
  public void setUp() throws IOException {
    System.out.println("Setting up spanner with lifo: " + useLifo);
    mockSpanner.reset();
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if(useLifo) {
      builder.useLifo();
    }
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(builder.build())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
  }

  @After
  public void tearDown() throws Exception {
    spanner.close();
  }

  private final ConcurrentMap<String, Boolean> cacheInvalidated = new ConcurrentHashMap<>();
  private final AtomicLong invalidatedHit = new AtomicLong();
  private final AtomicBoolean finished = new AtomicBoolean();

  @Test
  public void singleUseSelect() throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(PROCESSES);
    for(int i=0;i<PROCESSES;i++) {
      service.submit(new Runnable() {
        private final Random random = new Random();
        @Override
        public void run() {
          try {
            while(true) {
              int sleep = random.nextInt((int) (2.0 * ((PROCESSES * (1000 / TIME_FACTOR)) / tps - FIXED_EXECUTION_TIME - RANDOM_EXECUTION_TIME / 2)));
              Thread.sleep(sleep);
              numberOfTransactions.incrementAndGet();
              try (ReadContext context = client.singleUseReadOnlyTransaction()) {
                try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
                  while (rs.next()) {
                    @SuppressWarnings({"rawtypes", "resource"})
                    AutoClosingReadContext acrc = (AutoClosingReadContext) context;
                    SessionImpl session = ((AbstractReadContext) acrc.getReadContextDelegate()).session;
                    Boolean invalidated = cacheInvalidated.get(session.getName());
                    if(invalidated != null && invalidated.booleanValue()) {
                      invalidatedHit.incrementAndGet();
                      cacheInvalidated.put(session.getName(), Boolean.FALSE);
                      System.out.println(String.format("cache miss %d/%d (%.2f%%)", invalidatedHit.get(), numberOfTransactions.get(), invalidatedHit.get() * 100.0D / numberOfTransactions.get()));
                    }
                  }
                }
              }
              if(finished.get()) {
                break;
              }
            }
          } catch (InterruptedException e) {
            // just return
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    }
    createCacheInvalidator();
    createKeepAliveThread();
    Thread.sleep(RUN_TIME_MILLIS);
    finished.set(true);
    service.shutdown();
    service.awaitTermination(2L, TimeUnit.SECONDS);
    System.out.println("------------------------------------------");
    System.out.println("LIFO: " + useLifo);
    System.out.println(String.format("TPS: %.2f", tps));
    System.out.println(String.format("cache miss %d/%d (%.2f%%)", invalidatedHit.get(), numberOfTransactions.get(), invalidatedHit.get() * 100.0D / numberOfTransactions.get()));
    System.out.println("------------------------------------------");
    System.out.println("");
  }

  private void createCacheInvalidator() {
    ExecutorService service = Executors.newSingleThreadExecutor();
    service.submit(new Runnable() {
      private final Random random = new Random();
      @Override
      public void run() {
        try {
          while(true) {
            Thread.sleep(1000L / TIME_FACTOR);
            if(finished.get()) {
              break;
            }
            ListSessionsPagedResponse response =
                spannerClient.listSessions("projects/[PROJECT]/instances/[INSTANCE]/databases/[DATABASE]");
            for (com.google.spanner.v1.Session session : response.iterateAll()) {
              if(random.nextDouble() < SESSION_CACHE_INVALIDATED_CHANCE) {
                cacheInvalidated.put(session.getName(), Boolean.TRUE);
              }
            }
          }
        } catch(InterruptedException e) {
          // just return
        }
      }
    });
  }

  private void createKeepAliveThread() {
    ExecutorService service = Executors.newSingleThreadExecutor();
    service.submit(new Runnable() {
      @Override
      public void run() {
        try {
          while(true) {
            int sleep = (int) ((1000 / TIME_FACTOR) / tps - FIXED_EXECUTION_TIME - RANDOM_EXECUTION_TIME / 2) / 10;
            if(sleep <= 0) {
              sleep = 1;
            }
            Thread.sleep(sleep);
            if(finished.get()) {
              break;
            }
            try (ReadContext context = client.singleUseReadOnlyTransaction()) {
              try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
                while (rs.next()) {
                  @SuppressWarnings({"rawtypes", "resource"})
                  AutoClosingReadContext acrc = (AutoClosingReadContext) context;
                  SessionImpl session = ((AbstractReadContext) acrc.getReadContextDelegate()).session;
                  Boolean invalidated = cacheInvalidated.get(session.getName());
                  if(invalidated != null && invalidated.booleanValue()) {
                    cacheInvalidated.put(session.getName(), Boolean.FALSE);
                  }
                }
              }
            }
          }
        } catch(InterruptedException e) {
          // just return
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
