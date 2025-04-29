package ec.edu.ups.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "ec.edu.ups.Backend.model")
@EnableJpaRepositories(basePackages = "ec.edu.ups.Backend.repository")
public class BackendTesisApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendTesisApplication.class, args);
	}
}
