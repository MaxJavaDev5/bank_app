package ru.practicum.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs",
        "spring.security.oauth2.client.registration.transfer-client.client-id=transfer-client",
        "spring.security.oauth2.client.registration.transfer-client.client-secret=transfer-secret",
        "spring.security.oauth2.client.registration.transfer-client.authorization-grant-type=client_credentials",
        "spring.security.oauth2.client.registration.transfer-client.scope=accounts,notifications",
        "spring.security.oauth2.client.provider.transfer-client.authorization-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/auth",
        "spring.security.oauth2.client.provider.transfer-client.token-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/token",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class TransferServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
