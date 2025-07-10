package energy.eddie.exampleappbackend;

import energy.eddie.exampleappbackend.config.ExampleAppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("energy.eddie")
@EnableConfigurationProperties(ExampleAppConfig.class)
public class ExampleAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExampleAppBackendApplication.class, args);
	}

}
