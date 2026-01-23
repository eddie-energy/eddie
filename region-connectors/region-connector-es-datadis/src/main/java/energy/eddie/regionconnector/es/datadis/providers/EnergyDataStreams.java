// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers;

import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EnergyDataStreams implements AutoCloseable {
    private final Sinks.Many<IdentifiableMeteringData> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<IdentifiableAccountingPointData> apSink = Sinks.many().multicast().onBackpressureBuffer();

    public Sinks.EmitResult publish(IdentifiableMeteringData data) {
        return vhdSink.tryEmitNext(data);
    }

    public void publish(IdentifiableAccountingPointData data) {
        apSink.tryEmitNext(data);
    }

    public Flux<IdentifiableMeteringData> getValidatedHistoricalData() {
        return vhdSink.asFlux();
    }

    public Flux<IdentifiableAccountingPointData> getAccountingPointData() {
        return apSink.asFlux();
    }

    @Override
    public void close() {
        vhdSink.tryEmitComplete();
        apSink.tryEmitComplete();
    }
}
