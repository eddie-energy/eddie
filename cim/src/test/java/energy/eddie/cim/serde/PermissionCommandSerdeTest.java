// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.agnostic.PermissionCommand;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the polymorphic (de)serialization of {@link PermissionCommand}, discriminated on {@code action}.
 */
class PermissionCommandSerdeTest {
    private static final UUID PERMISSION_ID = UUID.fromString("a1f58555-4b87-4624-996c-ec4cf6ddb6c3");

    @Test
    void json_roundTripsUpdateSchedule_preservingSubtype() throws Exception {
        // Given
        var serde = new JsonMessageSerde();
        var command = new PermissionCommand.UpdateSchedule("rc-1", PERMISSION_ID, ZonedDateTime.now(), "0 */1 * * * *");

        // When
        var bytes = serde.serialize(command);
        var json = new String(bytes, StandardCharsets.UTF_8);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        assertTrue(json.contains("\"action\":\"UPDATE_SCHEDULE\""));
        var updateSchedule = assertInstanceOf(PermissionCommand.UpdateSchedule.class, result);
        assertEquals(PERMISSION_ID, updateSchedule.permissionId());
        assertEquals("rc-1", updateSchedule.regionConnectorId());
        assertEquals("0 */1 * * * *", updateSchedule.transmissionSchedule());
    }

    @Test
    void json_roundTripsSetTransmissionEnabled_preservingSubtype() throws Exception {
        // Given
        var serde = new JsonMessageSerde();
        var command = new PermissionCommand.SetTransmissionEnabled("rc-1", PERMISSION_ID, ZonedDateTime.now(), true);

        // When
        var bytes = serde.serialize(command);
        var json = new String(bytes, StandardCharsets.UTF_8);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        assertTrue(json.contains("\"action\":\"SET_TRANSMISSION_ENABLED\""));
        var setEnabled = assertInstanceOf(PermissionCommand.SetTransmissionEnabled.class, result);
        assertEquals(PERMISSION_ID, setEnabled.permissionId());
        assertTrue(setEnabled.enabled());
    }

    @Test
    void xml_roundTripsCommand_preservingSubtype() throws Exception {
        // Given
        var serde = new XmlMessageSerde();
        var command = new PermissionCommand.UpdateSchedule("rc-1", PERMISSION_ID, ZonedDateTime.now(), "0 */1 * * * *");

        // When
        var bytes = serde.serialize(command);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        var updateSchedule = assertInstanceOf(PermissionCommand.UpdateSchedule.class, result);
        assertEquals(PERMISSION_ID, updateSchedule.permissionId());
        assertEquals("0 */1 * * * *", updateSchedule.transmissionSchedule());
    }
}