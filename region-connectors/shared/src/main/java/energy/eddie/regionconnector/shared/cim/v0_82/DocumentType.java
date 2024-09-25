package energy.eddie.regionconnector.shared.cim.v0_82;

public enum DocumentType {
    PERMISSION_MARKET_DOCUMENT("permission-market-document"),
    ACCOUNTING_POINT_MARKET_DOCUMENT("accounting-point-market-document"),
    VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT("validated-historical-data-market-document");


    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
