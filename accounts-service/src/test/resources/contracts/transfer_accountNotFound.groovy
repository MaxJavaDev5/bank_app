package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод — получатель не найден"
    request {
        method 'POST'
        url '/accounts/transfer'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            fromLogin: 'user',
            toLogin: 'unknown',
            amount: 100.00
        ])
    }
    response {
        status 404
        headers {
            contentType(applicationJson())
        }
        body([
            error: "Аккаунт с логином 'unknown' не найден"
        ])
    }
}
