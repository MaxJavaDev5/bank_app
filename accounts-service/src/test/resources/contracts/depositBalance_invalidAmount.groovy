package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса — невалидная сумма"
    request {
        method 'PUT'
        url '/accounts/user/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            amount: 0,
            operationType: 'DEPOSIT'
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
