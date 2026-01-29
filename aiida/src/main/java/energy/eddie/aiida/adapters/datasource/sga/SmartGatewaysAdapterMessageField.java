// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public record SmartGatewaysAdapterMessageField(String rawTag, String value, UnitOfMeasurement unitOfMeasurement, ObisCode obisCode) {}
