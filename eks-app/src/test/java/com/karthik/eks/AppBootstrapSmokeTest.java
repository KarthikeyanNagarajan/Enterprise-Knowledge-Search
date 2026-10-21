package com.karthik.eks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppBootstrapSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void applicationStartsAndRequiredInfrastructureBeansExist() {
        assertThat(applicationContext).isNotNull();
        assertThat(dataSource).isNotNull();
        assertThat(applicationContext.containsBean("flyway")).isTrue();
        assertThat(applicationContext.containsBean("entityManagerFactory")).isTrue();
    }
}
