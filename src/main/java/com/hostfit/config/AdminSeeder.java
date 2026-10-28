package com.hostfit.config;

import com.hostfit.domain.AdminUser;
import com.hostfit.repo.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the administrator account on first start from {@code hostfit.admin.*}. */
@Component
@Order(1)
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminUserRepository repository;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminSeeder(AdminUserRepository repository, PasswordEncoder encoder, AppProperties props) {
        this.repository = repository;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = props.admin().username();
        if (repository.findByUsername(username).isPresent()) {
            return;
        }
        repository.save(new AdminUser(username, encoder.encode(props.admin().password())));
        if ("admin123".equals(props.admin().password())) {
            log.warn("Administrator '{}' created with the DEFAULT password. Set HOSTFIT_ADMIN_PASSWORD before deploying.", username);
        } else {
            log.info("Administrator '{}' created.", username);
        }
    }
}
