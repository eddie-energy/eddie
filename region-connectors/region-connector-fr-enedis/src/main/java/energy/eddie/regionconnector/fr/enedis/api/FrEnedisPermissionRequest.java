// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

public interface FrEnedisPermissionRequest extends MeterReadingPermissionRequest {
    String usagePointId();
    Granularity granularity();

    UsagePointType usagePointType();
}
