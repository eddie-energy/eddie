// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record MeasurementSlice(ZonedDateTime start, ZonedDateTime end, @Nullable List<Measurement> measurements) {}
