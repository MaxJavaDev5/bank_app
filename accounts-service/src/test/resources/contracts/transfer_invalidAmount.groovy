package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод — невалидная сумма"
    request {
        method 'POST'
        url '/accounts/transfer'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            fromLogin: 'user',
            toLogin: 'user2',
            amount: -10
        ])
    }
    response {
        status 400
        headers {
            contentType(applicationJson())
        }
        body([
            error: $(regex("amount:.*"))
        ])
    }
}
