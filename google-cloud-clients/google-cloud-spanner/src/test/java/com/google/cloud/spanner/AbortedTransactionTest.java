/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.api.gax.grpc.testing.MockServiceHelper;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerSettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test case that uses a mock Spanner server for testing the handling of aborted transactions. */
@RunWith(JUnit4.class)
public class AbortedTransactionTest {
  private static final String PROJECT_ID = "test-project";
  private static final String INSTANCE_ID = "test-instance";
  private static final String DATABASE_ID = "test-database";
  private static final String INSERT_DML =
      "INSERT INTO T (k, v) VALUES ('boo1', 1), ('boo2', 2), ('boo3', 3), ('boo4', 4);";
  private static final String UPDATE_DML = "UPDATE T SET T.V = 100 WHERE T.K LIKE 'boo%';";
  private static final String DELETE_DML = "DELETE FROM T WHERE T.K like 'boo%';";

  private static MockSpannerServiceImpl mockSpanner;
  private static MockServiceHelper serviceHelper;
  private static SpannerClient spannerClient;
  private static LocalChannelProvider channelProvider;
  private static Spanner spanner;
  private static DatabaseClient client;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any randomly aborted transactions.
    mockSpanner.putStatementResult(StatementResult.of(Statement.of(INSERT_DML), 4L));
    mockSpanner.putStatementResult(StatementResult.of(Statement.of(UPDATE_DML), 4L));
    mockSpanner.putStatementResult(StatementResult.of(Statement.of(DELETE_DML), 4L));
    serviceHelper =
        new MockServiceHelper("in-process-1", Arrays.<MockGrpcService>asList(mockSpanner));
    serviceHelper.start();

    channelProvider = serviceHelper.createChannelProvider();
    SpannerSettings settings =
        SpannerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    spannerClient = SpannerClient.create(settings);
    spanner =
        SpannerOptions.newBuilder()
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
  }

  @AfterClass
  public static void stopServer() {
    spannerClient.close();
    serviceHelper.stop();
  }

  @Before
  public void setUp() {
    serviceHelper.reset();
  }

  @Test
  public void commit() {
    final AtomicInteger runs = new AtomicInteger();
    final TransactionCallable<Void> callable =
        new TransactionCallable<Void>() {
          @Override
          public Void run(TransactionContext transaction) throws Exception {
            transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
            if (runs.incrementAndGet() == 1) {
              mockSpanner.abortTransaction(transaction);
            }
            return null;
          }
        };
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(callable);
    assertThat(runner.getCommitTimestamp()).isNotNull();
    assertThat(runs.get() == 2);
  }

  @Test
  public void batchUpdate() {
    final AtomicInteger runs = new AtomicInteger();
    final TransactionCallable<long[]> callable =
        new TransactionCallable<long[]>() {
          @Override
          public long[] run(TransactionContext transaction) throws Exception {
            List<Statement> stmts = new ArrayList<>();
            stmts.add(Statement.of(INSERT_DML));
            stmts.add(Statement.of(UPDATE_DML));
            stmts.add(Statement.of(DELETE_DML));
            if (runs.incrementAndGet() == 1) {
              mockSpanner.abortTransaction(transaction);
            }
            return transaction.batchUpdate(stmts);
          }
        };
    TransactionRunner runner = client.readWriteTransaction();
    long[] rowCounts = runner.run(callable);
    assertThat(rowCounts.length).isEqualTo(3);
    for (long rc : rowCounts) {
      assertThat(rc).isEqualTo(4);
    }
    assertThat(runs.get() == 2);
  }

  @Test
  public void executeUpdate() {
    final AtomicInteger runs = new AtomicInteger();
    final TransactionCallable<Long> callable =
        new TransactionCallable<Long>() {
          @Override
          public Long run(TransactionContext transaction) throws Exception {
            if (runs.incrementAndGet() == 1) {
              mockSpanner.abortTransaction(transaction);
            }
            return transaction.executeUpdate(Statement.of(INSERT_DML));
          }
        };
    TransactionRunner runner = client.readWriteTransaction();
    assertThat(runner.run(callable)).isEqualTo(4);
    assertThat(runs.get() == 2);
  }
}
