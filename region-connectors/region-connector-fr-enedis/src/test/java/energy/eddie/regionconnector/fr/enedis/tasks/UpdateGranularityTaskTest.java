package energy.eddie.regionconnector.fr.enedis.tasks;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisApiError;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrGranularityUpdateEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.v0_82.SimpleFrEnedisPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateGranularityTaskTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private UpdateGranularityTask updateGranularityTask;

    @ParameterizedTest
    @MethodSource("exceptionSource")
    void update_withWrongError_doesNothing(Exception exception) {
        // Given
        var pr = createPermissionRequest(Granularity.PT15M);

        // When
        updateGranularityTask.update(pr, exception);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void update_withMaxGranularityReached_emitsUnfulfillable() {
        // Given
        var exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST,
                "text",
                null,
                null,
                StandardCharsets.UTF_8,
                null
        );
        exception.setBodyDecodeFunction(r -> new EnedisApiError("no_data_found", "no data"));
        var pr = createPermissionRequest(Granularity.P1D);

        // When
        updateGranularityTask.update(pr, exception);

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())
        )));
    }

    @Test
    void update_withDataNeedsAllowedMaxGranularityReached_emitsUnfulfillable() {
        // Given
        var exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST,
                "text",
                null,
                null,
                StandardCharsets.UTF_8,
                null
        );
        exception.setBodyDecodeFunction(r -> new EnedisApiError("no_data_found", "no data"));
        var pr = createPermissionRequest(Granularity.PT30M);
        var today = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate(eq("dataNeedId"), any()))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(), timeframe, timeframe));

        // When
        updateGranularityTask.update(pr, exception);

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())
        )));
    }

    @Test
    void update_withPossibleHigherGranularity_emitsNewGranularity() {
        // Given
        var exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST,
                "text",
                null,
                null,
                StandardCharsets.UTF_8,
                null
        );
        exception.setBodyDecodeFunction(r -> new EnedisApiError("no_data_found", "no data"));
        var pr = createPermissionRequest(Granularity.PT30M);
        var today = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate(eq("dataNeedId"), any()))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.P1D), timeframe, timeframe));

        // When
        updateGranularityTask.update(pr, exception);

        // Then
        verify(outbox).commit(assertArg(event -> assertThat(event)
                .asInstanceOf(InstanceOfAssertFactories.type(FrGranularityUpdateEvent.class))
                .satisfies(ev -> {
                    assertThat(ev.permissionId()).isEqualTo("pid");
                    assertThat(ev.status()).isEqualTo(PermissionProcessStatus.ACCEPTED);
                    assertThat(ev.granularity()).isEqualTo(Granularity.P1D);
                })
        ));
    }

    private static @NotNull SimpleFrEnedisPermissionRequest createPermissionRequest(Granularity granularity) {
        return new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                granularity,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "pid",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
    }

    private static Stream<Arguments> exceptionSource() {
        var wrongErrorException = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST,
                "text",
                null,
                null,
                null,
                null
        );
        Function<ResolvableType, ?> decodeBodyFunction = type ->
                new EnedisApiError("technical_error", "no measure found for this usage point");
        wrongErrorException.setBodyDecodeFunction(decodeBodyFunction);
        var noBodyException = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST,
                "text",
                null,
                null,
                null,
                null
        );
        noBodyException.setBodyDecodeFunction(r -> null);
        return Stream.of(
                Arguments.of(new RuntimeException()),
                Arguments.of(wrongErrorException),
                Arguments.of(noBodyException)
        );
    }
}