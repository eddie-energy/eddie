// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

class EnerginetValidatedHistoricalDataMarketDocumentProviderTest {

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "token",
                                                now.toLocalDate(),
                                                now.toLocalDate(),
                                                Granularity.P1D,
                                                null,
                                                PermissionProcessStatus.ACCEPTED,
                                                now,
                                                null);
        var response = DtoLoader.loadValidatedHistoricalData().getResult();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst()));
                        streams.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument_alsoEmitsEquivalentV112() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "token",
                                                now.toLocalDate(),
                                                now.toLocalDate(),
                                                Granularity.P1D,
                                                null,
                                                PermissionProcessStatus.ACCEPTED,
                                                now,
                                                null);
        var response = DtoLoader.loadValidatedHistoricalData().getResult();

        // When
        StepVerifier.create(reactor.core.publisher.Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()).map(pair -> {
            var source = pair.getT1();
            var target = pair.getT2();
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationPermissionId(), target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationDataNeedId(), target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
            var sourceSeries = source.getMarketDocument().getTimeSeries();
            var targetSeries = target.getVHDMarketDocument().getTimeSeries();
            org.junit.jupiter.api.Assertions.assertEquals(sourceSeries.size(), targetSeries.size());
            for (int i = 0; i < sourceSeries.size(); i++) {
                var left = sourceSeries.get(i);
                var right = targetSeries.get(i);
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyMeasurementUnitName(), right.getEnergyMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyQualityMeasurementUnitName(), right.getEnergyQualityMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getFlowDirectionDirection(), right.getFlowDirectionDirection());
                org.junit.jupiter.api.Assertions.assertEquals(left.getPeriods().size(), right.getSeriesPeriods().size());
                for (int j = 0; j < left.getPeriods().size(); j++) {
                    var lp = left.getPeriods().get(j);
                    var rp = right.getSeriesPeriods().get(j);
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getResolution(), rp.getResolution());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getStart(), rp.getTimeInterval().getStart());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getEnd(), rp.getTimeInterval().getEnd());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getPoints().size(), rp.getPoints().size());
                    for (int k = 0; k < lp.getPoints().size(); k++) {
                        var l = lp.getPoints().get(k);
                        var r = rp.getPoints().get(k);
                        org.junit.jupiter.api.Assertions.assertEquals(l.getPosition(), r.getPosition());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuantity(), r.getEnergyQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuality(), r.getEnergyQuantityQuality());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuantity(), r.getEnergyQualityQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuality(), r.getEnergyQualityQuantityQuality());
                    }
                }
            }
            if (!targetSeries.isEmpty()) {
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                    var serialized = new energy.eddie.cim.serde.XmlMessageSerde().serialize(target);
                    org.junit.jupiter.api.Assertions.assertTrue(energy.eddie.cim.testing.XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
                });
            }
            return target;
        }))
                    .then(() -> {
                        streams.publish(new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst()));
                        streams.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }
}