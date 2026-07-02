package ru.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=logback-test",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs"
})
class GatewayRoutesTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void shouldDefineAllGatewayRoutes() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                    .collectList()
                    .block(Duration.ofSeconds(2));

            assertThat(routes).isNotNull();
            assertThat(routes).extracting(RouteDefinition::getId)
                    .containsExactlyInAnyOrder(
                            "accountsRoute",
                            "cashRoute",
                            "transferRoute",
                            "notificationsRoute"
                    );
        });

        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block(Duration.ofSeconds(2));
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
