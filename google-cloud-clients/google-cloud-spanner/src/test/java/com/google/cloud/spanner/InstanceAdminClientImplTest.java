/*
 * Copyright 2017 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.admin.instance.v1.MockInstanceAdmin;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Paginated;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.longrunning.MockOperations;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.InstanceName;
import com.google.spanner.admin.instance.v1.UpdateInstanceMetadata;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.threeten.bp.Duration;

/** Unit tests for {@link com.google.cloud.spanner.SpannerImpl.InstanceAdminClientImpl}. */
@RunWith(JUnit4.class)
public class InstanceAdminClientImplTest {
  private static final String PROJECT_ID = "my-project";
  private static final String INSTANCE_ID = "my-instance";
  private static final String INSTANCE_NAME = "projects/my-project/instances/my-instance";
  private static final String INSTANCE_NAME2 = "projects/my-project/instances/my-instance2";
  private static final String CONFIG_ID = "my-config";
  private static final String CONFIG_NAME = "projects/my-project/instanceConfigs/my-config";
  private static final String CONFIG_NAME2 = "projects/my-project/instanceConfigs/my-config2";

  @Mock SpannerRpc rpc;
  @Mock DatabaseAdminClient dbClient;
  InstanceAdminClientImpl client;
  private static MockInstanceAdmin mockInstanceAdmin;
  private static MockOperations mockOperations;
  private static Server server;
  private static LocalChannelProvider channelProvider;

  @Before
  public void setUp() {
    initMocks(this);
    client = new InstanceAdminClientImpl(PROJECT_ID, rpc, dbClient);
  }

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockInstanceAdmin = new MockInstanceAdmin();
    mockOperations = new MockOperations();
    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockInstanceAdmin.getServiceDefinition())
            .addService(mockOperations.getServiceDefinition())
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void setUpMockServer() throws IOException {
    mockInstanceAdmin.reset();
    mockOperations.reset();
  }

  @Test
  public void getInstanceConfig() {
    when(rpc.getInstanceConfig(CONFIG_NAME))
        .thenReturn(InstanceConfig.newBuilder().setName(CONFIG_NAME).build());
    assertThat(client.getInstanceConfig(CONFIG_ID).getId().getName()).isEqualTo(CONFIG_NAME);
  }

  @Test
  public void listInstanceConfigs() {
    String nextToken = "token";
    when(rpc.listInstanceConfigs(1, null))
        .thenReturn(
            new Paginated<InstanceConfig>(
                ImmutableList.of(InstanceConfig.newBuilder().setName(CONFIG_NAME).build()),
                nextToken));
    when(rpc.listInstanceConfigs(1, nextToken))
        .thenReturn(
            new Paginated<InstanceConfig>(
                ImmutableList.of(InstanceConfig.newBuilder().setName(CONFIG_NAME2).build()), ""));
    List<com.google.cloud.spanner.InstanceConfig> configs =
        Lists.newArrayList(client.listInstanceConfigs(Options.pageSize(1)).iterateAll());
    assertThat(configs.get(0).getId().getName()).isEqualTo(CONFIG_NAME);
    assertThat(configs.get(1).getId().getName()).isEqualTo(CONFIG_NAME2);
    assertThat(configs.size()).isEqualTo(2);
  }

  private com.google.spanner.admin.instance.v1.Instance getInstanceProto() {
    return com.google.spanner.admin.instance.v1.Instance.newBuilder()
        .setConfig(CONFIG_NAME)
        .setName(INSTANCE_NAME)
        .setNodeCount(1)
        .build();
  }

  private com.google.spanner.admin.instance.v1.Instance getAnotherInstanceProto() {
    return com.google.spanner.admin.instance.v1.Instance.newBuilder()
        .setConfig(CONFIG_NAME)
        .setName(INSTANCE_NAME2)
        .setNodeCount(1)
        .build();
  }

  @Test
  public void createInstance() throws Exception {
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, CreateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "createInstance", getInstanceProto(), CreateInstanceMetadata.getDefaultInstance());
    when(rpc.createInstance("projects/" + PROJECT_ID, INSTANCE_ID, getInstanceProto()))
        .thenReturn(rawOperationFuture);
    OperationFuture<Instance, CreateInstanceMetadata> op =
        client.createInstance(
            InstanceInfo.newBuilder(InstanceId.of(PROJECT_ID, INSTANCE_ID))
                .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, CONFIG_ID))
                .setNodeCount(1)
                .build());
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(INSTANCE_NAME);
  }

  @Test
  public void createInstanceTest() throws Exception {
    InstanceName name = InstanceName.of("[PROJECT]", "[INSTANCE]");
    InstanceConfigName config = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]");
    String displayName = "displayName1615086568";
    int nodeCount = 1539922066;
    com.google.spanner.admin.instance.v1.Instance expectedResponse =
        com.google.spanner.admin.instance.v1.Instance.newBuilder()
            .setName(name.toString())
            .setConfig(config.toString())
            .setDisplayName(displayName)
            .setNodeCount(nodeCount)
            .build();
    com.google.longrunning.Operation resultOperation =
        com.google.longrunning.Operation.newBuilder()
            .setName("createInstanceTest")
            .setDone(false)
            .setResponse(Any.getDefaultInstance())
            .build();
    com.google.longrunning.Operation resultOperationFinished =
        com.google.longrunning.Operation.newBuilder()
            .setName("createInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockInstanceAdmin.addResponse(resultOperation);
    mockOperations.addResponse(resultOperation);
    mockOperations.addResponse(resultOperationFinished);

    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    builder
        .getInstanceAdminStubSettingsBuilder()
        .createInstanceOperationSettings()
        .setPollingAlgorithm(
            OperationTimedPollAlgorithm.create(
                RetrySettings.newBuilder()
                    .setInitialRetryDelay(Duration.ofSeconds(1L))
                    .setMaxRetryDelay(Duration.ofSeconds(45L))
                    .setRetryDelayMultiplier(2)
                    .setTotalTimeout(Duration.ofHours(24L))
                    .build()));

    try (Spanner spanner = builder.build().getService()) {
      com.google.cloud.spanner.InstanceAdminClient client = spanner.getInstanceAdminClient();
      OperationFuture<Instance, CreateInstanceMetadata> op =
          client.createInstance(
              InstanceInfo.newBuilder(InstanceId.of("[PROJECT]", "[INSTANCE]")).build());
      Instance instance = op.get();
      assertThat(instance).isNotNull();
      assertThat(instance.getDisplayName()).isEqualTo(displayName);
    }
  }

  @Test
  public void getInstance() {
    when(rpc.getInstance(INSTANCE_NAME)).thenReturn(getInstanceProto());
    assertThat(client.getInstance(INSTANCE_ID).getId().getName()).isEqualTo(INSTANCE_NAME);
  }

  @Test
  public void dropInstance() {
    client.deleteInstance(INSTANCE_ID);
    verify(rpc).deleteInstance(INSTANCE_NAME);
  }

  @Test
  public void updateInstanceMetadata() throws Exception {
    com.google.spanner.admin.instance.v1.Instance instance =
        com.google.spanner.admin.instance.v1.Instance.newBuilder()
            .setName(INSTANCE_NAME)
            .setConfig(CONFIG_NAME)
            .setNodeCount(2)
            .build();
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, UpdateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "updateInstance", getInstanceProto(), UpdateInstanceMetadata.getDefaultInstance());
    when(rpc.updateInstance(instance, FieldMask.newBuilder().addPaths("node_count").build()))
        .thenReturn(rawOperationFuture);
    InstanceInfo instanceInfo =
        InstanceInfo.newBuilder(InstanceId.of(INSTANCE_NAME))
            .setInstanceConfigId(InstanceConfigId.of(CONFIG_NAME))
            .setNodeCount(2)
            .build();
    OperationFuture<Instance, UpdateInstanceMetadata> op =
        client.updateInstance(instanceInfo, InstanceInfo.InstanceField.NODE_COUNT);
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(INSTANCE_NAME);
  }

  @Test
  public void listInstances() {
    String nextToken = "token";
    String filter = "env:dev";
    when(rpc.listInstances(1, null, filter))
        .thenReturn(
            new Paginated<com.google.spanner.admin.instance.v1.Instance>(
                ImmutableList.of(getInstanceProto()), nextToken));
    when(rpc.listInstances(1, nextToken, filter))
        .thenReturn(
            new Paginated<com.google.spanner.admin.instance.v1.Instance>(
                ImmutableList.of(getAnotherInstanceProto()), ""));
    List<Instance> instances =
        Lists.newArrayList(
            client.listInstances(Options.pageSize(1), Options.filter(filter)).iterateAll());
    assertThat(instances.get(0).getId().getName()).isEqualTo(INSTANCE_NAME);
    assertThat(instances.get(1).getId().getName()).isEqualTo(INSTANCE_NAME2);
    assertThat(instances.size()).isEqualTo(2);
  }
}
