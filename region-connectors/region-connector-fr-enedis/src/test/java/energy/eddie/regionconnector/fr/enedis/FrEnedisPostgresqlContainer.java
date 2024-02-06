package energy.eddie.regionconnector.fr.enedis;

import org.testcontainers.containers.PostgreSQLContainer;

public class FrEnedisPostgresqlContainer extends PostgreSQLContainer<FrEnedisPostgresqlContainer> {
    private static final String IMAGE_VERSION = "postgres:latest";
    private static FrEnedisPostgresqlContainer container;

    private FrEnedisPostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    public static FrEnedisPostgresqlContainer getInstance() {
        if (container == null) {
            container = new FrEnedisPostgresqlContainer();
        }

        return container;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        // JVM handles shutdown
    }
}
