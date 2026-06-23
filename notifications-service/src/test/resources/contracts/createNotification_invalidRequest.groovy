package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Создание уведомления — невалидный запрос"
    request {
        method 'POST'
        url '/notifications'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            eventId: 1,
            login: '',
            message: 'Ваш счёт пополнен на 100.00 рублей',
            type: 'DEPOSIT'
        ])
    }
    response {
        status 400
        headers {
            contentType(applicationJson())
        }
        body([
            error: $(regex("login:.*"))
        ])
    }
}
