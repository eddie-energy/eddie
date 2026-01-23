// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.cim;

import java.util.List;

public record ServiceContract(String contractAddress, String serviceType, List<ServicePoint> servicePoints) {
}
