// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

public record InvoiceRecipientImpl(
        ContractPartner contractPartner,
        Address address
) implements InvoiceRecipient {
}
