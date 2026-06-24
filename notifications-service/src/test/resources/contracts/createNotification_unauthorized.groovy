package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Создание уведомления — нет авторизации"
    request {
        method 'POST'
        url '/notifications'
        headers {
            contentType(applicationJson())
            header('Authorization': absent())
        }
        body([
            eventId: 1,
            login: 'user',
            message: 'Ваш счёт пополнен на 100.00 рублей',
            type: 'DEPOSIT'
        ])
    }
    response {
        status 401
    }
}
