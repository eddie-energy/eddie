// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SupportedFeatureService {
    private final List<RegionConnectorSupportedFeatureExtension> supportedFeatureExtensions = new ArrayList<>();

    public void register(RegionConnectorSupportedFeatureExtension supportedFeatureExtension) {
        supportedFeatureExtensions.add(supportedFeatureExtension);
    }

    public List<RegionConnectorSupportedFeatureExtension> getSupportedFeatureExtensions() {
        return Collections.unmodifiableList(supportedFeatureExtensions);
    }
}
