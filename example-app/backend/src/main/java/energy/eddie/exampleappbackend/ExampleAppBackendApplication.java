package energy.eddie.exampleappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("energy.eddie")
public class ExampleAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExampleAppBackendApplication.class, args);
	}

}
