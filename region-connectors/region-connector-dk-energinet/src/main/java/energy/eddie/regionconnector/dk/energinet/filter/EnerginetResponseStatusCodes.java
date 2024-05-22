package energy.eddie.regionconnector.dk.energinet.filter;

/**
 * Class containing the possible response status codes from the Energinet API. Taken from the Energinet API
 * documentation <a href="https://api.eloverblik.dk/customerapi/index.html">customerApi</a>
 */
public class EnerginetResponseStatusCodes {

    public static final int NO_ERROR = 10000;
    public static final int WRONG_NUMBER_OF_ARGUMENTS = 10001;
    public static final int NO_CPR_CONSENT = 10007;
    public static final int WRONG_METERING_POINT_ID_OR_WEB_ACCESS_CODE = 20000;
    public static final int METERING_POINT_BLOCKED = 20001;
    public static final int METERING_POINT_RELATION_ALREADY_EXIST = 20002;
    public static final int METERING_POINT_ID_NOT_18_CHARS_LONG = 20003;
    public static final int METERING_POINT_ALIAS_TOO_LONG = 20005;
    public static final int WEB_ACCESS_CODE_NOT_8_CHARS_LONG = 20006;
    public static final int WEB_ACCESS_CODE_CONTAINS_ILLEGAL_CHARS = 20007;
    public static final int METERING_POINT_NOT_FOUND = 20008;
    public static final int METERING_POINT_IS_CHILD = 20009;
    public static final int RELATION_NOT_FOUND = 20010;
    public static final int UNKNOWN_ERROR = 20011;
    public static final int UNAUTHORIZED = 20012;
    public static final int NO_VALID_METERING_POINTS_IN_LIST = 20013;
    public static final int FROM_DATE_IS_GREATER_THAN_TODAY = 30000;
    public static final int FROM_DATE_IS_GREATER_THAN_TO_DATE = 30001;
    public static final int TO_DATE_CAN_NOT_BE_EQUAL_TO_FROM_DATE = 30002;
    public static final int TO_DATE_IS_GREATER_THAN_TODAY = 30003;
    public static final int INVALID_DATE_FORMAT = 30004;
    public static final int REQUESTED_AGGREGATION_UNAVAILABLE = 30008;
    public static final int INTERNAL_SERVER_ERROR = 30011;
    public static final int WRONG_TOKEN_TYPE = 50000;
    public static final int TOKEN_NOT_VALID = 50001;
    public static final int ERROR_CREATING_TOKEN = 50002;
    public static final int TOKEN_REGISTRATION_FAILED = 50003;
    public static final int TOKEN_ALREADY_ACTIVE = 50004;
    public static final int TOKEN_ALREADY_DEACTIVATED = 50005;
    public static final int THIRD_PARTY_NOT_FOUND = 60000;

    private EnerginetResponseStatusCodes() {
    }
}
