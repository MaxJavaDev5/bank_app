package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Уведомление об исходящем переводе"
    request {
        method 'POST'
        url '/notifications'
        headers {
            contentType(applicationJson())
            header('Authorization': 'Bearer service-token')
        }
        body([
            eventId: 2,
            login: 'user',
            message: 'Вы перевели 100.00 рублей пользователю user2',
            type: 'TRANSFER_OUT'
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
            message: 'Вы перевели 100.00 рублей пользователю user2',
            type: 'TRANSFER_OUT',
            eventId: 2
        ])
    }
}
