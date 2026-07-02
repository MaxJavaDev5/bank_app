package ru.practicum.front.client;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import ru.practicum.front.model.RemoteException;

public final class WebClientErrorHandler {

    private WebClientErrorHandler() {
    }

    public static ExchangeFilterFunction remoteErrorFilter(String serviceName) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RemoteException(serviceName, body)));
            }
            return Mono.just(response);
        });
    }
}
