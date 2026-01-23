// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import energy.eddie.regionconnector.at.eda.dto.masterdata.BillingData;
import jakarta.annotation.Nullable;

public record BillingData01p33(
        at.ebutilities.schemata.customerprocesses.masterdata._01p33.BillingData billingData) implements BillingData {
    @Override
    public String referenceNumber() {
        return billingData.getReferenceNumber();
    }

    @Override
    @Nullable
    public String gridInvoiceRecipient() {
        var gridInvoiceRecipient = billingData.getGridInvoiceRecipient();
        return gridInvoiceRecipient == null ? null : gridInvoiceRecipient.getValue().value();
    }

    @Override
    @Nullable
    public String budgetBillingCycle() {
        var budgetBillingCycle = billingData.getBudgetBillingCycle();
        return budgetBillingCycle == null ? null : budgetBillingCycle.getValue();
    }

    @Override
    public short meterReadingMonth() {
        var meterReadingMonth = billingData.getMeterReadingMonth();
        return meterReadingMonth == null ? 0 : meterReadingMonth.getValue();
    }

    @Override
    @Nullable
    public String consumptionBillingCycle() {
        var consumptionBillingCycle = billingData.getConsumptionBillingCycle();
        return consumptionBillingCycle == null ? null : consumptionBillingCycle.getValue();
    }

    @Override
    public short consumptionBillingMonth() {
        var consumptionBillingMonth = billingData.getConsumptionBillingMonth();
        return consumptionBillingMonth == null ? 0 : consumptionBillingMonth.getValue();
    }

    @Override
    @Nullable
    public String yearMonthOfNextBill() {
        return billingData.getYearMonthOfNextBill();
    }
}
