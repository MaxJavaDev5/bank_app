package ru.practicum.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// пробрасываем jwt токен в микросервисы за gateway
public class JwtTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public JwtTokenRelayGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) ->
                extractToken(exchange)
                        .switchIfEmpty(Mono.error(
                                new IllegalStateException(
                                        "JWT token not found in SecurityContext or Authorization header")))
                        .flatMap(token -> chain.filter(addToken(exchange, token)));
    }

    private Mono<String> extractToken(ServerWebExchange exchange) {
        Mono<String> fromContext = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken().getTokenValue());

        Mono<String> fromHeader = Mono
                .justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));

        return fromContext.switchIfEmpty(fromHeader);
    }

    private ServerWebExchange addToken(ServerWebExchange exchange, String token) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("Authorization", "Bearer " + token)
                        .build())
                .build();
    }
}
