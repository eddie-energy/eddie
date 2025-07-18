package energy.eddie.regionconnector.at.eda.ponton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
class NoOpEdaAdapterTest {

    public final NoOpEdaAdapter noOpEdaAdapter = new NoOpEdaAdapter();

    @Test
    void getCMRequestStatusStream() {
        Assertions.assertNotNull(noOpEdaAdapter.getCMRequestStatusStream());
    }

    @Test
    void getConsumptionRecordStream() {
        Assertions.assertNotNull(noOpEdaAdapter.getConsumptionRecordStream());
    }

    @Test
    void getCMRevokeStream() {
        Assertions.assertNotNull(noOpEdaAdapter.getCMRevokeStream());
    }

    @Test
    void getMasterDataStream() {
        Assertions.assertNotNull(noOpEdaAdapter.getMasterDataStream());
    }

    @Test
    void getCPRequestResultStream() {
        Assertions.assertNotNull(noOpEdaAdapter.getCPRequestResultStream());
    }

    @Test
    void sendCMRequest() {
        assertDoesNotThrow(() -> noOpEdaAdapter.sendCMRequest(null));
    }

    @Test
    void sendCMRevoke() {
        assertDoesNotThrow(() -> noOpEdaAdapter.sendCMRevoke(null));
    }

    @Test
    void sendCPRequest() {
        assertDoesNotThrow(() -> noOpEdaAdapter.sendCPRequest(null));
    }

    @Test
    void start() {
        assertDoesNotThrow(noOpEdaAdapter::start);
    }

    @Test
    void close() {
        assertDoesNotThrow(noOpEdaAdapter::close);
    }
}
