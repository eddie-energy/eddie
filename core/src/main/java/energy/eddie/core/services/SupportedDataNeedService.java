package energy.eddie.core.services;

import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedDataNeedExtension;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SupportedDataNeedService {
    private final List<RegionConnectorSupportedDataNeedExtension> supportedDataNeedExtensions = new ArrayList<>();

    public void register(RegionConnectorSupportedDataNeedExtension supportedFeatureExtension) {
        supportedDataNeedExtensions.add(supportedFeatureExtension);
    }

    public List<RegionConnectorSupportedDataNeedExtension> getSupportedDataNeedExtensions() {
        return Collections.unmodifiableList(supportedDataNeedExtensions);
    }
}
