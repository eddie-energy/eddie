package energy.eddie.regionconnector.fr.enedis.api;

import org.springframework.boot.health.contributor.Health;

import java.util.Map;

public interface EnedisHealth {
    Map<String, Health> health();
}
