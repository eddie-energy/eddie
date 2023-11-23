package energy.eddie.regionconnector.at.eda.ponton;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import reactor.core.publisher.Flux;

import java.util.Map;

public class NoOpEdaAdapter implements EdaAdapter {
    @Override
    public Flux<CMRequestStatus> getCMRequestStatusStream() {
        return Flux.empty();
    }

    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return Flux.empty();
    }

    @Override
    public Flux<CMRevoke> getCMRevokeStream() {
        return Flux.empty();
    }

    @Override
    public Flux<MasterData> getMasterDataStream() {
        return Flux.empty();
    }

    @Override
    public void sendCMRequest(CMRequest request) {
        // NoOp
    }

    @Override
    public void sendCMRevoke(CMRevoke revoke) {
        // NoOp
    }

    @Override
    public void start() {
        // NoOp
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of();
    }

    @Override
    public void close() {
        // NoOp
    }
}
