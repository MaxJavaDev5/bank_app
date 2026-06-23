package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса аккаунта"
    request {
        method 'PUT'
        url '/accounts/user/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            amount: 100.00,
            operationType: 'DEPOSIT'
        ])
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(anyPositiveInt()),
            login: 'user',
            firstName: 'Иван',
            balance: 1100.00
        ])
    }
}
