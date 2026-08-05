// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.cim.v0_82.pmd.IntermediatePermissionMarketDocumentV0_82;
import energy.eddie.regionconnector.shared.cim.v1_12.rpmd.IntermediateRequestPermissionMarketDocument;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import energy.eddie.regionconnector.simulation.permission.request.SimulationPermissionRequest;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

public class StatusEmissionStep implements Step {
    private final PermissionProcessStatus status;

    public StatusEmissionStep(PermissionProcessStatus status) {this.status = status;}

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        var request = getPermissionRequest(ctx);
        var streams = ctx.documentStreams();
        var dataNeed = ctx.dataNeed();
        streams.publish(
                new IntermediatePermissionMarketDocumentV0_82<>(
                        request,
                        SimulationConnectorMetadata.REGION_CONNECTOR_ID,
                        ignored -> null,
                        "N" + SimulationConnectorMetadata.getInstance().countryCode(),
                        ZoneOffset.UTC,
                        dataNeed
                ).toPermissionMarketDocument()
        );
        streams.publish(new ConnectionStatusMessage(ctx.connectionId(),
                                                    ctx.permissionId(),
                                                    ctx.dataNeedId(),
                                                    new SimulationDataSourceInformation(),
                                                    status));
        streams.publish(new IntermediateRequestPermissionMarketDocument<>(
                request,
                SimulationConnectorMetadata.REGION_CONNECTOR_ID,
                ignored -> null,
                "N" + SimulationConnectorMetadata.getInstance().countryCode(),
                ZoneOffset.UTC,
                dataNeed,
                request.status()
        ).toPermissionMarketDocument());
        return List.of();
    }

    @Override
    public int hashCode() {
        return status.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof StatusEmissionStep that)) return false;

        return status == that.status;
    }

    private PermissionRequest getPermissionRequest(SimulationContext ctx) {
        var timeframe = getTimeframe(ctx);
        return timeframe.map(value -> new SimulationPermissionRequest(
                                ctx.connectionId(),
                                ctx.permissionId(),
                                ctx.dataNeedId(),
                                status,
                                value.start(),
                                value.end()
                        ))
                        .orElseGet(() -> new SimulationPermissionRequest(ctx.connectionId(),
                                                                         ctx.permissionId(),
                                                                         ctx.dataNeedId(),
                                                                         status));
    }

    private static Optional<Timeframe> getTimeframe(SimulationContext ctx) {
        var timeframe = switch (ctx.calculationResult()) {
            case AccountingPointDataNeedResult r -> r.permissionTimeframe();
            case AiidaDataNeedResult r -> r.energyTimeframe();
            case CESUJoinRequestDataNeedResult r -> r.permissionTimeframe();
            case ValidatedHistoricalDataDataNeedResult r -> r.permissionTimeframe();
            default -> null;
        };
        return Optional.ofNullable(timeframe);
    }
}
