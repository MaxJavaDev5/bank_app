package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод — недостаточно средств"
    request {
        method 'POST'
        url '/accounts/transfer'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            fromLogin: 'poor-user',
            toLogin: 'user2',
            amount: 300.00
        ])
    }
    response {
        status 409
        headers {
            contentType(applicationJson())
        }
        body([
            error: $(regex(".*недостаточно средств.*"))
        ])
    }
}
