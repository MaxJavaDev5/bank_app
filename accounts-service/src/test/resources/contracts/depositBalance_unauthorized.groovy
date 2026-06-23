package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса — нет авторизации"
    request {
        method 'PUT'
        url '/accounts/user/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': absent())
        }
        body([
            amount: 100.00,
            operationType: 'DEPOSIT'
        ])
    }
    response {
        status 401
    }
}
