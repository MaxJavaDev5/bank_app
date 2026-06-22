package ru.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs",
        "spring.cloud.gateway.routes[0].id=accountsRoute",
        "spring.cloud.gateway.routes[0].uri=lb://accounts-service",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/accounts/**",
        "spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[1].id=cashRoute",
        "spring.cloud.gateway.routes[1].uri=lb://cash-service",
        "spring.cloud.gateway.routes[1].predicates[0]=Path=/api/cash/**",
        "spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[2].id=transferRoute",
        "spring.cloud.gateway.routes[2].uri=lb://transfer-service",
        "spring.cloud.gateway.routes[2].predicates[0]=Path=/api/transfer/**",
        "spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[3].id=notificationsRoute",
        "spring.cloud.gateway.routes[3].uri=lb://notifications-service",
        "spring.cloud.gateway.routes[3].predicates[0]=Path=/api/notifications/**",
        "spring.cloud.gateway.routes[3].filters[0]=StripPrefix=1"
})
class GatewayRoutesTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void shouldDefineAllGatewayRoutes() {
        Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
        List<String> routeIds = routes.map(RouteDefinition::getId).collectList().block();

        assertThat(routeIds).containsExactlyInAnyOrder(
                "accountsRoute",
                "cashRoute",
                "transferRoute",
                "notificationsRoute"
        );
    }
}
