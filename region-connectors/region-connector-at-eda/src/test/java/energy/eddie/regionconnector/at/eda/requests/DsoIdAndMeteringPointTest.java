// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsoIdAndMeteringPointTest {
    @Test
    void dsoIdValidAndMeteringPointNull_returnsDsoId() {
        // given
        String dsoId = "123456789";
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(dsoId, null);

        // when
        String result = dsoIdAndMeteringPoint.dsoId();

        // then
        assertEquals(dsoId, result);
    }

    @Test
    void ctr_dsoIdNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DsoIdAndMeteringPoint(null, "meteringPoint"));
    }

    @Test
    void meteringPoint_returnsOptionalWithValue() {
        // given
        String meteringPoint = "12345678901234567890";
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("dsoId", meteringPoint);

        // when
        Optional<String> result = dsoIdAndMeteringPoint.meteringPoint();

        // then
        assertEquals(meteringPoint, result.orElse(null));
    }

    @Test
    void meteringPointAndBlankDsoId_returnsOptionalWithValue() {
        // given
        String meteringPoint = "12345678901234567890";
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("", meteringPoint);

        // when
        Optional<String> result = dsoIdAndMeteringPoint.meteringPoint();

        // then
        assertEquals(meteringPoint, result.orElse(null));
    }

    @Test
    void nullMeteringPoint_returnsEmptyOptional() {
        // given
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("123456789", null);

        // when
        Optional<String> result = dsoIdAndMeteringPoint.meteringPoint();

        // then
        assertEquals(Optional.empty(), result);
    }
}