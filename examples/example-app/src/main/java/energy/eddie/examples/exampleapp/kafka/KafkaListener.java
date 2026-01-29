package energy.eddie.examples.exampleapp.kafka;

import energy.eddie.cim.v0_82.pmd.MktActivityRecordComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.PointComplexType;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.TimeSeriesComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.examples.exampleapp.Env;
import energy.eddie.examples.exampleapp.kafka.serdes.PermissionMarketDocumentSerde;
import energy.eddie.examples.exampleapp.kafka.serdes.ValidatedHistoricalDataEnvelopeSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KafkaListener implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaListener.class);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Jdbi jdbi;
    private final ObjectMapper mapper;
    private static final Map<String, Integer> meteringIntervalForCode = Map.of(
            "PT15M", 900,
            "PT30M", 1800,
            "PT1H", 3600,
            "PT1D", 86400,
            "P1D", 86400
    );

    public KafkaListener(Jdbi jdbi, ObjectMapper mapper) {
        this.jdbi = jdbi;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        var vhdTopology = createVhdTopology();
        var statusTopology = createPermissionMarketDocumentTopology();
        var vhdProperties = getKafkaProperties("vhd");
        var statusProperties = getKafkaProperties("status");
        var vhdStream = new KafkaStreams(vhdTopology, vhdProperties);
        var statusStream = new KafkaStreams(statusTopology, statusProperties);

        vhdStream.start();
        statusStream.start();

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            vhdStream.close(new KafkaStreams.CloseOptions().leaveGroup(true));
            statusStream.close(new KafkaStreams.CloseOptions().leaveGroup(true));

            Thread.currentThread().interrupt();
        }

        vhdStream.close(new KafkaStreams.CloseOptions().leaveGroup(true));
        statusStream.close(new KafkaStreams.CloseOptions().leaveGroup(true));

        vhdStream.cleanUp();
        statusStream.cleanUp();
    }

    private Topology createVhdTopology() {
        var inputTopic = "validated-historical-data";
        Serde<String> stringSerde = Serdes.String();
        var vhdSerde = new ValidatedHistoricalDataEnvelopeSerde(mapper);

        StreamsBuilder builder = new StreamsBuilder();
        builder
                .stream(inputTopic, Consumed.with(stringSerde, vhdSerde))
                .filterNot((unusedKey, value) -> Objects.isNull(value))
                .foreach(this::insertVhdIntoDb);

        return builder.build();
    }

    private void insertVhdIntoDb(String key, ValidatedHistoricalDataEnvelope document) {
        var vhd = document.getValidatedHistoricalDataMarketDocument();
        TimeSeriesComplexType timeSeries = vhd.getTimeSeriesList().getTimeSeries().getFirst();
        TimeSeriesComplexType.SeriesPeriodList seriesPeriods = timeSeries.getSeriesPeriodList();

        String connectionId = document.getMessageDocumentHeader()
                                      .getMessageDocumentHeaderMetaInformation()
                                      .getConnectionid();
        String meteringPoint = timeSeries.getMarketEvaluationPointMRID().getValue();
        ZonedDateTime startDateTime = ZonedDateTime.parse(vhd.getPeriodTimeInterval().getStart());
        Integer resolution = meteringIntervalForCode.get(seriesPeriods.getSeriesPeriods().getFirst().getResolution());

        LOGGER.info("Writing consumption records for connection {} with metering point {} with {} data points",
                connectionId, meteringPoint, seriesPeriods.getSeriesPeriods().size());

        jdbi.withHandle(h -> {
            var id = h.createUpdate("""
                            INSERT INTO consumption_records(connection_id, metering_point,start_date_time,metering_interval_secs) VALUES (?,?,?,?)
                            """)
                    .bind(0, connectionId)
                    .bind(1, meteringPoint)
                    .bind(2, startDateTime)
                    .bind(3, resolution)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Integer.class)
                    .first();
            int order = 0;
            for (SeriesPeriodComplexType period : seriesPeriods.getSeriesPeriods()) {
                for (PointComplexType point : period.getPointList().getPoints()) {
                    BigDecimal consumption = point.getEnergyQuantityQuantity();
                    String type = point.getEnergyQuantityQuality().value();

                    h.createUpdate("""
                                           INSERT INTO consumption_points(consumption_record_id, ord, consumption, metering_type) VALUES (?, ?, ?, ?)
                                           """)
                            .bind(0, id)
                            .bind(1, order)
                            .bind(2, consumption)
                            .bind(3, type)
                            .execute();
                    order++;
                }
            }
            return null;
        });
    }

    private Topology createPermissionMarketDocumentTopology() {
        var inputTopic = "permission-market-documents";
        Serde<String> stringSerde = Serdes.String();
        Serde<PermissionEnvelope> statusSerde = new PermissionMarketDocumentSerde(mapper);

        StreamsBuilder builder = new StreamsBuilder();
        builder
                .stream(inputTopic, Consumed.with(stringSerde, statusSerde))
                .filterNot((unusedKey, value) -> Objects.isNull(value))
                .foreach(this::insertPermissionMarketDocument);

        return builder.build();
    }

    private void insertPermissionMarketDocument(String key, PermissionEnvelope document) {
        PermissionComplexType permission = document.getPermissionMarketDocument()
                                                   .getPermissionList()
                                                   .getPermissions()
                                                   .getFirst();
        MktActivityRecordComplexType mktRecord = permission.getMktActivityRecordList().getMktActivityRecords().getFirst();
        String status = mktRecord.getStatus().value();
        ZonedDateTime timestamp = ZonedDateTime.parse(mktRecord.getCreatedDateTime());
        String connectionId = permission.getMarketEvaluationPointMRID().getValue();

        LOGGER.info("Writing permission market document status for {}: {}", connectionId, status);
        jdbi.withHandle(h ->
                h.createUpdate("INSERT INTO connection_status (connection_id, timestamp_, consent_status) VALUES (?,?,?)")
                        .bind(0, connectionId)
                        .bind(1, timestamp)
                        .bind(2, status)
                        .execute());
    }

    private Properties getKafkaProperties(String streamName) {
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "example-app-" + streamName);
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, Env.KAFKA_BOOTSTRAP_SERVERS.get());

        for (Map.Entry<String, String> env : System.getenv().entrySet()) {
            var key = env.getKey().toLowerCase(Locale.ROOT);
            if (key.startsWith("example_app_kafka_")) {
                var kafkaPropertyKey = key // Kafka properties are written in lowercase
                                           .replace("example_app_kafka_", "")
                                           .replace("_", "."); // Kafka properties are separated with "."
                streamsConfiguration.put(kafkaPropertyKey, env.getValue());
            }
        }

        return streamsConfiguration;
    }

    public void stop() {
        synchronized (latch) {
            latch.countDown();
        }
    }
}
