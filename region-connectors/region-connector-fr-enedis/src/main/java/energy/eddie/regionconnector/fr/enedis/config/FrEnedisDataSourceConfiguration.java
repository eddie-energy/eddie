package energy.eddie.regionconnector.fr.enedis.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "frEnedisEntityManagerFactory",
        transactionManagerRef = "frEnedisTransactionManager",
        basePackages = {
                "energy.eddie.regionconnector.fr.enedis.permission.request.repositories"
        }
)
public class FrEnedisDataSourceConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "region-connector.fr.enedis.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "frEnedisDataSource")
    public DataSource dataSource() {
        return dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "frEnedisEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean frEnedisEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                               @Qualifier("frEnedisDataSource") DataSource dataSource) {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        return builder.dataSource(dataSource)
                .properties(properties)
                .packages("energy.eddie.regionconnector.fr.enedis.permission.request.models")
                .persistenceUnit("FutureDataPermission")
                .build();
    }

    @Bean(name = "frEnedisTransactionManager")
    public PlatformTransactionManager frEnedisTransactionManager(@Qualifier("frEnedisEntityManagerFactory") EntityManagerFactory frEnedisEntityManagerFactory) {
        return new JpaTransactionManager(frEnedisEntityManagerFactory);
    }
}
