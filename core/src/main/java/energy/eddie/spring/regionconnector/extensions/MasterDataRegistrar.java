package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.core.services.MasterDataService;

import java.util.Optional;

@RegionConnectorExtension
public class MasterDataRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public MasterDataRegistrar(
            Optional<MasterData> masterData,
            MasterDataService masterDataService
    ) {
        masterData.ifPresent(masterDataService::registerMasterData);
    }
}
