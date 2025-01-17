package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.shared.cim.v0_82.pmd.IntermediatePermissionMarketDocument;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import energy.eddie.regionconnector.simulation.permission.request.SimulationPermissionRequest;

import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.SequencedCollection;

public class StatusEmissionStep implements Step {
    private final PermissionProcessStatus status;

    public StatusEmissionStep(PermissionProcessStatus status) {this.status = status;}

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        var streams = ctx.documentStreams();
        streams.publish(
                new IntermediatePermissionMarketDocument<>(
                        new SimulationPermissionRequest(ctx.connectionId(),
                                                        ctx.permissionId(),
                                                        ctx.dataNeedId(),
                                                        status),
                        SimulationConnectorMetadata.REGION_CONNECTOR_ID,
                        ignored -> null,
                        "N" + SimulationConnectorMetadata.getInstance().countryCode(),
                        ZoneOffset.UTC,
                        new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ofYears(-3),
                                                                                 Period.ofYears(3),
                                                                                 null),
                                                            EnergyType.ELECTRICITY,
                                                            Granularity.PT5M,
                                                            Granularity.P1Y)
                ).toPermissionMarketDocument()
        );
        streams.publish(new ConnectionStatusMessage(ctx.connectionId(),
                                                    ctx.permissionId(),
                                                    ctx.dataNeedId(),
                                                    new SimulationDataSourceInformation(),
                                                    status));
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
}
