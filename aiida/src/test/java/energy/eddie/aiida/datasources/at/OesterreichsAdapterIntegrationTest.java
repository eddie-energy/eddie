package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.record.IntegerAiidaRecord;
import energy.eddie.aiida.utils.MqttConfig;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
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
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
class OesterreichsAdapterIntegrationTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    public static Network network = Network.newNetwork();
    @Container
    // lifecycle is handled by @Testcontainers
    @SuppressWarnings("resource")
    public static GenericContainer<?> mqtt = new GenericContainer<>(DockerImageName.parse("emqx/nanomq:0.20.0"))
            .withExposedPorts(1883)
            .withNetwork(network)
            .withNetworkAliases("mqtt");
    @Container
    public static ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);
    private ObjectMapper mapper;
    private Proxy proxy;
    private String serverURI;

    @BeforeEach
    void setUp() throws IOException {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        proxy = toxiproxyClient.createProxy("mqtt", "0.0.0.0:8666", "mqtt:1883");

        var ipAddressViaToxiproxy = toxiproxy.getHost();
        var portViaToxiproxy = toxiproxy.getMappedPort(8666);
        serverURI = "tcp://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy;

        logCaptor.setLogLevelToTrace();
    }

    @AfterEach
    public void teardown() throws Exception {
        logCaptor.clearLogs();
        proxy.delete();
    }

    @Test
    @Timeout(5)
    // adapter is closed by StepVerifier
    @SuppressWarnings("resource")
    void givenSampleJsonViaMqtt_recordsArePublishedToFlux() {
        var sampleJson = "{\"0-0:96.1.0\":{\"value\":\"90296857\"},\"0-0:1.0.0\":{\"value\":0,\"time\":1697623015},\"1-0:1.8.0\":{\"value\":83403,\"time\":1697623015},\"1-0:2.8.0\":{\"value\":16564,\"time\":1697623015},\"1-0:1.7.0\":{\"value\":40,\"time\":1697623015},\"1-0:2.7.0\":{\"value\":0,\"time\":1697623015},\"0-0:2.0.0\":{\"value\":481,\"time\":0},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2435.7}";

        MqttConfig config = new MqttConfig(serverURI, "MyTestTopic");

        var adapter = new OesterreichsEnergieAdapter(config, mapper);

        StepVerifier.create(adapter.start())
                .then(() -> publishSampleMqttMessage(config.subscribeTopic(), sampleJson))
                .expectNextCount(7)
                .then(adapter::close)
                .expectComplete()
                .verify();
    }

    /**
     * Blockingly publishes the msg <b>directly</b> to the server, i.e. without the proxy.
     */
    private void publishSampleMqttMessage(String topic, String msg) {
        var directServerURI = "tcp://" + mqtt.getHost() + ":" + mqtt.getMappedPort(1883);

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try {
                    var client = new MqttClient(directServerURI, MqttClient.generateClientId());
                    var options = new MqttConnectOptions();
                    options.setAutomaticReconnect(false);
                    options.setCleanSession(true);
                    client.connect(options);
                    client.publish(topic, msg.getBytes(), 2, false);
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
    @SuppressWarnings("resource")
    void verify_mqttClientAutomaticallyReconnects() {
        MqttConfig config = new MqttConfig(serverURI, "MyReconnectTopic", 1);
        var json1 = "{\"1-0:2.7.0\":{\"value\":0,\"time\":1697622950},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2370.6}";
        var json2 = "{\"1-0:2.7.0\":{\"value\":10,\"time\":1697622960},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2380.6}";
        var json3 = "{\"1-0:2.7.0\":{\"value\":20,\"time\":1697622970},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2390.6}";

        var adapter = new OesterreichsEnergieAdapter(config, mapper);

        var scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
            cutConnection();
            publishSampleMqttMessage(config.subscribeTopic(), json2);
        }, 1, TimeUnit.SECONDS);
        scheduler.schedule(this::restoreConnection, 3, TimeUnit.SECONDS);
        scheduler.schedule(() -> publishSampleMqttMessage(config.subscribeTopic(), json3), 4, TimeUnit.SECONDS);


        StepVerifier.create(adapter.start())
                .then(() -> publishSampleMqttMessage(config.subscribeTopic(), json1))
                .expectNextMatches(aiidaRecord -> ((IntegerAiidaRecord) aiidaRecord).value() == 0)
                .expectNextMatches(aiidaRecord -> ((IntegerAiidaRecord) aiidaRecord).value() == 20)
                .then(adapter::close)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        assertThat(logCaptor.getWarnLogs()).contains("Lost connection to MQTT broker");
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