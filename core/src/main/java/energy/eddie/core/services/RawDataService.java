// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@OnRawDataMessagesEnabled
public class RawDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawDataService.class);

    private final Sinks.Many<RawDataMessage> rawDataSink = Sinks.many().multicast().onBackpressureBuffer();

    public void registerProvider(RawDataProvider rawDataProvider) {
        LOGGER.info("RawDataService: Registering {}", rawDataProvider.getClass().getName());
        rawDataProvider.getRawDataStream()
                       .onErrorContinue((err, obj) -> LOGGER.warn(
                               "Encountered error while processing raw data",
                               err))
                       .subscribe(rawDataSink::tryEmitNext);
    }

    public Flux<RawDataMessage> getRawDataStream() {
        return rawDataSink.asFlux();
    }
}