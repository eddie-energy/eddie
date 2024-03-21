package energy.eddie.regionconnector.aiida;

import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
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

import java.time.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID;

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
     * @throws DataNeedNotFoundException    Thrown if the dataNeedsService from the parent doesn't contain a dataNeed
     *                                      with the specified ID.
     * @throws UnsupportedDataNeedException If the data need is not an AIIDA data need.
     */
    public AiidaPermissionRequestInterface createPermissionRequest(
            String connectionId,
            String dataNeedId
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        LocalDate today = LocalDate.now(clock);

        DataNeedWrapper wrapper = dataNeedsService.findDataNeedAndCalculateStartAndEnd(
                dataNeedId,
                today,
                // in case of open end/start, fixed values are used
                Period.ZERO,
                Period.ZERO);

        if (!(wrapper.timeframedDataNeed() instanceof GenericAiidaDataNeed))
            throw new UnsupportedDataNeedException(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId,
                                                   "Only GenericDataNeeds are currently supported.");

        // use nice looking fixed values if data need uses open start/end --> only possible for AIIDA because it has no real limits how long data can be accessed
        LocalDate startDate = getAppropriateStartDate(wrapper);
        LocalDate endDate = getAppropriateEndDate(wrapper);


        // convert to AIIDA required datetime format
        ZonedDateTime startTime = startDate.atStartOfDay(REGION_CONNECTOR_ZONE_ID);
        ZonedDateTime expirationTime = endDate.atTime(LocalTime.of(23, 59, 59)).atZone(REGION_CONNECTOR_ZONE_ID);

        var permissionId = UUID.randomUUID().toString();
        var terminationTopic = terminationTopicForPermissionId(permissionId);
        AiidaPermissionRequest request = new AiidaPermissionRequest(permissionId, connectionId, dataNeedId,
                                                                    terminationTopic, startTime, expirationTime);

        return PermissionRequestProxy.createProxy(request,
                                                  extensions,
                                                  AiidaPermissionRequestInterface.class,
                                                  PermissionRequestProxy.CreationInfo.NEWLY_CREATED);
    }

    /**
     * Returns the calculated start date from the wrapper or the fixed start date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link DataNeedsService#findDataNeedAndCalculateStartAndEnd(String, LocalDate, Period, Period)}
     * @return LocalDate to use as start date for the permission request.
     */
    private LocalDate getAppropriateStartDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.start().isEmpty())
            return LocalDate.of(2000, 1, 1);

        return wrapper.calculatedStart();
    }

    /**
     * Returns the calculated end date from the wrapper or the fixed end date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link DataNeedsService#findDataNeedAndCalculateStartAndEnd(String, LocalDate, Period, Period)}
     * @return LocalDate to use as end date for the permission request.
     */
    private LocalDate getAppropriateEndDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.end().isEmpty())
            return LocalDate.of(9999, 12, 31);

        return wrapper.calculatedEnd();
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
     * Creates and populates a new {@link PermissionDto} with values from configuration.
     *
     * @param aiidaRequest Request for which a PermissionDto should be created.
     * @return Populated PermissionDto.
     * @throws InvalidTopicException        If topic name for the termination topic is invalid. This indicates that
     *                                      either the permissionId is not a valid UUID-4, or the prefix from the
     *                                      configuration is invalid.
     * @throws DataNeedNotFoundException    If there is no data need with the specified ID in the service.
     * @throws UnsupportedDataNeedException If the data need is not a supported by this region connector. Should not
     *                                      occur, because at permission request creation time it is checked whether the
     *                                      linked data need is supported.
     */
    public PermissionDto createPermissionDto(AiidaPermissionRequestInterface aiidaRequest) throws InvalidTopicException, DataNeedNotFoundException, UnsupportedDataNeedException {
        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                aiidaRequest.terminationTopic()
        );

        String dataNeedId = aiidaRequest.dataNeedId();
        DataNeed dataNeed = dataNeedsService.findById(dataNeedId)
                                            .orElseThrow(() -> new DataNeedNotFoundException(dataNeedId));

        // TODO currently only GenericDataNeed is supported
        if (!(dataNeed instanceof GenericAiidaDataNeed genericAiidaDataNeed))
            throw new UnsupportedDataNeedException(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeedId,
                                                   "Only GenericDataNeeds are currently supported.");

        return new PermissionDto(
                aiidaRequest.permissionId(),
                dataNeed.name(),
                dataNeedId,
                aiidaRequest.start().toInstant(),
                Objects.requireNonNull(aiidaRequest.end()).toInstant(),
                aiidaRequest.connectionId(),
                genericAiidaDataNeed.dataTags(),
                kafkaConfig
        );
    }

    /**
     * Creates the termination topic name by concatenating the termination topic prefix and the permissionId with an
     * underscore.
     *
     * @param permissionId Id of permission request
     * @return Kafka topic name
     * @throws InvalidTopicException If the resulting topic name is invalid. This indicates that either the permissionId
     *                               is not a valid UUID-4, or the prefix is invalid.
     */
    private String terminationTopicForPermissionId(String permissionId) throws InvalidTopicException {
        var topic = configuration.kafkaTerminationTopicPrefix() + "_" + permissionId;
        Topic.validate(topic);
        return topic;
    }
}
