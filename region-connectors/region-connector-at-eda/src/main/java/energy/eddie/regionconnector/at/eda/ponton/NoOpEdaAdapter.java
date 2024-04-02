package energy.eddie.regionconnector.at.eda.ponton;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.masterdata._01p30.MasterData;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import reactor.core.publisher.Flux;

import java.util.Map;

public class NoOpEdaAdapter implements EdaAdapter {
    @Override
    public Flux<CMRequestStatus> getCMRequestStatusStream() {
        return Flux.empty();
    }

    @Override
    public Flux<EdaConsumptionRecord> getConsumptionRecordStream() {
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
    public void sendCMRequest(CCMORequest request) {
        // NoOp
    }

    @Override
    public void sendCMRevoke(CCMORevoke revoke) {
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
