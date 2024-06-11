package energy.eddie.examples.newexampleapp;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewExampleApp {
    public static void main(String[] args) {
        Flyway flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(Env.JDBC_URL.get(), Env.JDBC_USER.get(), Env.JDBC_PASSWORD.get())
                .locations("classpath:db.migration")
                .load();
        flyway.migrate();

        SpringApplication.run(NewExampleApp.class, args);
    }
}