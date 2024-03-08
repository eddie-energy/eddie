package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.internals.Topic;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class AiidaFactory {
    private final AiidaConfiguration configuration;
    private final DataNeedsService dataNeedsService;
    private final Clock clock;
    private final Set<Extension<AiidaPermissionRequestInterface>> extensions;

    // DataNeedsService is provided by core, that's why autodiscovery in IntelliJ fails
    public AiidaFactory(
            AiidaConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            Clock clock,
            Set<Extension<AiidaPermissionRequestInterface>> extensions
    ) {
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
        this.clock = clock;
        this.extensions = extensions;
    }

    /**
     * Creates and populates a new permission request.
     *
     * @param connectionId connectionId that should be used for this new permission request.
     * @param dataNeedId   dataNeedId that should be used for this new permission request.
     * @return Populated permission request.
     * @throws DataNeedNotFoundException Thrown if the dataNeedsService from the parent doesn't contain a dataNeed with the specified ID.
     */
    public AiidaPermissionRequestInterface createPermissionRequest(
            String connectionId,
            String dataNeedId) throws DataNeedNotFoundException {

        var dataNeed = getDataNeed(dataNeedId);
        // DataNeeds have relative time but AIIDA needs absolute timestamps
        var startTime = calculateStart(dataNeed);
        var expirationTime = calculateEnd(dataNeed);

        var permissionId = UUID.randomUUID().toString();
        var terminationTopic = terminationTopicForPermissionId(permissionId);
        AiidaPermissionRequest request = new AiidaPermissionRequest(permissionId, connectionId, dataNeedId,
                terminationTopic, startTime, expirationTime);

        return PermissionRequestProxy.createProxy(request, extensions,
                AiidaPermissionRequestInterface.class, PermissionRequestProxy.CreationInfo.NEWLY_CREATED);
    }

    /**
     * Returns a proxy of an {@link AiidaPermissionRequest} that executes any {@link Extension}s when a state transition
     * method is called on the returned object.
     *
     * @param permissionRequest Permission request as e.g. returned by the persistence layer.
     * @return Proxy of {@link AiidaPermissionRequest}.
     */
    public AiidaPermissionRequestInterface recreatePermissionRequest(AiidaPermissionRequestInterface permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                AiidaPermissionRequestInterface.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }

    /**
     * Wrapper that gets the DataNeed from the DataNeedsService.
     *
     * @param dataNeedId ID of the dataNeed to get.
     * @return DataNeed
     * @throws DataNeedNotFoundException If there is no data need with the specified ID in the service.
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
    private ZonedDateTime calculateStart(DataNeed dataNeed) {
        var todayAtStartOfDay = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC);
        return todayAtStartOfDay.plusDays(dataNeed.durationStart());
    }

    /**
     * Converts the relative end date from the DataNeed to an absolute UTC Instant as required by AIIDA.
     *
     * @param dataNeed DataNeed that specifies the end date.
     * @return Instant representing the end date.
     */
    private ZonedDateTime calculateEnd(DataNeed dataNeed) {
        var todayAtEndOfDay = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).plusDays(1).minusSeconds(1);

        if (Boolean.TRUE.equals(dataNeed.durationOpenEnd()))
            // AIIDA needs fixed end date therefore just use a really long time
            return todayAtEndOfDay.plusYears(1000);

        // dataNeed validation ensures that this is never null if durationOpenEnd is false
        Integer durationEnd = Objects.requireNonNull(dataNeed.durationEnd());
        return todayAtEndOfDay.plusDays(durationEnd);
    }

    /**
     * Creates and populates a new {@link PermissionDto} with values from configuration.
     *
     * @param aiidaRequest Request for which a PermissionDto should be created.
     * @return Populated PermissionDto.
     * @throws InvalidTopicException If topic name for the termination topic is invalid. This indicates that either the
     *                               permissionId is not a valid UUID-4, or the prefix from the configuration is invalid.
     */
    public PermissionDto createPermissionDto(AiidaPermissionRequestInterface aiidaRequest) throws InvalidTopicException, DataNeedNotFoundException {
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
                aiidaRequest.start().toInstant(),
                Objects.requireNonNull(aiidaRequest.end()).toInstant(),
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
