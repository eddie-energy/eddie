package energy.eddie.regionconnector.us.green.button.providers.agnostic;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsRawDataProviderTest {
    private final SyndFeedInput input = new SyndFeedInput();
    @Mock
    private PublishService publishService;
    @InjectMocks
    private UsRawDataProvider provider;

    @Test
    void emitsRawSyndData() throws FeedException, IOException {
        // Given
        var data = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(getClass().getResourceAsStream("/xml/usagepoint/UsagePoint.xml")),
                        StandardCharsets.UTF_8
                )
        )
                .lines()
                .collect(Collectors.joining("\n"));
        var xml = input.build(new XmlReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))));
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest("pid",
                                                  "cid",
                                                  "dnid",
                                                  now,
                                                  now,
                                                  Granularity.PT15M,
                                                  PermissionProcessStatus.ACCEPTED,
                                                  ZonedDateTime.now(ZoneOffset.UTC),
                                                  "US",
                                                  "cid",
                                                  "http://localhost",
                                                  "other", "1111");
        var identifiableSyndFeed = new IdentifiableSyndFeed(pr, xml);
        when(publishService.flux())
                .thenReturn(Flux.just(identifiableSyndFeed));

        // When
        var flux = provider.getRawDataStream();

        // Then
        StepVerifier.create(flux)
                    .assertNext(id -> assertAll(
                            () -> assertEquals("pid", id.permissionId()),
                            () -> assertEquals("cid", id.connectionId()),
                            () -> assertEquals("dnid", id.dataNeedId()),
                            () -> assertNotNull(id.rawPayload())
                    ))
                    .verifyComplete();
    }
}