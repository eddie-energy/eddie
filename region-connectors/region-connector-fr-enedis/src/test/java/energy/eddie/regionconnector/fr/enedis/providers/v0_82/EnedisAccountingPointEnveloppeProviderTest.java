// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.address.AddressData;
import energy.eddie.regionconnector.fr.enedis.dto.address.InstallationAddress;
import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisAccountingPointEnvelopeProviderTest {
    @Test
    void testGetEddieValidatedHistoricalDataMarketDocumentStream_publishesDocuments() {
        // Given
        var identifiableAccountingPointData = identifiableAccountingPointData();
        EnedisConfiguration enedisConfiguration = new EnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        IntermediateMarketDocumentFactory factory = new IntermediateMarketDocumentFactory(
                enedisConfiguration,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        var provider = new EnedisAccountingPointDataEnvelopeProvider(streams, factory);

        // When
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> {
                        streams.publish(identifiableAccountingPointData);
                        streams.close();
                    })
                    .assertNext(ap -> assertEquals(identifiableAccountingPointData.permissionRequest().permissionId(),
                                                   ap.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getPermissionid()))
                    .verifyComplete();
    }

    private IdentifiableAccountingPointData identifiableAccountingPointData() {
        var situation = new ContractualSituation(
                "usagePointId", null, null, null, null,
                null, null, null, null, null, null, null,
                List.of("C5"), null, null, null, null
        );
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(null, null, null, null, null, "75112"))
        );
        var permissionRequest = new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                null,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "permissionId",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
        return new IdentifiableAccountingPointData(
                permissionRequest,
                List.of(situation),
                generalData
        );
    }
}
