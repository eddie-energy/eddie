// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.pmd;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.StandardStatusTypeList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionMarketDocumentFactoryTest {
    private static final UUID PERMISSION_ID = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private static final UUID DATA_NEED_ID = UUID.fromString("92831e2c-a01c-41b8-9db6-3f51670df7a5");
    private static final UUID AIIDA_ID = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private static final String CONNECTION_ID = "connection-id";
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ApplicationInformationService applicationInformationService;
    @Mock
    private ApplicationInformation applicationInformation;
    @Mock
    private Permission permission;
    @Mock
    private AiidaLocalDataNeed dataNeed;

    static Stream<Arguments> statuses() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.REVOKED, StandardStatusTypeList.WITHDRAWN),
                Arguments.of(PermissionProcessStatus.EXTERNALLY_TERMINATED, StandardStatusTypeList.DEACTIVATION),
                Arguments.of(PermissionProcessStatus.FULFILLED, StandardStatusTypeList.CONFIRMED)
        );
    }

    @ParameterizedTest
    @MethodSource("statuses")
    void create_mapsStatus_andFillsIdentifiers(PermissionProcessStatus status, StandardStatusTypeList expectedStatus) {
        // Given
        when(applicationInformationService.applicationInformation()).thenReturn(applicationInformation);
        when(applicationInformation.aiidaId()).thenReturn(AIIDA_ID);
        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.connectionId()).thenReturn(CONNECTION_ID);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.dataNeedId()).thenReturn(DATA_NEED_ID);
        when(permission.startTime()).thenReturn(Instant.parse("2026-06-01T00:00:00Z"));
        when(permission.expirationTime()).thenReturn(Instant.parse("2026-07-01T00:00:00Z"));
        var factory = new PermissionMarketDocumentFactory(applicationInformationService, clock);

        // When
        var envelope = factory.create(permission, status);

        // Then
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        var marketDocument = envelope.getMarketDocument();
        var activityRecord = marketDocument.getRequestPermission().getMktActivityRecords().getFirst();
        assertAll(
                () -> assertEquals(PERMISSION_ID.toString(), metaInformation.getRequestPermissionId()),
                () -> assertEquals(CONNECTION_ID, metaInformation.getConnectionId()),
                () -> assertEquals(DATA_NEED_ID.toString(), metaInformation.getDataNeedId()),
                () -> assertEquals(AIIDA_ID.toString(), metaInformation.getFinalCustomerId()),
                () -> assertEquals("aiida", metaInformation.getRegionConnector()),
                () -> assertEquals(PERMISSION_ID.toString(), marketDocument.getMRID()),
                () -> assertEquals("2026-06-01T00:00Z", marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals("2026-07-01T00:00Z", marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(expectedStatus.value(), activityRecord.getStatus()),
                () -> assertEquals(status.toString(), activityRecord.getDescription())
        );
    }

    @Test
    void create_withUnsupportedStatus_throws() {
        // Given
        var factory = new PermissionMarketDocumentFactory(applicationInformationService, clock);

        // When, Then
        assertThrows(IllegalArgumentException.class,
                     () -> factory.create(permission, PermissionProcessStatus.ACCEPTED));
    }
}
