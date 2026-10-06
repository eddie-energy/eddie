// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.ConnectionLimitViolationRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.UserSettingsService;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitNotificationServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private static final Instant TIMESTAMP = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant CLOCK_INSTANT = Instant.parse("2026-07-10T11:00:00Z");
    private static final String RECIPIENT = "user@example.com";

    @Mock
    private AiidaRecordRepository aiidaRecordRepository;
    @Mock
    private ConnectionLimitViolationRepository connectionLimitViolationRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private ConnectionLimitService connectionLimitService;
    @Mock
    private UserSettingsService userSettingsService;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private Permission permission;

    private final List<ConnectionLimitViolation> storedViolations = new ArrayList<>();
    private ConnectionLimitNotificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(connectionLimitViolationRepository.save(any(ConnectionLimitViolation.class)))
                 .thenAnswer(invocation -> {
                     var violation = invocation.<ConnectionLimitViolation>getArgument(0);
                     if (!storedViolations.contains(violation)) {
                         storedViolations.add(violation);
                     }
                     return violation;
                 });
        lenient().when(connectionLimitViolationRepository.findByEndedAtIsNull())
                 .thenAnswer(invocation -> storedViolations.stream().filter(v -> v.endedAt() == null).toList());

        service = new ConnectionLimitNotificationService(aiidaRecordRepository,
                                                         connectionLimitViolationRepository,
                                                         permissionRepository,
                                                         connectionLimitService,
                                                         userSettingsService,
                                                         mailSenderProvider,
                                                         Clock.fixed(CLOCK_INSTANT, ZoneOffset.UTC),
                                                         "aiida@example.com");
    }

    @Test
    void givenNoAssignment_doesNotNotify() {
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNull()).thenReturn(List.of());

        service.checkViolations();

        verifyNoInteractions(mailSender);
    }

    @Test
    void givenFirstSweep_baselinesToLatestRecordAndSkipsHistory() {
        givenAssignment();
        givenLatestRecordId(10);
        givenNewRecords(10);

        service.checkViolations();

        verify(aiidaRecordRepository).findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, 10L);
        verifyNoInteractions(mailSender);
    }

    @Test
    void givenViolation_notifiesUser() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")));

        service.checkViolations();

        var message = sentMessage();
        assertEquals(RECIPIENT, message.getTo()[0]);
        assertEquals("Connection limits exceeded", message.getSubject());
        assertTrue(message.getText().contains("Measured power: 9 kW"));
    }

    @Test
    void givenViolationThenBackWithinLimits_notifiesRecovery() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")), record(7, imported("5.0")));

        service.checkViolations();

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        assertEquals("Connection limits exceeded", captor.getAllValues().get(0).getSubject());
        assertEquals("Connection limits restored", captor.getAllValues().get(1).getSubject());
    }

    @Test
    void givenRepeatedViolations_notifiesEachTime() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")), record(7, imported("5.0")), record(8, imported("2.0")));

        service.checkViolations();

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(3)).send(captor.capture());
        verify(connectionLimitService, times(1)).getConnectionLimits(any(), any(), any(), any(), any());
        assertEquals(2, storedViolations.size());
        assertEquals(TIMESTAMP, storedViolations.get(0).startedAt());
        assertEquals(TIMESTAMP, storedViolations.get(0).endedAt());
        assertEquals(null, storedViolations.get(1).endedAt());
        assertEquals(List.of("Connection limits exceeded",
                             "Connection limits restored",
                             "Connection limits exceeded"),
                     captor.getAllValues().stream().map(SimpleMailMessage::getSubject).toList());
    }

    @Test
    void givenAssignmentRemovedWhileViolated_notifiesAgainAfterReassignment() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")));

        service.checkViolations();
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNull()).thenReturn(List.of());
        service.checkViolations();
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNull()).thenReturn(List.of(permission));
        service.checkViolations();

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        assertEquals(2, storedViolations.size());
        assertEquals(CLOCK_INSTANT, storedViolations.getFirst().endedAt());
    }

    @Test
    void givenViolationOngoingBeforeRestart_doesNotNotifyAgain() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        storedViolations.add(new ConnectionLimitViolation(PERMISSION_ID,
                                                          DATA_SOURCE_ID,
                                                          TIMESTAMP.minusSeconds(60),
                                                          null,
                                                          BigDecimal.valueOf(3),
                                                          BigDecimal.valueOf(8),
                                                          BigDecimal.valueOf(9)));
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.5")));

        service.checkViolations();

        verifyNoInteractions(mailSender);
        assertEquals(0, BigDecimal.valueOf(9.5).compareTo(storedViolations.getFirst().peakPowerKw()));
    }

    @Test
    void givenWorseValues_storesPeakAndNotifiesOnce() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")), record(7, imported("10.0")), record(8, imported("9.5")));

        service.checkViolations();

        verify(mailSender).send(any(SimpleMailMessage.class));
        assertEquals(1, storedViolations.size());
        assertEquals(0, BigDecimal.valueOf(9).compareTo(storedViolations.getFirst().startPowerKw()));
        assertEquals(0, BigDecimal.TEN.compareTo(storedViolations.getFirst().peakPowerKw()));
        assertEquals(null, storedViolations.getFirst().endedAt());
    }

    @Test
    void givenPowerInWatt_convertsToKilowatt() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, value(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "9000", UnitOfMeasurement.WATT)));

        service.checkViolations();

        assertTrue(sentMessage().getText().contains("Measured power: 9 kW"));
    }

    @Test
    void givenMailFails_continuesWithoutRetrying() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")), record(7, imported("5.0")));
        givenNewRecords(7);
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        service.checkViolations();
        service.checkViolations();

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(aiidaRecordRepository).findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, 7L);
    }

    @Test
    void givenRecordsAlreadyChecked_doesNotCheckThemAgain() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")));
        givenNewRecords(6);

        service.checkViolations();
        service.checkViolations();

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(aiidaRecordRepository).findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, 6L);
    }

    @Test
    void givenNoPowerValue_doesNotNotify() {
        givenAssignment();
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, energyOnly()));

        service.checkViolations();

        verifyNoInteractions(mailSender);
    }

    @Test
    void givenLowerThanMinLimit_notifiesUser() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(-5), null);
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, exported("7.0")));
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.of(RECIPIENT));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        service.checkViolations();

        assertTrue(sentMessage().getText().contains("Measured power: -7 kW"));
    }

    @Test
    void givenNoContactEmail_doesNotNotify() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")));
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.empty());

        service.checkViolations();

        verifyNoInteractions(mailSender);
        verifyNoInteractions(mailSenderProvider);
    }

    @Test
    void givenNoMailSender_doesNotNotify() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        givenLatestRecordId(5);
        givenNewRecords(5, record(6, imported("9.0")));
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.of(RECIPIENT));
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        service.checkViolations();

        verifyNoInteractions(mailSender);
    }

    private void givenViolationContext() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.of(RECIPIENT));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    }

    private void givenAssignment() {
        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.monitoringDataSourceId()).thenReturn(DATA_SOURCE_ID);
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNull()).thenReturn(List.of(permission));
    }

    private void givenLatestRecordId(long id) {
        var latest = mock(AiidaRecord.class);
        when(latest.id()).thenReturn(id);
        when(aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID)).thenReturn(Optional.of(latest));
    }

    private void givenNewRecords(long afterId, AiidaRecord... records) {
        when(aiidaRecordRepository.findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, afterId)).thenReturn(
                List.of(records));
    }

    private void givenEffectiveLimit(BigDecimal min, BigDecimal max) {
        when(permission.userId()).thenReturn(USER_ID);
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any(), any())).thenReturn(List.of(
                new ConnectionLimitDto(PERMISSION_ID,
                                       "",
                                       null,
                                       TIMESTAMP,
                                       TIMESTAMP.plusMillis(1),
                                       min,
                                       max)));
    }

    private AiidaRecord record(long id, AiidaRecordValue... values) {
        var record = mock(AiidaRecord.class);
        when(record.id()).thenReturn(id);
        when(record.aiidaRecordValues()).thenReturn(List.of(values));
        lenient().when(record.timestamp()).thenReturn(TIMESTAMP);
        return record;
    }

    private AiidaRecordValue imported(String value) {
        return value(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, value);
    }

    private AiidaRecordValue exported(String value) {
        return value(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, value);
    }

    private AiidaRecordValue energyOnly() {
        return value(ObisCode.POSITIVE_ACTIVE_ENERGY, "1");
    }

    private AiidaRecordValue value(ObisCode dataTag, String value) {
        return value(dataTag, value, UnitOfMeasurement.KILO_WATT);
    }

    private AiidaRecordValue value(ObisCode dataTag, String value, UnitOfMeasurement unit) {
        return new AiidaRecordValue(dataTag.toString(), dataTag, value, unit, value, unit);
    }

    private SimpleMailMessage sentMessage() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
