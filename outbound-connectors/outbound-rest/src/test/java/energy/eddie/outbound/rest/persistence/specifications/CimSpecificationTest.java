// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.specifications;

import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.cim.v1_04.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.RestTestConfig;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.model.cim.v1_12.NearRealTimeDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.PersistenceConfig;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_04.ValidatedHistoricalDataMarketDocumentV1_04Repository;
import energy.eddie.outbound.rest.persistence.cim.v1_12.NearRealTimeDataMarketDocumentRepository;
import energy.eddie.outbound.shared.TopicStructure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PersistenceConfig.class, RestTestConfig.class})
@Testcontainers
@DirtiesContext
class CimSpecificationTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private PermissionMarketDocumentRepository pmdRepository;
    @Autowired
    private ValidatedHistoricalDataMarketDocumentV1_04Repository vhdRepository;
    @Autowired
    private NearRealTimeDataMarketDocumentRepository rtdRepository;

    @Test
    void givenPermissionMarketDocumentsAndSpecification_whenQueryRepository_thenReturnDocument() {
        // Given
        var pid = UUID.randomUUID().toString();
        var dnid = UUID.randomUUID().toString();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusHours(1);
        var to = now.plusHours(1);
        var payload = new PermissionEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeaderComplexType()
                                .withCreationDateTime(now)
                                .withMessageDocumentHeaderMetaInformation(
                                        new MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid(pid)
                                                .withConnectionid("1")
                                                .withDataNeedid(dnid)
                                                .withDataType(TopicStructure.DocumentTypes.PERMISSION_MD.value())
                                                .withMessageDocumentHeaderRegion(
                                                        new MessageDocumentHeaderRegionComplexType()
                                                                .withConnector("at-eda")
                                                                .withCountry(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                )
                );
        pmdRepository.save(new PermissionMarketDocumentModel(payload));
        PredicateSpecification<PermissionMarketDocumentModel> specs = CimSpecification.buildQueryForV0_82(
                Optional.of(pid),
                Optional.of("1"),
                Optional.of(dnid),
                Optional.of("NAT"),
                Optional.of("at-eda"),
                Optional.of(from),
                Optional.of(to)
        );

        // When
        var res = pmdRepository.findAll(specs);

        // Then
        assertThat(res).hasSize(1);
    }


    @Test
    void givenPermissionMarketDocumentsAndSpecification_whenQueryRepository_thenReturnNoDocument() {
        // Given
        var pid = UUID.randomUUID().toString();
        var dnid = UUID.randomUUID().toString();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusHours(1);
        var to = now.plusHours(1);
        var payload = new PermissionEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeaderComplexType()
                                .withCreationDateTime(now)
                                .withMessageDocumentHeaderMetaInformation(
                                        new MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid(pid)
                                                .withConnectionid("1")
                                                .withDataNeedid(dnid)
                                                .withDataType(TopicStructure.DocumentTypes.PERMISSION_MD.value())
                                                .withMessageDocumentHeaderRegion(
                                                        new MessageDocumentHeaderRegionComplexType()
                                                                .withConnector("at-eda")
                                                                .withCountry(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                )
                );
        pmdRepository.save(new PermissionMarketDocumentModel(payload));
        PredicateSpecification<PermissionMarketDocumentModel> specs = CimSpecification.buildQueryForV0_82(
                Optional.of(pid),
                Optional.of("2"),
                Optional.of(dnid),
                Optional.of("NAT"),
                Optional.of("at-eda"),
                Optional.of(from),
                Optional.of(to)
        );

        // When
        var res = pmdRepository.findAll(specs);

        // Then
        assertThat(res).isEmpty();
    }


    @Test
    void givenValidatedHistoricalDataMarketDocumentsV1_04AndSpecification_whenQueryRepository_thenReturnDocument() {
        // Given
        var pid = UUID.randomUUID().toString();
        var dnid = UUID.randomUUID().toString();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusHours(1);
        var to = now.plusHours(1);
        var payload = new VHDEnvelope()
                .withMessageDocumentHeaderCreationDateTime(now)
                .withMessageDocumentHeaderMetaInformationPermissionId(pid)
                .withMessageDocumentHeaderMetaInformationConnectionId("1")
                .withMessageDocumentHeaderMetaInformationDataNeedId(dnid)
                .withMessageDocumentHeaderMetaInformationDocumentType(TopicStructure.DocumentTypes.VALIDATED_HISTORICAL_DATA_MD.value())
                .withMessageDocumentHeaderMetaInformationRegionConnector("at-eda")
                .withMessageDocumentHeaderMetaInformationRegionCountry(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value());
        vhdRepository.save(new ValidatedHistoricalDataMarketDocumentModelV1_04(payload));
        PredicateSpecification<ValidatedHistoricalDataMarketDocumentModelV1_04> specs = CimSpecification.buildQueryForV1_04(
                Optional.of(pid),
                Optional.of("1"),
                Optional.of(dnid),
                Optional.of("NAT"),
                Optional.of("at-eda"),
                Optional.of(from),
                Optional.of(to)
        );

        // When
        var res = vhdRepository.findAll(specs);

        // Then
        assertThat(res).hasSize(1);
    }

    @Test
    void givenValidatedHistoricalDataMarketDocumentsV1_04AndSpecification_whenQueryRepository_thenReturnNoDocument() {
        // Given
        var pid = UUID.randomUUID().toString();
        var dnid = UUID.randomUUID().toString();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusHours(1);
        var to = now.plusHours(1);
        var payload = new VHDEnvelope()
                .withMessageDocumentHeaderCreationDateTime(now)
                .withMessageDocumentHeaderMetaInformationPermissionId(pid)
                .withMessageDocumentHeaderMetaInformationConnectionId("1")
                .withMessageDocumentHeaderMetaInformationDataNeedId(dnid)
                .withMessageDocumentHeaderMetaInformationDocumentType(TopicStructure.DocumentTypes.VALIDATED_HISTORICAL_DATA_MD.value())
                .withMessageDocumentHeaderMetaInformationRegionConnector("at-eda")
                .withMessageDocumentHeaderMetaInformationRegionCountry(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value());
        vhdRepository.save(new ValidatedHistoricalDataMarketDocumentModelV1_04(payload));
        PredicateSpecification<ValidatedHistoricalDataMarketDocumentModelV1_04> specs = CimSpecification.buildQueryForV1_04(
                Optional.of(pid),
                Optional.of("2"),
                Optional.of(dnid),
                Optional.of("NAT"),
                Optional.of("at-eda"),
                Optional.of(from),
                Optional.of(to)
        );

        // When
        var res = vhdRepository.findAll(specs);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void givenRealTimeDataMarketDocumentsV1_12AndSpecification_whenQueryRepository_thenReturnDocument() {
        // Given
        var pid = UUID.randomUUID().toString();
        var dnid = UUID.randomUUID().toString();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var from = now.minusHours(1);
        var to = now.plusHours(1);
        var payload = new energy.eddie.cim.v1_12.rtd.RTDEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v1_12.rtd.MessageDocumentHeader()
                                .withCreationDateTime(now)
                                .withMetaInformation(
                                        new energy.eddie.cim.v1_12.rtd.MetaInformation()
                                                .withRequestPermissionId(pid)
                                                .withConnectionId("1")
                                                .withDataNeedId(dnid)
                                                .withDocumentType(TopicStructure.DocumentTypes.NEAR_REAL_TIME_DATA_MD.value())
                                                .withRegionConnector("aiida")
                                                .withRegionCountry(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())));
        rtdRepository.save(new NearRealTimeDataMarketDocumentModel(payload));
        PredicateSpecification<NearRealTimeDataMarketDocumentModel> specs = CimSpecification.buildQueryForV1_12(
                Optional.of(pid),
                Optional.of("1"),
                Optional.of(dnid),
                Optional.of("NAT"),
                Optional.of("aiida"),
                Optional.of(from),
                Optional.of(to)
        );

        // When
        var res = rtdRepository.findAll(specs);

        // Then
        assertThat(res).hasSize(1);
    }
}