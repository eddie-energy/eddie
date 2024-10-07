package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonBeanConfig;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionUpdateServiceTest {
    @Spy
    @SuppressWarnings("unused")
    private final PublishService publishService = new PublishService();
    @Spy
    @SuppressWarnings("unused")
    private final Jaxb2Marshaller marshaller = new GreenButtonBeanConfig().jaxb2Marshaller();
    @Mock
    private Outbox outbox;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @InjectMocks
    private PermissionUpdateService permissionUpdateService;
    @Captor
    private ArgumentCaptor<UsMeterReadingUpdateEvent> eventCaptor;


    @Test
    void updatePermissionRequest_updatesCorrectUsagePointAndEndDateTime() throws FeedException {
        // Given
        var xml = XmlLoader.xmlFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new StringReader(xml));
        var start = LocalDate.of(2024, 9, 3);
        var created = ZonedDateTime.of(start, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        var pr = createPermissionRequest(created, start);
        var payload = new IdentifiableSyndFeed(pr, feed);
        when(meterReadingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        permissionUpdateService.updatePermissionRequest(payload);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertThat(res.latestMeterReadingEndDateTime()).contains(created.plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BatchWithInvalidIntervalBlock.xml",
            "BatchWithInvalidSelfLink.xml",
            "BatchWithoutSelfLink.xml",
            "BatchWithWrongContentForIntervalBlock.xml",
            "BatchWithoutContentForIntervalBlock.xml",
    })
    void updatePermissionRequest_withInvalidIntervalBlock_doesNotUpdateLatestMeterReadings(String filename) throws FeedException {
        // Given
        var xml = XmlLoader.xmlFromResource("/xml/batch/" + filename);
        var feed = new SyndFeedInput().build(new StringReader(xml));
        var start = LocalDate.of(2024, 9, 3);
        var created = ZonedDateTime.of(start, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        var pr = createPermissionRequest(created, start);
        var payload = new IdentifiableSyndFeed(pr, feed);

        // When
        permissionUpdateService.updatePermissionRequest(payload);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertThat(res.latestMeterReadingEndDateTime()).isEmpty();
    }

    @Test
    void updatePermissionRequest_withoutIntervalBlock_doesNotReadyToPollEvent() throws FeedException {
        // Given
        var xml = XmlLoader.xmlFromResource("/xml/batch/BatchWithoutIntervalBlock.xml");
        var feed = new SyndFeedInput().build(new StringReader(xml));
        var start = LocalDate.of(2024, 9, 3);
        var created = ZonedDateTime.of(start, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        var pr = createPermissionRequest(created, start);
        var payload = new IdentifiableSyndFeed(pr, feed);

        // When
        permissionUpdateService.updatePermissionRequest(payload);

        // Then
        verify(outbox).commit(isA(UsPollingNotReadyEvent.class));
    }

    private static GreenButtonPermissionRequest createPermissionRequest(ZonedDateTime created, LocalDate start) {
        var meterReading = new MeterReading("pid", "1669851", created.minusDays(1));
        return new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                LocalDate.of(2024, 9, 4),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                created,
                "US",
                "company",
                "http://localhost",
                "scope",
                List.of(meterReading),
                "1111");
    }
}