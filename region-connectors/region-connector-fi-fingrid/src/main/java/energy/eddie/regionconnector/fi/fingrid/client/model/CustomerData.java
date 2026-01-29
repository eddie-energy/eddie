// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.fi.fingrid.client.deserializer.CustomerTransactionDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CustomerData(@JsonProperty(value = "Header", required = true) Header header,
                           @JsonProperty(value = "Transaction", required = true)
                           @JsonDeserialize(using = CustomerTransactionDeserializer.class)
                           CustomerTransaction transaction) {
}
