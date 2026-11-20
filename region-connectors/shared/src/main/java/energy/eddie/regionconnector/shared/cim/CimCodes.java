// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim;

/**
 * Version-neutral ENTSO-E code values that are shared by the CIM versions.
 * Region connectors use them to interpret their source data once and map the result to each CIM version,
 * without coupling one CIM version to the generated classes of another.
 */
public final class CimCodes {
    private CimCodes() {
        // Constants only
    }

    public static final class Direction {
        public static final String UP = "A01";
        public static final String DOWN = "A02";
        public static final String UP_AND_DOWN = "A03";

        private Direction() {}
    }

    public static final class Quality {
        public static final String ADJUSTED = "A01";
        public static final String NOT_AVAILABLE = "A02";
        public static final String ESTIMATED = "A03";
        public static final String AS_PROVIDED = "A04";

        private Quality() {}
    }

    public static final class BusinessType {
        public static final String PRODUCTION = "A01";
        public static final String CONSUMPTION = "A04";
        public static final String NET_PRODUCTION_CONSUMPTION = "A07";

        private BusinessType() {}
    }

    public static final class EnergyProduct {
        public static final String ACTIVE_POWER = "8716867000016";
        public static final String ACTIVE_ENERGY = "8716867000030";
        public static final String REACTIVE_ENERGY = "8716867000047";
        public static final String CAPACITIVE_REACTIVE_ENERGY = "8716867000139";
        public static final String INDUCTIVE_REACTIVE_ENERGY = "8716867000146";

        private EnergyProduct() {}
    }

    public static final class UnitOfMeasure {
        public static final String AMPERE = "AMP";
        public static final String CELSIUS = "CEL";
        public static final String CUBIC_METRE = "MTQ";
        public static final String CUBIC_METRES_PER_SECOND = "MQS";
        public static final String DEGREE_UNIT_OF_ANGLE = "DD";
        public static final String HECTOPASCAL = "A97";
        public static final String KELVIN = "KEL";
        public static final String KILOVOLT = "KVT";
        public static final String KILOVOLT_AMPERE_REACTIVE = "KVR";
        public static final String KILOWATT = "KWT";
        public static final String KILOWATT_HOUR = "KWH";
        public static final String MEGAVOLT_AMPERE = "MVA";
        public static final String MEGAVOLT_AMPERE_REACTIVE_HOURS = "MAH";
        public static final String MEGAWATT_HOURS = "MWH";
        public static final String METRE = "MTR";
        public static final String ONE = "C62";
        public static final String WATT = "WTT";

        private UnitOfMeasure() {}
    }

    public static final class ReasonCode {
        public static final String ERRORS_NOT_SPECIFICALLY_IDENTIFIED = "999";

        private ReasonCode() {}
    }

    /**
     * Values of the CIM {@code CommodityKind}.
     */
    public static final class Commodity {
        public static final String NONE = "0";
        public static final String ELECTRICITY_SECONDARY_METERED = "1";
        public static final String ELECTRICITY_PRIMARY_METERED = "2";
        public static final String NATURAL_GAS = "7";
        public static final String POTABLE_WATER = "9";

        private Commodity() {}
    }

    /**
     * Values of the CIM {@code AggregateKind}.
     */
    public static final class Aggregate {
        public static final String SUM = "26";

        private Aggregate() {}
    }
}
