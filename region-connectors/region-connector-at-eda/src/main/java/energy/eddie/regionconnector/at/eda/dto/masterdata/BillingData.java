// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

public interface BillingData {
    @Nullable
    String referenceNumber();

    @Nullable
    String gridInvoiceRecipient();

    @Nullable
    String budgetBillingCycle();

    short meterReadingMonth();

    @Nullable
    String consumptionBillingCycle();

    short consumptionBillingMonth();

    @Nullable
    String yearMonthOfNextBill();
}
