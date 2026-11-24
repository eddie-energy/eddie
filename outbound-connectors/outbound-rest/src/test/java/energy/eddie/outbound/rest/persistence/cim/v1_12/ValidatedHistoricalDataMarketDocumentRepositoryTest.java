// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.cim.v1_12;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import energy.eddie.cim.v1_12.vhd.VHDMarketDocument;
import energy.eddie.outbound.rest.RestTestConfig;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.model.cim.v1_12.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.PersistenceConfig;
import energy.eddie.outbound.rest.persistence.cim.v1_04.ValidatedHistoricalDataMarketDocumentV1_04Repository;
import energy.eddie.outbound.rest.persistence.specifications.CimSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest()
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext
@Import({PersistenceConfig.class, RestTestConfig.class})
class ValidatedHistoricalDataMarketDocumentRepositoryTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private ValidatedHistoricalDataMarketDocumentRepository repository;
    @Autowired
    private ValidatedHistoricalDataMarketDocumentV1_04Repository v104Repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        v104Repository.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("filters")
    void findAll_withV112Query_returnsMatchingDocument(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String countryCode,
            String regionConnectorId
    ) {
        // Given
        var expected = repository.save(new ValidatedHistoricalDataMarketDocumentModel(
                envelope("pid", "cid", "dnid", "AT", "at-eda")
        ));
        repository.save(new ValidatedHistoricalDataMarketDocumentModel(
                envelope("other-pid", "other-cid", "other-dnid", "FR", "fr-enedis")
        ));
        var spec = CimSpecification.<ValidatedHistoricalDataMarketDocumentModel>buildQueryForV1_12(
                Optional.ofNullable(permissionId),
                Optional.ofNullable(connectionId),
                Optional.ofNullable(dataNeedId),
                Optional.ofNullable(countryCode),
                Optional.ofNullable(regionConnectorId),
                Optional.empty(),
                Optional.empty()
        );

        // When
        var res = repository.findAll(spec);

        // Then
        assertThat(res)
                .singleElement()
                .extracting(ValidatedHistoricalDataMarketDocumentModel::id)
                .isEqualTo(expected.id());
        assertThat(res.getFirst().payload().getVHDMarketDocument().getMRID()).isEqualTo("mrid-pid");
    }

    @Test
    void findAll_isIsolatedFromV104Documents() {
        // Given
        v104Repository.save(new ValidatedHistoricalDataMarketDocumentModelV1_04(
                new VHDEnvelope().withMessageDocumentHeaderMetaInformationPermissionId("pid")
        ));
        repository.save(new ValidatedHistoricalDataMarketDocumentModel(envelope("pid", "cid", "dnid", "AT", "at-eda")));

        // When
        var v112 = repository.findAll();
        var v104 = v104Repository.findAll();

        // Then
        assertThat(v112).hasSize(1);
        assertThat(v104).hasSize(1);
    }

    private static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of("pid", null, null, null, null),
                Arguments.of(null, "cid", null, null, null),
                Arguments.of(null, null, "dnid", null, null),
                // A null value means that the filter is not used, the country code filter is case-insensitive
                Arguments.of(null, null, null, "at", null),
                Arguments.of(null, null, null, null, "at-eda"),
                Arguments.of("pid",
                             "cid",
                             "dnid",
                             "AT",
                             "at-eda")
        );
    }

    private static Envelope envelope(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String countryCode,
            String regionConnector
    ) {
        return new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withRequestPermissionId(permissionId)
                                                .withConnectionId(connectionId)
                                                .withDataNeedId(dataNeedId)
                                                .withDocumentType("validated-historical-data-market-document")
                                                .withRegionCountry(countryCode)
                                                .withRegionConnector(regionConnector)
                                )
                )
                .withVHDMarketDocument(new VHDMarketDocument().withMRID("mrid-" + permissionId));
    }
}
