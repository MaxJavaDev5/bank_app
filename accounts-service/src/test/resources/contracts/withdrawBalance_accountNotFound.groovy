package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Снятие денег — аккаунт не найден"
    request {
        method 'PUT'
        url '/accounts/unknown/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            amount: 100.00,
            operationType: 'WITHDRAW'
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
