package energy.eddie.aiida.adapters.datasource.at;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Also tests that authentication works with username/password
 */
@Testcontainers
class OesterreichsEnergieAdapterIntegrationTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("9211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "testPassword";
    private MqttConfiguration mqttConfiguration;

    public static Network network = Network.newNetwork();
    @Container
    // lifecycle is handled by @Testcontainers
    @SuppressWarnings("resource")
    public static GenericContainer<?> mqtt = new GenericContainer<>(DockerImageName.parse("emqx/nanomq:0.20.0"))
            .withExposedPorts(1883)
            .withNetwork(network)
            .withCopyFileToContainer(MountableFile.forClasspathResource("nanomq/nanomq_pwd.conf"),
                                     "/etc/nanomq_pwd.conf")
            .withCopyFileToContainer(MountableFile.forClasspathResource("nanomq/nanomq.conf"), "/etc/nanomq.conf")
            .withNetworkAliases("mqtt");
    @Container
    public static ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);
    private ObjectMapper mapper;
    private Proxy proxy;
    private OesterreichsEnergieDataSource dataSource;

    @BeforeEach
    void setUp() throws IOException {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));
        mapper = new AiidaConfiguration().customObjectMapper().build();

        var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        proxy = toxiproxyClient.createProxy("mqtt", "0.0.0.0:8666", "mqtt:1883");

        var ipAddressViaToxiproxy = toxiproxy.getHost();
        var portViaToxiproxy = toxiproxy.getMappedPort(8666);
        var serverURI = "tcp://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy;
        mqttConfiguration = new MqttConfiguration(serverURI, serverURI, 10, USERNAME, PASSWORD);

        dataSource = new OesterreichsEnergieDataSource(
                new DataSourceDto(DATA_SOURCE_ID,
                                  DataSourceType.SMART_METER_ADAPTER,
                                  AiidaAsset.SUBMETER,
                                  "sma",
                                  "AT",
                                  true,
                                  null,
                                  null,
                                  null),
                USER_ID,
                new DataSourceMqttDto(serverURI,
                                      serverURI,
                                      "aiida/test",
                                      USERNAME,
                                      PASSWORD)
        );

        LOG_CAPTOR.setLogLevelToTrace();
    }

    @AfterEach
    void teardown() throws Exception {
        LOG_CAPTOR.clearLogs();
        proxy.delete();
    }

    @Test
    @Timeout(30)
    // adapter is closed by StepVerifier
    @SuppressWarnings("resource")
    void givenSampleJsonViaMqtt_recordsArePublishedToFlux() {
        var sampleJson = "{\"0-0:96.1.0\":{\"value\":\"90296857\"},\"0-0:1.0.0\":{\"value\":0,\"time\":1697623015},\"1-0:1.8.0\":{\"value\":83403,\"time\":1697623015},\"1-0:2.8.0\":{\"value\":16564,\"time\":1697623015},\"1-0:1.7.0\":{\"value\":40,\"time\":1697623015},\"1-0:2.7.0\":{\"value\":0,\"time\":1697623015},\"0-0:2.0.0\":{\"value\":481,\"time\":0},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2435.7}";

        var adapter = new OesterreichsEnergieAdapter(dataSource, mapper, mqttConfiguration);

        StepVerifier.create(adapter.start())
                    .then(() -> publishSampleMqttMessage(dataSource.mqttSubscribeTopic(), sampleJson))
                    .expectNextCount(1)
                    .then(adapter::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(33));    // internal timeout for .close is 30 seconds
    }

    /**
     * Blockingly publishes the msg <b>directly</b> to the server, i.e. without the proxy.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    private void publishSampleMqttMessage(String topic, String msg) {
        var directServerURI = "tcp://" + mqtt.getHost() + ":" + mqtt.getMappedPort(1883);

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try {
                    var client = new MqttClient(directServerURI, UUID.randomUUID().toString());
                    var options = new MqttConnectionOptions();
                    options.setAutomaticReconnect(false);
                    options.setCleanStart(true);
                    options.setUserName(USERNAME);
                    options.setPassword(PASSWORD.getBytes(StandardCharsets.UTF_8));
                    client.connect(options);
                    client.publish(topic, msg.getBytes(StandardCharsets.UTF_8), 2, false);
                    client.disconnect();
                    client.close();
                } catch (MqttException ignored) {
                    // ignored, timeout should cause testcase to fail
                }
            });
        }
    }

    @Test
    // adapter is closed by StepVerifier
    @SuppressWarnings({"resource", "FutureReturnValueIgnored"})
    void verify_mqttClientAutomaticallyReconnects() {
        var value = 20;
        var expectedValue = String.valueOf(value / 1000f);
        var json = "{\"1-0:2.7.0\":{\"value\":" + value + ",\"time\":1697622970},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2390.6}";

        var adapter = new OesterreichsEnergieAdapter(dataSource, mapper, mqttConfiguration);
        adapter.setKeepAliveInterval(1);

        var scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(this::cutConnection, 1, TimeUnit.SECONDS);
        scheduler.schedule(this::restoreConnection, 3, TimeUnit.SECONDS);
        scheduler.schedule(() -> publishSampleMqttMessage(dataSource.mqttSubscribeTopic(), json), 4, TimeUnit.SECONDS);


        StepVerifier.create(adapter.start())
                    .expectNextMatches(aiidaRecord -> aiidaRecord.aiidaRecordValues().stream()
                                                                 .anyMatch(aiidaRecordValue -> aiidaRecordValue.value()
                                                                                                               .equals(expectedValue)))
                    .then(adapter::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));

        assertTrue(LOG_CAPTOR.getInfoLogs().stream().anyMatch(s -> s.endsWith("was from automatic reconnect is true")));
        assertThat(LOG_CAPTOR.getWarnLogs()).contains("Disconnected from MQTT broker");
    }

    private void cutConnection() {
        try {
            proxy.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0);
            proxy.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0);
        } catch (IOException e) {
            fail(e);
        }
    }

    private void restoreConnection() {
        try {
            proxy.toxics().get("CUT_CONNECTION_DOWNSTREAM").remove();
            proxy.toxics().get("CUT_CONNECTION_UPSTREAM").remove();
        } catch (IOException e) {
            fail(e);
        }
    }
}