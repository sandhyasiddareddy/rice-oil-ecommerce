package com.riceoil.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "app.catalog.sample-data.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:platform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none"
})
class PlatformBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
