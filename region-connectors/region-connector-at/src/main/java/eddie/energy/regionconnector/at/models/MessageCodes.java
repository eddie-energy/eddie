package eddie.energy.regionconnector.at.models;


/**
 * This class contains the message code for the various messages we receive / send to EDA.
 */
public class MessageCodes {

    public static class Revoke {
        public static final String CUSTOMER = "AUFHEBUNG_CCMC";
        public static final String IMPLICIT = "AUFHEBUNG_CCMI";

    }

    public static class Notification {
        public static final String ANSWER = "ANTWORT_CCMO";
        public static final String ACCEPT = "ZUSTIMMUNG_CCMO";
        public static final String REJECT = "ABLEHNUNG_CCMO";
    }

    public static final String CONSUMPTION_RECORD = "DATEN_CRMSG";

    public static final String MASTER_DATA = "ANTWORT_GN";

    public static class Request {
        public static final String CODE = "ANFORDERUNG_CCMO";
        public static final String SCHEMA = "CM_REQ_ONL_01.10";
        public static final String VERSION = "01.10";
    }

}
