// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class StatusEmissionStepTest {
    @Test
    void testExecute_emitsPermissionMarketDocument() {
        // Given
        var ctx = TestSimulationContext.create();
        var streams = ctx.documentStreams();
        var step = new StatusEmissionStep(PermissionProcessStatus.CREATED);

        // When
        step.execute(ctx);

        // Then
        StepVerifier.create(streams.getPermissionMarketDocumentStream())
                    .then(streams::close)
                    .assertNext(pmd ->
                                        assertEquals(ctx.permissionId(), pmd.getPermissionMarketDocument().getMRID())
                    )
                    .verifyComplete();
    }

    @Test
    void testExecute_emitsConnectionStatusMessage() {
        // Given
        var ctx = TestSimulationContext.create();
        var streams = ctx.documentStreams();
        var step = new StatusEmissionStep(PermissionProcessStatus.CREATED);

        // When
        step.execute(ctx);

        // Then
        StepVerifier.create(streams.getConnectionStatusMessageStream())
                    .then(streams::close)
                    .assertNext(csm -> assertAll(
                                        () -> assertEquals(ctx.permissionId(), csm.permissionId()),
                                        () -> assertEquals(ctx.connectionId(), csm.connectionId()),
                                        () -> assertEquals(ctx.dataNeedId(), csm.dataNeedId()),
                                        () -> assertEquals(PermissionProcessStatus.CREATED, csm.status())
                                )
                    )
                    .verifyComplete();
    }

    @Test
    void testEquals_forSameStep_returnsTrue() {
        // Given
        var step1 = new StatusEmissionStep(PermissionProcessStatus.CREATED);
        var step2 = new StatusEmissionStep(PermissionProcessStatus.CREATED);

        // When
        var res = step1.equals(step2);

        // Then
        assertTrue(res);
    }

    @Test
    void testHashcode_forSameStep_returnsEqualHashcode() {
        // Given
        var step1 = new StatusEmissionStep(PermissionProcessStatus.CREATED);
        var step2 = new StatusEmissionStep(PermissionProcessStatus.CREATED);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertEquals(res1, res2);
    }

    @Test
    void testEquals_forDifferentSteps_returnsFalse() {
        // Given
        var step1 = new StatusEmissionStep(PermissionProcessStatus.CREATED);
        var step2 = new StatusEmissionStep(PermissionProcessStatus.VALIDATED);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }

    @Test
    void testHashcode_forDifferentSteps_returnsUnequalHashcode() {
        // Given
        var step1 = new StatusEmissionStep(PermissionProcessStatus.CREATED);
        var step2 = new StatusEmissionStep(PermissionProcessStatus.VALIDATED);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertNotEquals(res1, res2);
    }

    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsIncompatibleType"})
    @Test
    void testEquals_forDifferentTypesOfSteps_returnsFalse() {
        // Given
        var step1 = new StatusEmissionStep(PermissionProcessStatus.CREATED);
        var step2 = new DelayStep(10, ChronoUnit.SECONDS);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }
}