// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;


/**
 * This class contains the message code for the various messages we receive / send to EDA.
 */
public class MessageCodes {
    public static final String CONSUMPTION_RECORD = "DATEN_CRMSG";
    public static final String MASTER_DATA = "ANTWORT_GN";

    private MessageCodes() {
    }

    public static class Revoke {
        public static final String CUSTOMER = "AUFHEBUNG_CCMC";
        public static final String IMPLICIT = "AUFHEBUNG_CCMI";
        public static final String VERSION = "01.00";

        private Revoke() {
        }

        public static class EligibleParty {
            public static final String REVOKE = "AUFHEBUNG_CCMS";
            public static final String ANSWER = "ANTWORT_CCMS";
            public static final String DENIAL = "ABLEHNUNG_CCMS";
            public static final String SCHEMA = "CM_REV_SP_01.10";
            public static final String VERSION = "01.10";

            private EligibleParty() {
            }
        }
    }

    public static class Notification {
        public static final String ANSWER = "ANTWORT_CCMO";
        public static final String ACCEPT = "ZUSTIMMUNG_CCMO";
        public static final String REJECT = "ABLEHNUNG_CCMO";

        private Notification() {
        }
    }

    public static class CPNotification {
        public static final String ANSWER = "ANTWORT_PT";
        public static final String REJECT = "ABLEHNUNG_PT";

        private CPNotification() {
        }
    }

    public static class Request {
        public static final String CODE = "ANFORDERUNG_CCMO";
        public static final String SCHEMA = "CM_REQ_ONL_01.30";
        public static final String VERSION = "01.30";

        private Request() {
        }
    }

    public static class CPRequest {
        public static final String CODE = "ANFORDERUNG_PT";

        private CPRequest() {
        }
    }
}
