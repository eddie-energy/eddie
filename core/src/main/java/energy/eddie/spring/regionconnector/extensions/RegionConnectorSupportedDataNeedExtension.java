package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.SupportedDataNeedService;

import java.util.List;

@RegionConnectorExtension
public class RegionConnectorSupportedDataNeedExtension {
    private final RegionConnectorMetadata metadata;

     public RegionConnectorSupportedDataNeedExtension(
            SupportedDataNeedService supportedDataNeedService,
            RegionConnectorMetadata metadata
    ) {
        this.metadata = metadata;
        supportedDataNeedService.register(this);
    }


    @JsonProperty
    public List<String> supportedDataNeeds() {
        return metadata.supportedDataNeeds();
    }

    @JsonProperty
    public String regionConnectorId() {
        return metadata.id();
    }
}
