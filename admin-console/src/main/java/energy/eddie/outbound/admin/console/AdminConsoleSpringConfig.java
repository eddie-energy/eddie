package energy.eddie.outbound.admin.console;

import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@EnableWebMvc
@SpringBootApplication
@OutboundConnector(name = "admin-console")
public class AdminConsoleSpringConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vue assets
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/public/assets/");
        // Vue app entry point
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/public/index.html");
        // Vue favicon
        registry.addResourceHandler("/favicon.svg")
                .addResourceLocations("classpath:/public/favicon.svg");
        // Static resources separate from the Vue app
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        // Register resource handlers before view controllers
        registry.setOrder(-1);
    }
}