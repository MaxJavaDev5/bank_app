package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Пополнение баланса — недостаточно прав"
    request {
        method 'PUT'
        url '/accounts/user/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer user-token')
        }
        body([
            amount: 100.00,
            operationType: 'DEPOSIT'
        ])
    }
    response {
        status 403
    }
}
