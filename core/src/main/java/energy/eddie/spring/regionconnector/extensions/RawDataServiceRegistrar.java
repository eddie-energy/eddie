// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.core.services.RawDataService;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RegionConnectorExtension
@OnRawDataMessagesEnabled
public class RawDataServiceRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawDataServiceRegistrar.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    RawDataServiceRegistrar(
            Optional<RawDataProvider> rawDataProvider,
            RawDataService rawDataService,
            @Qualifier(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME) String regionConnectorName
    ) {
        requireNonNull(rawDataProvider);
        requireNonNull(rawDataService);
        requireNonNull(regionConnectorName);

        rawDataProvider.ifPresentOrElse(rawDataService::registerProvider, () ->
                LOGGER.warn("Region connector '{}' does not implement a RawDataProvider", regionConnectorName));
    }
}
