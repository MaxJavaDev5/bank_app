package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Снятие денег — недостаточно средств"
    request {
        method 'PUT'
        url '/accounts/poor-user/balance'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            amount: 500.00,
            operationType: 'WITHDRAW'
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
