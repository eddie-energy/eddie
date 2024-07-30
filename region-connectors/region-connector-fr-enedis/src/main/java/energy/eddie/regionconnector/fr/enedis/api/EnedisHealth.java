package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.v0.HealthState;

import java.util.Map;

public interface EnedisHealth {
    Map<String, HealthState> health();
}
