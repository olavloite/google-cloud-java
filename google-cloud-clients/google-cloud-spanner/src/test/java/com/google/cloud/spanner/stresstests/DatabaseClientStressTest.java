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

package com.google.cloud.spanner.stresstests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.api.gax.grpc.testing.MockServiceHelper;
import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.StressTest;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.common.base.Function;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Stress test for {@link DatabaseClient}. This test uses the {@link MockSpannerServiceImpl} as a
 * server. This is a mock implementation of a Cloud Spanner server that is started in-process.
 */
@Category(StressTest.class)
@RunWith(JUnit4.class)
public class DatabaseClientStressTest {
  private static final String PROJECT_ID = "test-project";
  private static final String INSTANCE_ID = "test-instance";
  private static final String DATABASE_ID = "test-database";
  private static final long MAX_WAIT_FOR_TEST_RUN = 60000L;
  private static final int NUMBER_OF_SELECT1_TEST_EXECUTIONS = 100000;
  private static final int NUMBER_OF_RANDOM_TEST_EXECUTIONS = 1000;
  private static final int NUMBER_OF_UPDATE_TEST_EXECUTIONS = 100000;
  private static final int NUMBER_OF_PARALLEL_THREADS = 64;
  private static final int NUMBER_OF_QUERIES_IN_TX = 100;
  private static final int NUMBER_OF_UPDATES_IN_TX = 500;
  private static final int ROW_COUNT_RANDOM_RESULT_SET = 1000;

  private static final Statement SELECT1 = Statement.of("SELECT 1 AS COL1");
  private static final ResultSetMetadata SELECT1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("1").build())
                  .build())
          .setMetadata(SELECT1_METADATA)
          .build();
  private static final Statement SELECT_RANDOM = Statement.of("SELECT * FROM RANDOM");
  private static final com.google.spanner.v1.ResultSet RANDOM_RESULT_SET =
      new RandomResultSetGenerator(ROW_COUNT_RANDOM_RESULT_SET).generate();
  private static final Statement SELECT_RANDOM_WITH_PARAMS =
      Statement.newBuilder("SELECT * FROM RANDOM WHERE FOO=@param1 AND BAR=@param2")
          .bind("param1")
          .to(1L)
          .bind("param2")
          .to("TEST")
          .build();
  private static final com.google.spanner.v1.ResultSet RANDOM_WITH_PARAMS_RESULT_SET =
      new RandomResultSetGenerator(ROW_COUNT_RANDOM_RESULT_SET).generate();

  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static final Mutation MUTATION =
      Mutation.newInsertBuilder("FOO")
          .set("COL1")
          .to(Boolean.TRUE)
          .set("COL2")
          .to(ByteArray.copyFrom(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}))
          .set("COL3")
          .to(Date.fromJavaUtilDate(new java.util.Date()))
          .set("COL4")
          .to(Math.PI)
          .set("COL5")
          .to(1L)
          .set("COL6")
          .to("TEST")
          .set("COL7")
          .to(Timestamp.of(new java.sql.Timestamp(System.currentTimeMillis())))
          .set("COL8")
          .toBoolArray(Arrays.asList(true, false, true))
          .set("COL9")
          .toBytesArray(
              Arrays.asList(
                  ByteArray.copyFrom(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}),
                  ByteArray.copyFrom(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}),
                  ByteArray.copyFrom(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})))
          .set("COL10")
          .toDateArray(
              Arrays.asList(
                  Date.fromJavaUtilDate(new java.util.Date()),
                  Date.fromJavaUtilDate(new java.util.Date()),
                  Date.fromJavaUtilDate(new java.util.Date())))
          .set("COL11")
          .toFloat64Array(Arrays.asList(Math.PI, Math.PI, Math.PI))
          .set("COL12")
          .toInt64Array(Arrays.asList(1L, 2L, 3L))
          .set("COL13")
          .toStringArray(Arrays.asList("TEST", "TEST", "TEST"))
          .set("COL14")
          .toTimestampArray(
              Arrays.asList(
                  Timestamp.of(new java.sql.Timestamp(System.currentTimeMillis())),
                  Timestamp.of(new java.sql.Timestamp(System.currentTimeMillis())),
                  Timestamp.of(new java.sql.Timestamp(System.currentTimeMillis()))))
          .build();
  private static final List<Mutation> MUTATIONS = Arrays.asList(MUTATION, MUTATION, MUTATION);

  private static MockSpannerServiceImpl mockSpanner;
  private static MockServiceHelper serviceHelper;
  private SpannerClient client;
  private LocalChannelProvider channelProvider;
  private Spanner spanner;

  @BeforeClass
  public static void startStaticServer() {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(SELECT_RANDOM, RANDOM_RESULT_SET));
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT_RANDOM_WITH_PARAMS, RANDOM_WITH_PARAMS_RESULT_SET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    serviceHelper =
        new MockServiceHelper("in-process-1", Arrays.<MockGrpcService>asList(mockSpanner));
    serviceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    serviceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    serviceHelper.reset();
    channelProvider = serviceHelper.createChannelProvider();
    SpannerSettings settings =
        SpannerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    client = SpannerClient.create(settings);
    spanner =
        SpannerOptions.newBuilder()
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  @Test
  public void singleUseSelect1Test()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifySelect1ResultSet(input);
            return null;
          }
        },
        NUMBER_OF_SELECT1_TEST_EXECUTIONS);
  }

  @Test
  public void singleUseRandomTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS);
  }

  @Test
  public void singleUseRandomWithParamsTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomWithParamsResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS);
  }

  private <T> void singleUseTest(final Function<ReadContext, T> verifyFunction, int testRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try (ReadContext context = dbClient.singleUse()) {
              verifyFunction.apply(context);
            }
            return null;
          }
        };
    runStressTest(callable, testRuns);
  }

  @Test
  public void singleUseReadOnlyTransactionSelect1Test()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseReadOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifySelect1ResultSet(input);
            return null;
          }
        },
        NUMBER_OF_SELECT1_TEST_EXECUTIONS);
  }

  @Test
  public void singleUseReadOnlyTransactionRandomTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseReadOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS);
  }

  @Test
  public void singleUseReadOnlyTransactionRandomWithParamsTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    singleUseReadOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomWithParamsResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS);
  }

  private <T> void singleUseReadOnlyTransactionTest(
      final Function<ReadContext, T> verifyFunction, int testRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try (ReadOnlyTransaction context = dbClient.singleUseReadOnlyTransaction()) {
              verifyFunction.apply(context);
              assertThat(context.getReadTimestamp(), is(notNullValue()));
            }
            return null;
          }
        };
    runStressTest(callable, testRuns);
  }

  @Test
  public void readOnlyTransactionSelect1Test()
      throws InterruptedException, ExecutionException, TimeoutException {
    readOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifySelect1ResultSet(input);
            return null;
          }
        },
        NUMBER_OF_SELECT1_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void readOnlyTransactionRandomTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    readOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void readOnlyTransactionRandomWithParamsTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    readOnlyTransactionTest(
        new Function<ReadContext, Void>() {
          @Override
          public Void apply(ReadContext input) {
            verifyRandomWithParamsResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  private <T> void readOnlyTransactionTest(
      final Function<ReadContext, T> verifyFunction, int testRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try (ReadOnlyTransaction context = dbClient.readOnlyTransaction()) {
              for (int i = 0; i < NUMBER_OF_QUERIES_IN_TX; i++) {
                verifyFunction.apply(context);
                assertThat(context.getReadTimestamp(), is(notNullValue()));
              }
            }
            return null;
          }
        };
    runStressTest(callable, testRuns);
  }

  @Test
  public void readWriteTransactionSelect1Test()
      throws InterruptedException, ExecutionException, TimeoutException {
    readWriteTransactionTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifySelect1ResultSet(input);
            return null;
          }
        },
        NUMBER_OF_SELECT1_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void readWriteTransactionRandomTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    readWriteTransactionTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifyRandomResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void readWriteTransactionRandomWithParamsTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    readWriteTransactionTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifyRandomWithParamsResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  private <T> void readWriteTransactionTest(
      final Function<TransactionContext, T> verifyFunction, int testRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            TransactionRunner runner = dbClient.readWriteTransaction();
            Void res =
                runner.run(
                    new TransactionCallable<Void>() {
                      @Override
                      public Void run(TransactionContext transaction) throws Exception {
                        for (int i = 0; i < NUMBER_OF_QUERIES_IN_TX; i++) {
                          verifyFunction.apply(transaction);
                        }
                        return null;
                      }
                    });
            assertThat(runner.getCommitTimestamp(), is(notNullValue()));
            return res;
          }
        };
    runStressTest(callable, testRuns);
  }

  @Test
  public void transactionManagerSelect1Test()
      throws InterruptedException, ExecutionException, TimeoutException {
    transactionManagerTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifySelect1ResultSet(input);
            return null;
          }
        },
        NUMBER_OF_SELECT1_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void transactionManagerRandomTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    transactionManagerTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifyRandomResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  @Test
  public void transactionManagerRandomWithParamsTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    transactionManagerTest(
        new Function<TransactionContext, Void>() {
          @Override
          public Void apply(TransactionContext input) {
            verifyRandomWithParamsResultSet(input);
            return null;
          }
        },
        NUMBER_OF_RANDOM_TEST_EXECUTIONS / NUMBER_OF_QUERIES_IN_TX);
  }

  private <T> void transactionManagerTest(
      final Function<TransactionContext, T> verifyFunction, int testRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try (TransactionManager manager = dbClient.transactionManager()) {
              TransactionContext txn = manager.begin();
              while (true) {
                try {
                  for (int i = 0; i < NUMBER_OF_QUERIES_IN_TX; i++) {
                    verifyFunction.apply(txn);
                  }
                  manager.commit();
                  break;
                } catch (AbortedException e) {
                  Thread.sleep(e.getRetryDelayInMillis() / 1000);
                  txn = manager.resetForRetry();
                }
              }
              assertThat(manager.getCommitTimestamp(), is(notNullValue()));
            }
            return null;
          }
        };
    runStressTest(callable, testRuns);
  }

  @Test
  public void readWriteTransactionUpdateTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            TransactionRunner runner = dbClient.readWriteTransaction();
            Void res =
                runner.run(
                    new TransactionCallable<Void>() {
                      @Override
                      public Void run(TransactionContext transaction) throws Exception {
                        for (int i = 0; i < NUMBER_OF_UPDATES_IN_TX; i++) {
                          assertThat(
                              transaction.executeUpdate(UPDATE_STATEMENT),
                              is(equalTo(UPDATE_COUNT)));
                        }
                        return null;
                      }
                    });
            assertThat(runner.getCommitTimestamp(), is(notNullValue()));
            return res;
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS / NUMBER_OF_UPDATES_IN_TX);
  }

  @Test
  public void transactionManagerUpdateTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          @SuppressWarnings("resource")
          public Void call() throws Exception {
            try (TransactionManager manager = dbClient.transactionManager()) {
              TransactionContext txn = manager.begin();
              while (true) {
                try {
                  for (int i = 0; i < NUMBER_OF_UPDATES_IN_TX; i++) {
                    assertThat(txn.executeUpdate(UPDATE_STATEMENT), is(equalTo(UPDATE_COUNT)));
                  }
                  manager.commit();
                  break;
                } catch (AbortedException e) {
                  Thread.sleep(e.getRetryDelayInMillis() / 1000);
                  txn = manager.resetForRetry();
                }
              }
              assertThat(manager.getCommitTimestamp(), is(notNullValue()));
            }
            return null;
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS / NUMBER_OF_UPDATES_IN_TX);
  }

  /**
   * This test will leak (read) sessions as neither the {@link ResultSet} or the {@link ReadContext}
   * is closed by the user. The {@link ResultSet} is also not consumed in full, and this will cause
   * the session pool to think that the sessions are still in use, and will eventually cause the
   * call threads to become block indefinitely as no read session can be acquired from the pool.
   *
   * @throws TimeoutException
   */
  @Test(expected = TimeoutException.class)
  public void unclosedSingleUseTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Long> callable =
        new Callable<Long>() {
          @Override
          public Long call() throws Exception {
            // This ResultSet is not fully consumed and not closed, leaking a session from the pool.
            ResultSet rs = dbClient.singleUse().executeQuery(SELECT1);
            rs.next();
            return rs.getLong(0);
          }
        };
    runStressTest(callable, NUMBER_OF_SELECT1_TEST_EXECUTIONS);
  }

  @Test
  public void readWriteTransactionBatchUpdateTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            TransactionRunner runner = dbClient.readWriteTransaction();
            Void res =
                runner.run(
                    new TransactionCallable<Void>() {
                      @Override
                      public Void run(TransactionContext transaction) throws Exception {
                        for (int i = 0; i < NUMBER_OF_UPDATES_IN_TX; i++) {
                          assertThat(
                              transaction.batchUpdate(
                                  Arrays.asList(
                                      UPDATE_STATEMENT, UPDATE_STATEMENT, UPDATE_STATEMENT)),
                              is(equalTo(new long[] {UPDATE_COUNT, UPDATE_COUNT, UPDATE_COUNT})));
                        }
                        return null;
                      }
                    });
            assertThat(runner.getCommitTimestamp(), is(notNullValue()));
            return res;
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS / NUMBER_OF_UPDATES_IN_TX);
  }

  @Test
  public void transactionManagerBatchUpdateTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Void> callable =
        new Callable<Void>() {
          @Override
          @SuppressWarnings("resource")
          public Void call() throws Exception {
            try (TransactionManager manager = dbClient.transactionManager()) {
              TransactionContext txn = manager.begin();
              while (true) {
                try {
                  for (int i = 0; i < NUMBER_OF_UPDATES_IN_TX; i++) {
                    assertThat(
                        txn.batchUpdate(
                            Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT, UPDATE_STATEMENT)),
                        is(equalTo(new long[] {UPDATE_COUNT, UPDATE_COUNT, UPDATE_COUNT})));
                  }
                  manager.commit();
                  break;
                } catch (AbortedException e) {
                  Thread.sleep(e.getRetryDelayInMillis() / 1000);
                  txn = manager.resetForRetry();
                }
              }
              assertThat(manager.getCommitTimestamp(), is(notNullValue()));
            }
            return null;
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS / NUMBER_OF_UPDATES_IN_TX);
  }

  @Test
  public void partitionedUpdateTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Long> callable =
        new Callable<Long>() {
          @Override
          public Long call() throws Exception {
            return dbClient.executePartitionedUpdate(UPDATE_STATEMENT);
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS);
  }

  @Test
  public void writeTest() throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Timestamp> callable =
        new Callable<Timestamp>() {
          @Override
          public Timestamp call() throws Exception {
            return dbClient.write(MUTATIONS);
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS);
  }

  @Test
  public void writeAtLeastOnceTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    final DatabaseClient dbClient =
        spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
    Callable<Timestamp> callable =
        new Callable<Timestamp>() {
          @Override
          public Timestamp call() throws Exception {
            return dbClient.writeAtLeastOnce(MUTATIONS);
          }
        };
    runStressTest(callable, NUMBER_OF_UPDATE_TEST_EXECUTIONS);
  }

  private void verifySelect1ResultSet(ReadContext context) {
    try (ResultSet rs = context.executeQuery(SELECT1)) {
      assertThat(rs, is(notNullValue()));
      assertThat(rs.next(), is(true));
      assertThat(rs.getLong(0), is(equalTo(1L)));
      assertThat(rs.next(), is(false));
    }
  }

  private void verifyRandomResultSet(ReadContext context) {
    try (ResultSet rs = context.executeQuery(SELECT_RANDOM)) {
      int rowCount = 0;
      while (rs.next()) {
        for (int col = 0; col < rs.getColumnCount(); col++) {
          assertThat(getValue(rs, col), is(notNullValue()));
        }
        rowCount++;
      }
      assertThat(rowCount, is(equalTo(ROW_COUNT_RANDOM_RESULT_SET)));
    }
  }

  private void verifyRandomWithParamsResultSet(ReadContext context) {
    try (ResultSet rs = context.executeQuery(SELECT_RANDOM_WITH_PARAMS)) {
      int rowCount = 0;
      while (rs.next()) {
        for (int col = 0; col < rs.getColumnCount(); col++) {
          assertThat(getValue(rs, col), is(notNullValue()));
        }
        rowCount++;
      }
      assertThat(rowCount, is(equalTo(ROW_COUNT_RANDOM_RESULT_SET)));
    }
  }

  private Object getValue(ResultSet rs, int col) {
    if (rs.isNull(col)) {
      return Empty.getDefaultInstance();
    }
    switch (rs.getColumnType(col).getCode()) {
      case ARRAY:
        switch (rs.getColumnType(col).getArrayElementType().getCode()) {
          case BOOL:
            return rs.getBooleanList(col);
          case BYTES:
            return rs.getBytesList(col);
          case DATE:
            return rs.getDateList(col);
          case FLOAT64:
            return rs.getDoubleList(col);
          case INT64:
            return rs.getLongList(col);
          case STRING:
            return rs.getStringList(col);
          case TIMESTAMP:
            return rs.getTimestampList(col);
          case STRUCT:
          case ARRAY:
          default:
            break;
        }
        break;
      case BOOL:
        return rs.getBoolean(col);
      case BYTES:
        return rs.getBytes(col);
      case DATE:
        return rs.getDate(col);
      case FLOAT64:
        return rs.getDouble(col);
      case INT64:
        return rs.getLong(col);
      case STRING:
        return rs.getString(col);
      case TIMESTAMP:
        return rs.getTimestamp(col);
      case STRUCT:
      default:
        break;
    }
    throw new IllegalArgumentException("Unknown or unsupported type");
  }

  private <T> List<T> runStressTest(Callable<T> callable, int numberOfRuns)
      throws InterruptedException, ExecutionException, TimeoutException {
    ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(NUMBER_OF_PARALLEL_THREADS);
    List<Future<T>> futures = new ArrayList<>(numberOfRuns);
    List<T> res = new ArrayList<>(numberOfRuns);
    for (int i = 0; i < numberOfRuns; i++) {
      futures.add(executor.submit(callable));
    }
    executor.shutdown();
    if (executor.awaitTermination(MAX_WAIT_FOR_TEST_RUN, TimeUnit.MILLISECONDS)) {
      for (Future<T> future : futures) {
        res.add(future.get());
      }
      assertThat((int) executor.getCompletedTaskCount(), is(equalTo(numberOfRuns)));
      assertThat(
          executor.getLargestPoolSize(),
          is(equalTo(Math.min(NUMBER_OF_PARALLEL_THREADS, numberOfRuns))));
      return res;
    } else {
      throw new TimeoutException("Stress test run timed out");
    }
  }
}
