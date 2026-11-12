// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataMarketDocumentProviderV112Test {
    @Mock
    private DocumentStreams streams;

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_mapsSimulatedMeterReadings() {
        // Given
        var reading = new SimulatedMeterReading("cid",
                                                "dnid",
                                                "pid",
                                                "mid",
                                                ZonedDateTime.of(2025, 11, 6, 0, 0, 0, 0, ZoneOffset.UTC),
                                                PT15M.toString(),
                                                List.of(new Measurement(10.0, Measurement.MeasurementType.MEASURED)));
        when(streams.getSimulatedMeterReadingStream()).thenReturn(Flux.just(reading));
        var cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "EP-ID");
        var provider = new ValidatedHistoricalDataMarketDocumentProviderV112(streams, cimConfig);

        // When
        var res = provider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .assertNext(envelope -> assertEquals(
                            "pid",
                            envelope.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()
                    ))
                    .verifyComplete();
    }
}
