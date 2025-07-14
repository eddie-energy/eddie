package energy.eddie.exampleappbackend;

import energy.eddie.exampleappbackend.config.ExampleAppEddieConfig;
import energy.eddie.exampleappbackend.config.ExampleAppKafkaConfig;
import energy.eddie.exampleappbackend.config.ExampleAppMqttConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients("energy.eddie")
@EnableConfigurationProperties({ExampleAppEddieConfig.class, ExampleAppKafkaConfig.class, ExampleAppMqttConfig.class})
public class ExampleAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExampleAppBackendApplication.class, args);
	}

}
