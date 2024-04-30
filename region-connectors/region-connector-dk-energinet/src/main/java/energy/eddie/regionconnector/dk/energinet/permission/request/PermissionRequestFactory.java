package energy.eddie.regionconnector.dk.energinet.permission.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.*;

@Component
public class PermissionRequestFactory {
    private final EnerginetCustomerApi customerApi;
    private final Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions;
    private final StateBuilderFactory stateBuilderFactory;
    private final DataNeedsService dataNeedsService;
    private final ObjectMapper mapper;

    public PermissionRequestFactory(
            EnerginetCustomerApi customerApi,
            Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions,
            StateBuilderFactory stateBuilderFactory,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in parent context
            DataNeedsService dataNeedsService,
            ObjectMapper mapper
    ) {
        this.customerApi = customerApi;
        this.extensions = extensions;
        this.stateBuilderFactory = stateBuilderFactory;
        this.dataNeedsService = dataNeedsService;
        this.mapper = mapper;
    }

    public DkEnerginetCustomerPermissionRequest create(PermissionRequestForCreation request) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var referenceDate = LocalDate.now(DK_ZONE_ID);

        var dataNeed = dataNeedsService.findById(request.dataNeedId())
                                       .orElseThrow(() -> new DataNeedNotFoundException(request.dataNeedId()));
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed))
            throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   request.dataNeedId(),
                                                   "This region connector only supports validated historical data data needs.");

        DataNeedWrapper wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                vhdDataNeed,
                referenceDate,
                PERIOD_EARLIEST_START,
                PERIOD_LATEST_END
        );


        var granularity = validateAndGetGranularity(vhdDataNeed);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                request,
                customerApi,
                wrapper.calculatedStart(),
                wrapper.calculatedEnd(),
                granularity,
                stateBuilderFactory,
                mapper);
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                DkEnerginetCustomerPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    private Granularity validateAndGetGranularity(ValidatedHistoricalDataDataNeed dataNeed) throws UnsupportedDataNeedException {
        return switch (dataNeed.minGranularity()) {
            case Granularity.PT15M, Granularity.PT1H, Granularity.P1D, Granularity.P1M, Granularity.P1Y ->
                    dataNeed.minGranularity();
            default -> throw new UnsupportedDataNeedException(EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                              dataNeed.id(),
                                                              "Unsupported granularity: '" + dataNeed.minGranularity() + "'");
        };
    }

    public DkEnerginetCustomerPermissionRequest create(DkEnerginetCustomerPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest.withApiClient(customerApi, mapper).withStateBuilderFactory(stateBuilderFactory),
                extensions,
                DkEnerginetCustomerPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }
}
