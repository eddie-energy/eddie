// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.streams;

import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.tasks.MeterReadingFilterTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.function.UnaryOperator;

@Component
public class IdentifiableDataStreams implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableDataStreams.class);
    private final Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many()
                                                                               .multicast()
                                                                               .onBackpressureBuffer();
    private final UnaryOperator<IdentifiableMeteringData> task;

    @Autowired
    public IdentifiableDataStreams(MeterReadingFilterTask task) {this((UnaryOperator<IdentifiableMeteringData>) task);}

    public IdentifiableDataStreams() {
        this(id -> id);
    }

    IdentifiableDataStreams(UnaryOperator<IdentifiableMeteringData> filter) {
        this.task = filter;
    }

    public void publish(
            FluviusPermissionRequest permissionRequest,
            GetEnergyResponseModelApiDataResponse payload
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Publishing metering data for permission request {}");
        var filtered = task.apply(new IdentifiableMeteringData(permissionRequest, payload));
        meteringDataSink.emitNext(filtered, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    public Flux<IdentifiableMeteringData> getMeteringData() {
        return meteringDataSink.asFlux();
    }

    @Override
    public void close() {
        meteringDataSink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }
}
