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
