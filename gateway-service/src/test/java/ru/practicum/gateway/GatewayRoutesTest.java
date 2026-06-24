package ru.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:http://localhost:8888",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs",
        "spring.cloud.gateway.routes[0].id=accountsRoute",
        "spring.cloud.gateway.routes[0].uri=lb://accounts-service",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/accounts/**",
        "spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[0].filters[1].name=JwtTokenRelay",
        "spring.cloud.gateway.routes[1].id=cashRoute",
        "spring.cloud.gateway.routes[1].uri=lb://cash-service",
        "spring.cloud.gateway.routes[1].predicates[0]=Path=/api/cash/**",
        "spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[1].filters[1].name=JwtTokenRelay",
        "spring.cloud.gateway.routes[2].id=transferRoute",
        "spring.cloud.gateway.routes[2].uri=lb://transfer-service",
        "spring.cloud.gateway.routes[2].predicates[0]=Path=/api/transfer/**",
        "spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[2].filters[1].name=JwtTokenRelay",
        "spring.cloud.gateway.routes[3].id=notificationsRoute",
        "spring.cloud.gateway.routes[3].uri=lb://notifications-service",
        "spring.cloud.gateway.routes[3].predicates[0]=Path=/api/notifications/**",
        "spring.cloud.gateway.routes[3].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.routes[3].filters[1].name=JwtTokenRelay"
})
class GatewayRoutesTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void shouldDefineAllGatewayRoutes() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull();
        assertThat(routes).extracting(RouteDefinition::getId)
                .containsExactlyInAnyOrder(
                        "accountsRoute",
                        "cashRoute",
                        "transferRoute",
                        "notificationsRoute"
                );

        routes.forEach(this::assertRouteHasRequiredFilters);
    }

    private void assertRouteHasRequiredFilters(RouteDefinition route) {
        List<FilterDefinition> filters = route.getFilters();
        List<String> names = filters.stream().map(FilterDefinition::getName).toList();

        assertThat(names).contains("StripPrefix", "JwtTokenRelay");

        FilterDefinition stripPrefix = filters.stream()
                .filter(f -> "StripPrefix".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(stripPrefix.getArgs().values()).contains("1");
    }
}
