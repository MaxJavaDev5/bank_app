package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод между аккаунтами"
    request {
        method 'POST'
        url '/accounts/transfer'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer test-token')
        }
        body([
            fromLogin: 'user',
            toLogin: 'user2',
            amount: 300.00
        ])
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            fromLogin: 'user',
            toLogin: 'user2',
            amount: 300.00,
            senderBalance: 700.00
        ])
    }
}
