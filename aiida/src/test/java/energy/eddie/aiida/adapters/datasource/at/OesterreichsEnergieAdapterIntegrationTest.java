package energy.eddie.aiida.adapters.datasource.at;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Also tests that authentication works with username/password
 */
@Testcontainers
class OesterreichsEnergieAdapterIntegrationTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    private static final String TOPIC = "aiida/test";
    private static final String USERNAME = "aiida";
    private static final String PASSWORD = "testPassword";

    private static final String EMQX_IMAGE = "emqx/emqx:5.8.6";
    private static final String EMQX_BASE_CONFIG_FILE = "emqx/base.hocon";
    private static final String EMQX_INIT_USER_FILE = "emqx/init-user.json";
    private static final String EMQX_BASE_CONFIG_CONTAINER_PATH = "/opt/emqx/etc/" + EMQX_BASE_CONFIG_FILE.split("/")[1];
    private static final String EMQX_INIT_USER_CONTAINER_PATH = "/opt/emqx/data/" + EMQX_INIT_USER_FILE.split("/")[1];

    private static final String TOXIPROXY_IMAGE = "ghcr.io/shopify/toxiproxy:2.5.0";

    private static final OesterreichsEnergieDataSource DATA_SOURCE = mock(OesterreichsEnergieDataSource.class);

    public static Network network = Network.newNetwork();
    @Container
    // lifecycle is handled by @Testcontainers
    @SuppressWarnings("resource")
    public static GenericContainer<?> mqtt = new GenericContainer<>(DockerImageName.parse(EMQX_IMAGE))
            .withExposedPorts(1883)
            .withNetwork(network)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(EMQX_BASE_CONFIG_FILE),
                    EMQX_BASE_CONFIG_CONTAINER_PATH)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(EMQX_INIT_USER_FILE),
                    EMQX_INIT_USER_CONTAINER_PATH)
            .withNetworkAliases("mqtt");
    @Container
    public static ToxiproxyContainer toxiproxy = new ToxiproxyContainer(TOXIPROXY_IMAGE)
            .withNetwork(network);
    private MqttConfiguration mqttConfiguration;
    private ObjectMapper mapper;
    private Proxy proxy;

    @BeforeEach
    void setUp() throws IOException {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));
        mapper = new AiidaConfiguration().customObjectMapper().build();

        var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        proxy = toxiproxyClient.createProxy("mqtt", "0.0.0.0:8666", "mqtt:1883");

        var ipAddressViaToxiproxy = toxiproxy.getHost();
        var portViaToxiproxy = toxiproxy.getMappedPort(8666);
        var serverURI = "tcp://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy;
        mqttConfiguration = new MqttConfiguration(serverURI, serverURI, 10, PASSWORD, "");

        when(DATA_SOURCE.enabled()).thenReturn(true);
        when(DATA_SOURCE.internalHost()).thenReturn(serverURI);
        when(DATA_SOURCE.topic()).thenReturn(TOPIC);
        when(DATA_SOURCE.username()).thenReturn(USERNAME);
        when(DATA_SOURCE.password()).thenReturn(PASSWORD);

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

        var adapter = new OesterreichsEnergieAdapter(DATA_SOURCE, mapper, mqttConfiguration);

        StepVerifier.create(adapter.start())
                    .then(() -> publishSampleMqttMessage(TOPIC, sampleJson))
                    .expectNextCount(1)
                    .then(adapter::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(33));    // internal timeout for .close is 30 seconds
    }

    @Test
    // adapter is closed by StepVerifier
    @SuppressWarnings({"resource", "FutureReturnValueIgnored"})
    void verify_mqttClientAutomaticallyReconnects() {
        var value = 20;
        var expectedValue = String.valueOf(value / 1000f);
        var json = "{\"1-0:2.7.0\":{\"value\":" + value + ",\"time\":1697622970},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2390.6}";

        var adapter = new OesterreichsEnergieAdapter(DATA_SOURCE, mapper, mqttConfiguration);
        adapter.setKeepAliveInterval(1);

        var scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(this::cutConnection, 1, TimeUnit.SECONDS);
        scheduler.schedule(this::restoreConnection, 3, TimeUnit.SECONDS);
        scheduler.schedule(() -> publishSampleMqttMessage(TOPIC, json), 4, TimeUnit.SECONDS);


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