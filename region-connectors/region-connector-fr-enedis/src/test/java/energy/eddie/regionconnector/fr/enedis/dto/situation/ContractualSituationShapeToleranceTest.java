// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The exact JSON shape of {@code situation_contrat_auto} is not published (the Swagger leaves
 * {@code contracts} unexpanded). These tests pin that the DTO tolerates both documented candidate
 * shapes, because a hard deserialization failure would abort the whole master-data fetch and leave
 * the permission request without any terminal status.
 */
class ContractualSituationShapeToleranceTest {

    private static final String PRM = "24115050XXXXXX";

    @Test
    void segments_asArrayOfStrings() throws IOException {
        var situation = TestResourceProvider.readFromFile("situation-segment-strings.json", ContractualSituation.class);

        assertEquals(PRM, situation.usagePointId());
        assertEquals(List.of("C5"), situation.segments());
    }

    /**
     * Enedis' sibling {@code synth_contrat_auto} documents {@code segments} as an array of objects
     * ({@code [{"segment": "C3"}]}); the ITC guide renders the same concept as {@code segment: ex. C5, P4}.
     */
    @Test
    void segments_asArrayOfSegmentObjects() throws IOException {
        var situation = TestResourceProvider.readFromFile("situation-segment-objects.json", ContractualSituation.class);

        assertEquals(List.of("C5"), situation.segments());
    }

    @Test
    void segments_asSingleString() throws IOException {
        var situation = TestResourceProvider.readFromFile("situation-segment-single-string.json",
                                                          ContractualSituation.class);

        assertEquals(List.of("P4"), situation.segments());
    }

    /**
     * The ITC Swagger defines a {@code power} object ({@code {unit, value}}) rather than a bare scalar.
     */
    @Test
    void subscribedPower_asPlainScalar() throws IOException {
        var situation = TestResourceProvider.readFromFile("situation-subscribed-power-string.json",
                                                          ContractualSituation.class);

        assertEquals("6", situation.subscribedPower());
    }

    @Test
    void subscribedPower_asPowerObject_exposesValue() throws IOException {
        var situation = TestResourceProvider.readFromFile("situation-subscribed-power-object.json",
                                                          ContractualSituation.class);

        assertEquals("6", situation.subscribedPower());
    }
}