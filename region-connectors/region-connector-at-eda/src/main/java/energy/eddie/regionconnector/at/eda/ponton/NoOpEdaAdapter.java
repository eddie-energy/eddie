package energy.eddie.regionconnector.at.eda.ponton;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.requests.CPRequestResult;
import reactor.core.publisher.Flux;

public class NoOpEdaAdapter implements EdaAdapter {
    @Override
    public Flux<CMRequestStatus> getCMRequestStatusStream() {
        return Flux.empty();
    }

    @Override
    public Flux<IdentifiableConsumptionRecord> getConsumptionRecordStream() {
        return Flux.empty();
    }

    @Override
    public Flux<EdaCMRevoke> getCMRevokeStream() {
        return Flux.empty();
    }

    @Override
    public Flux<IdentifiableMasterData> getMasterDataStream() {
        return Flux.empty();
    }

    @Override
    public Flux<CPRequestResult> getCPRequestResultStream() {
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
    public void sendCPRequest(CPRequestCR cpRequestCR) {
        // NoOp
    }

    @Override
    public void start() {
        // NoOp
    }

    @Override
    public void close() {
        // NoOp
    }
}
