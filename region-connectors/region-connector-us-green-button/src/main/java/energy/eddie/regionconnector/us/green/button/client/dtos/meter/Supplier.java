// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Supplier {
    @JsonProperty(value = "supplier_name", required = true)
    private final String supplierName;
    @JsonProperty(value = "supplier_service_id")
    private final String supplierServiceId;
    @JsonProperty(value = "supplier_tariff")
    private final String supplierTariff;
    @JsonProperty(value = "third_party")
    private final String thirdParty;

    @JsonCreator
    public Supplier(String supplierName, String supplierServiceId, String supplierTariff, String thirdParty) {
        this.supplierName = supplierName;
        this.supplierServiceId = supplierServiceId;
        this.supplierTariff = supplierTariff;
        this.thirdParty = thirdParty;
    }
}
