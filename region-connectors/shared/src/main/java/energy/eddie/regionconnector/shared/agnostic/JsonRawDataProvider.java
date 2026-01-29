// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.agnostic;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JsonRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRawDataProvider.class);
    private final String regionConnector;
    private final ObjectMapper objectMapper;
    private final Flux<IdentifiablePayload<?, ?>> fluxes;

    @SafeVarargs
    public JsonRawDataProvider(
            String regionConnector,
            ObjectMapper objectMapper,
            Flux<? extends IdentifiablePayload<?, ?>>... fluxes
    ) {
        this.regionConnector = regionConnector;
        this.objectMapper = objectMapper;
        this.fluxes = Flux.merge(fluxes);
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return fluxes.mapNotNull(this::createRawDataMessage);
    }

    @Nullable
    private RawDataMessage createRawDataMessage(IdentifiablePayload<?, ?> pair) {
        try {
            String rawString = objectMapper.writeValueAsString(pair.payload());
            return new RawDataMessage(pair.permissionRequest(), rawString);
        } catch (JacksonException e) {
            LOGGER.warn("Error serializing metering data for region-connector {}", regionConnector, e);
            return null;
        }
    }

    @Override
    public void close() {
        // No-Op
    }
}
