package energy.eddie.aiida.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity()
public class UserSecurityConfig {
    @Bean
    public InMemoryUserDetailsManager userDetailsService(@Value("${app.login.password}") String password) {
        UserDetails user = User.withUsername("aiida-admin")
                               .password(passwordEncoder().encode(password))
                               .roles("ADMIN")
                               .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
        return http.cors(withDefaults())
                   .csrf(withDefaults())
                   .authorizeHttpRequests(authorize -> authorize
                           .requestMatchers("/img/eddie.svg",
                                            "/img/icon.svg",
                                            "/css/style.css",
                                            "/css/login.css",
                                            "/js/main.js").permitAll()
                           .anyRequest().authenticated()
                   )
                   .formLogin(form -> form
                           .loginPage("/login")
                           .defaultSuccessUrl("/")
                           .permitAll()
                   )
                   .logout(logout -> logout
                           .logoutUrl("/logout")
                           .logoutSuccessUrl("/login")
                           .deleteCookies("JSESSIONID", "connectionId")
                   )
                   .build();
    }
}
