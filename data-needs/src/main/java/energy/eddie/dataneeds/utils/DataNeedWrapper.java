// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils;

import energy.eddie.dataneeds.needs.TimeframedDataNeed;

import java.time.LocalDate;

public record DataNeedWrapper(
        TimeframedDataNeed timeframedDataNeed,
        LocalDate calculatedStart,
        LocalDate calculatedEnd) {
}
