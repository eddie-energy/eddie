package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.DeDataSourceInformation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DePermissionRequestTest {

    @Test
    void builderShouldCreateRequestWithAllFields() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate start = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        LocalDate end = LocalDate.now(ZoneId.systemDefault()).plusDays(1);

        DePermissionRequest request = new DePermissionRequestBuilder()
                .permissionId("perm-123")
                .connectionId("conn-456")
                .meteringPointId("mp-789")
                .start(start)
                .end(end)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.VALIDATED)
                .created(now)
                .dataNeedId("dn-1")
                .latestMeterReadingEndDate(end)
                .message("some message")
                .cause("some cause")
                .build();

        assertThat(request.permissionId()).isEqualTo("perm-123");
        assertThat(request.connectionId()).isEqualTo("conn-456");
        assertThat(request.meteringPointId()).isEqualTo("mp-789");
        assertThat(request.start()).isEqualTo(start);
        assertThat(request.end()).isEqualTo(end);
        assertThat(request.granularity()).isEqualTo(Granularity.PT15M);
        assertThat(request.energyType()).isEqualTo(EnergyType.ELECTRICITY);
        assertThat(request.status()).isEqualTo(PermissionProcessStatus.VALIDATED);
        assertThat(request.created()).isEqualTo(now);
        assertThat(request.dataSourceInformation()).isInstanceOf(DeDataSourceInformation.class);
        assertThat(request.dataNeedId()).isEqualTo("dn-1");
        assertThat(request.latestMeterReadingEndDate()).contains(end);
        assertThat(request.message()).contains("some message");
        assertThat(request.cause()).contains("some cause");
    }

    @Test
    void optionalFieldsShouldReturnEmptyWhenNotSet() {
        DePermissionRequest request = new DePermissionRequestBuilder()
                .permissionId("id")
                .build();

        assertThat(request.latestMeterReadingEndDate()).isEmpty();
        assertThat(request.message()).isEmpty();
        assertThat(request.cause()).isEmpty();
    }
}
