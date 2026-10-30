// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.UserSettingsService;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitNotificationServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final Instant STARTED_AT = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-10T10:05:00Z");
    private static final String RECIPIENT = "user@example.com";

    @Mock
    private UserSettingsService userSettingsService;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private Permission permission;

    private ConnectionLimitNotificationService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionLimitNotificationService(userSettingsService, mailSenderProvider, "aiida@example.com");
        lenient().when(permission.id()).thenReturn(PERMISSION_ID);
        lenient().when(permission.userId()).thenReturn(USER_ID);
        lenient().when(permission.displayName()).thenReturn("My permission");
    }

    @Test
    void notifyViolationStarted_sendsMailWithViolationDetails() {
        givenMailIsPossible();

        service.notifyViolationStarted(permission, violation(BigDecimal.valueOf(3), BigDecimal.valueOf(8)));

        var message = sentMessage();
        assertEquals("aiida@example.com", message.getFrom());
        assertEquals(RECIPIENT, message.getTo()[0]);
        assertEquals("Connection limits exceeded", message.getSubject());
        assertTrue(message.getText().contains("\"My permission\" exceeded the connection limits"));
        assertTrue(message.getText().contains("Time: " + STARTED_AT));
        assertTrue(message.getText().contains("Measured power: 9 kW"));
        assertTrue(message.getText().contains("Allowed limits: min 3 kW, max 8 kW"));
    }

    @Test
    void notifyViolationEnded_sendsMailWithRecoveredPower() {
        givenMailIsPossible();
        var violation = violation(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        violation.end(ENDED_AT);

        service.notifyViolationEnded(permission, violation, BigDecimal.valueOf(5));

        var message = sentMessage();
        assertEquals("Connection limits restored", message.getSubject());
        assertTrue(message.getText().contains("back within the connection limits"));
        assertTrue(message.getText().contains("Time: " + ENDED_AT));
        assertTrue(message.getText().contains("Measured power: 5 kW"));
    }

    @Test
    void givenOnlyMaxLimit_formatsOnlyMax() {
        givenMailIsPossible();

        service.notifyViolationStarted(permission, violation(null, BigDecimal.valueOf(8)));

        assertTrue(sentMessage().getText().contains("Allowed limits: max 8 kW"));
    }

    @Test
    void givenNoContactEmail_doesNotSend() {
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.empty());

        service.notifyViolationStarted(permission, violation(BigDecimal.valueOf(3), BigDecimal.valueOf(8)));

        verifyNoInteractions(mailSenderProvider);
        verifyNoInteractions(mailSender);
    }

    @Test
    void givenNoMailServer_doesNotSend() {
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.of(RECIPIENT));
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        service.notifyViolationStarted(permission, violation(BigDecimal.valueOf(3), BigDecimal.valueOf(8)));

        verifyNoInteractions(mailSender);
    }

    @Test
    void givenMailFails_doesNotThrow() {
        givenMailIsPossible();
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.notifyViolationStarted(permission,
                                                                violation(BigDecimal.valueOf(3),
                                                                          BigDecimal.valueOf(8))));
    }

    @Test
    void givenLookupFails_doesNotThrow() {
        when(userSettingsService.findContactEmail(USER_ID)).thenThrow(new IllegalStateException("database down"));

        assertDoesNotThrow(() -> service.notifyViolationStarted(permission,
                                                                violation(BigDecimal.valueOf(3),
                                                                          BigDecimal.valueOf(8))));
        verifyNoInteractions(mailSender);
    }

    private void givenMailIsPossible() {
        when(userSettingsService.findContactEmail(USER_ID)).thenReturn(Optional.of(RECIPIENT));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    }

    private ConnectionLimitViolation violation(BigDecimal minLimitKw, BigDecimal maxLimitKw) {
        return new ConnectionLimitViolation(PERMISSION_ID,
                                            UUID.randomUUID(),
                                            STARTED_AT,
                                            null,
                                            minLimitKw,
                                            maxLimitKw,
                                            BigDecimal.valueOf(9));
    }

    private SimpleMailMessage sentMessage() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
