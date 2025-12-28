package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DePermissionRequestTest {

    @Test
    void builderShouldCreateRequestWithAllFields() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate start = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        LocalDate end = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
        DataSourceInformation dsi = mock(DataSourceInformation.class);

        DePermissionRequest request = DePermissionRequest.builder()
                .permissionId("perm-123")
                .connectionId("conn-456")
                .meteringPointId("mp-789")
                .start(start)
                .end(end)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.VALIDATED)
                .created(now)
                .dataSourceInformation(dsi)
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
        assertThat(request.dataSourceInformation()).isEqualTo(dsi);
        assertThat(request.dataNeedId()).isEqualTo("dn-1");
        assertThat(request.latestMeterReadingEndDate()).contains(end);
        assertThat(request.message()).contains("some message");
        assertThat(request.cause()).contains("some cause");
    }

    @Test
    void optionalFieldsShouldReturnEmptyWhenNotSet() {
        DePermissionRequest request = DePermissionRequest.builder()
                .permissionId("id")
                .build();

        assertThat(request.latestMeterReadingEndDate()).isEmpty();
        assertThat(request.message()).isEmpty();
        assertThat(request.cause()).isEmpty();
    }
}
