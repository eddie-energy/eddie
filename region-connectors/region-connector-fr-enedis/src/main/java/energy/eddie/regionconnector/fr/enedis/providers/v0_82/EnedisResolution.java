package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;

enum EnedisResolution {
    PT10M(Granularity.PT10M),
    PT15M(Granularity.PT15M),
    PT30M(Granularity.PT30M),
    PT60M(Granularity.PT1H),
    P1D(Granularity.P1D);

    private final Granularity granularity;

    EnedisResolution(Granularity iso8601) {
        this.granularity = iso8601;
    }

    public Granularity granularity() {
        return granularity;
    }
}
