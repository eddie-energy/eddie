// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.cim;

import java.util.List;

public record Account(String cdsCustomerNumber, String accountName, String accountType, List<ServiceContract> serviceContracts) {
    public List<Meter> meters() {
        return serviceContracts.stream()
                               .flatMap(serviceContract -> serviceContract.servicePoints().stream())
                               .flatMap(servicePoint -> servicePoint.meterDevices().stream())
                               .toList();
    }
}
