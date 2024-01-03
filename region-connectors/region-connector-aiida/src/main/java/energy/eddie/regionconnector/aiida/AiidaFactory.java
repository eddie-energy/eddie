package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.shared.exceptions.DataNeedNotFoundException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.internals.Topic;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AiidaFactory {
    private final AiidaConfiguration configuration;
    private final DataNeedsService dataNeedsService;

    // DataNeedsService is provided by core, that's why autodiscovery in IntelliJ fails
    public AiidaFactory(AiidaConfiguration configuration, @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
    }

    /**
     * Creates and populates a new permission request.
     *
     * @param connectionId connectionId that should be used for this new permission request.
     * @param dataNeedId   dataNeedId that should be used for this new permission request.
     * @param service      Reference to the service that allows the request to transition states.
     * @return Populated permission request.
     * @throws DataNeedNotFoundException Thrown if the dataNeedsService from the parent doesn't contain a dataNeed with the specified ID.
     */
    public AiidaPermissionRequest createPermissionRequest(
            String connectionId,
            String dataNeedId,
            AiidaRegionConnectorService service) throws DataNeedNotFoundException {

        var dataNeed = getDataNeed(dataNeedId);
        // DataNeeds have relative time but AIIDA needs absolute timestamps
        var startTime = calculateStartInstant(dataNeed);
        var expirationTime = calculateEndInstant(dataNeed);

        var permissionId = UUID.randomUUID().toString();
        var terminationTopic = terminationTopicForPermissionId(permissionId);
        return new AiidaPermissionRequest(permissionId, connectionId, dataNeedId, terminationTopic, startTime, expirationTime, service);
    }

    /**
     * Wrapper that gets the DataNeed from the DataNeedsService.
     *
     * @param dataNeedId ID of the dataNeed to get.
     * @return DataNeed
     * @throws DataNeedNotFoundException If there is no DataNeed with the specified ID in the service.
     */
    private DataNeed getDataNeed(String dataNeedId) throws DataNeedNotFoundException {
        Optional<DataNeed> optionalDataNeed = dataNeedsService.getDataNeed(dataNeedId);

        if (optionalDataNeed.isEmpty())
            throw new DataNeedNotFoundException(dataNeedId);

        return optionalDataNeed.get();
    }

    /**
     * Converts the relative start date from the DataNeed to an absolute UTC Instant as required by AIIDA.
     *
     * @param dataNeed DataNeed that specifies the start date.
     * @return Instant representing the start date.
     */
    private Instant calculateStartInstant(DataNeed dataNeed) {
        var now = ZonedDateTime.now(ZoneId.of("UTC"));

        return now.plusDays(dataNeed.durationStart()).toInstant();
    }

    /**
     * Converts the relative end date from the DataNeed to an absolute UTC Instant as required by AIIDA.
     *
     * @param dataNeed DataNeed that specifies the end date.
     * @return Instant representing the end date.
     */
    private Instant calculateEndInstant(DataNeed dataNeed) {
        var now = ZonedDateTime.now(ZoneId.of("UTC"));

        if (Boolean.TRUE.equals(dataNeed.durationOpenEnd()))
            // AIIDA needs fixed end date therefore just use a really long time
            return now.plusYears(1000).toInstant();

        return now.plusDays(Objects.requireNonNull(dataNeed.durationEnd())).toInstant();
    }

    /**
     * Creates and populates a new {@link PermissionDto} with values from configuration.
     *
     * @param aiidaRequest Request for which a PermissionDto should be created.
     * @return Populated PermissionDto.
     * @throws InvalidTopicException If topic name for the termination topic is invalid. This indicates that either the
     *                               permissionId is not a valid UUID-4, or the prefix from the configuration is invalid.
     */
    public PermissionDto createPermissionDto(AiidaPermissionRequest aiidaRequest) throws InvalidTopicException, DataNeedNotFoundException {
        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                aiidaRequest.terminationTopic()
        );

        var dataNeed = getDataNeed(aiidaRequest.dataNeedId());

        return new PermissionDto(
                aiidaRequest.permissionId(),
                dataNeed.serviceName(),
                aiidaRequest.dataNeedId(),
                aiidaRequest.startTime(),
                aiidaRequest.expirationTime(),
                aiidaRequest.connectionId(),
                dataNeed.sharedDataIds(),
                kafkaConfig
        );
    }

    /**
     * Creates the termination topic name by concatenating the termination topic prefix and the permissionId with an underscore.
     *
     * @param permissionId Id of permission request
     * @return Kafka topic name
     * @throws InvalidTopicException If the resulting topic name is invalid. This indicates that either
     *                               the permissionId is not a valid UUID-4, or the prefix is invalid.
     */
    private String terminationTopicForPermissionId(String permissionId) throws InvalidTopicException {
        var topic = configuration.kafkaTerminationTopicPrefix() + "_" + permissionId;
        Topic.validate(topic);
        return topic;
    }
}
