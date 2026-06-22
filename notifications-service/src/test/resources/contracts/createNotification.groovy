package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Создание уведомления об операции"
    request {
        method 'POST'
        url '/notifications'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer test-token')
        }
        body([
            login: 'user',
            message: 'Ваш счёт пополнен на 100.00 рублей',
            type: 'DEPOSIT'
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(anyPositiveInt()),
            login: 'user',
            message: 'Ваш счёт пополнен на 100.00 рублей',
            type: 'DEPOSIT'
        ])
    }
}
