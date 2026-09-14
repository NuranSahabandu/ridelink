package com.ridelink.account.config;

import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Bean
    ApplicationRunner seedAdmin(UserRepository users,
                                PasswordEncoder encoder,
                                @Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            String normalised = email.trim().toLowerCase();
            if (users.existsByEmail(normalised)) {
                return;
            }
            users.save(new User("System Administrator", normalised,
                    encoder.encode(password), null, Role.ADMIN));
            log.info("Seeded default admin account: {}", normalised);
        };
    }
}