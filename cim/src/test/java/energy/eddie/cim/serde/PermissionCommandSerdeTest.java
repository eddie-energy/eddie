// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.agnostic.PermissionCommand;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the polymorphic (de)serialization of {@link PermissionCommand}, discriminated on {@code action}.
 */
class PermissionCommandSerdeTest {
    private static final UUID PERMISSION_ID = UUID.fromString("a1f58555-4b87-4624-996c-ec4cf6ddb6c3");

    @Test
    void json_roundTripsUpdateTransmissionSchedule_preservingSubtype() throws Exception {
        // Given
        var serde = new JsonMessageSerde();
        var command = new PermissionCommand.UpdateTransmissionSchedule("rc-1", PERMISSION_ID, "0 */1 * * * *");

        // When
        var bytes = serde.serialize(command);
        var json = new String(bytes, StandardCharsets.UTF_8);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        assertTrue(json.contains("\"action\":\"UPDATE_TRANSMISSION_SCHEDULE\""));
        var updateTransmissionSchedule = assertInstanceOf(PermissionCommand.UpdateTransmissionSchedule.class, result);
        assertEquals(PERMISSION_ID, updateTransmissionSchedule.permissionId());
        assertEquals("rc-1", updateTransmissionSchedule.regionConnectorId());
        assertEquals("0 */1 * * * *", updateTransmissionSchedule.transmissionSchedule());
    }

    @ParameterizedTest
    @MethodSource("limitDefaults")
    void json_roundTripsUpdateLimitDefaults_preservingSubtype(
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw
    ) throws Exception {
        // Given
        var serde = new JsonMessageSerde();
        var command = new PermissionCommand.UpdateLimitDefaults("rc-1", PERMISSION_ID, minLimitKw, maxLimitKw);

        // When
        var bytes = serde.serialize(command);
        var json = new String(bytes, StandardCharsets.UTF_8);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        assertTrue(json.contains("\"action\":\"UPDATE_LIMIT_DEFAULTS\""));
        assertTrue(json.contains("\"minLimitKw\":%s".formatted(minLimitKw == null ? "null" : minLimitKw)));
        assertTrue(json.contains("\"maxLimitKw\":%s".formatted(maxLimitKw == null ? "null" : maxLimitKw)));
        var updateLimitDefaults = assertInstanceOf(PermissionCommand.UpdateLimitDefaults.class, result);
        assertEquals(PERMISSION_ID, updateLimitDefaults.permissionId());
        assertEquals("rc-1", updateLimitDefaults.regionConnectorId());
        assertEquals(minLimitKw, updateLimitDefaults.minLimitKw());
        assertEquals(maxLimitKw, updateLimitDefaults.maxLimitKw());
    }

    @Test
    void json_roundTripsSetTransmissionEnabled_preservingSubtype() throws Exception {
        // Given
        var serde = new JsonMessageSerde();
        var command = new PermissionCommand.SetTransmissionEnabled("rc-1", PERMISSION_ID, true);

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
        var command = new PermissionCommand.UpdateTransmissionSchedule("rc-1", PERMISSION_ID, "0 */1 * * * *");

        // When
        var bytes = serde.serialize(command);
        var result = serde.deserialize(bytes, PermissionCommand.class);

        // Then
        var updateTransmissionSchedule = assertInstanceOf(PermissionCommand.UpdateTransmissionSchedule.class, result);
        assertEquals(PERMISSION_ID, updateTransmissionSchedule.permissionId());
        assertEquals("0 */1 * * * *", updateTransmissionSchedule.transmissionSchedule());
    }

    static Stream<Arguments> limitDefaults() {
        return Stream.of(Arguments.of(BigDecimal.ONE, BigDecimal.TEN),
                         Arguments.of(BigDecimal.ONE, null),
                         Arguments.of(null, null));
    }
}