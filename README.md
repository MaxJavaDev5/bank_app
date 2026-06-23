# My Bank App

Микросервисное банковское приложение. Спринт 9, Яндекс Практикум.

## Стек

Java 21, Maven, Spring Boot 3.4.4, Spring Cloud 2024.0.1, Eureka, Config Server, Gateway, Keycloak, PostgreSQL, JPA, Resilience4j, Spring Cloud Contract.

## Модули

| Модуль | Порт | Назначение |
|--------|------|------------|
| discovery-service | 8761 | Eureka |
| config-service | 8888 | Config Server |
| gateway-service | 8080 | API Gateway |
| accounts-service | 8081 | Аккаунты, баланс, outbox |
| cash-service | 8082 | Пополнение и снятие |
| transfer-service | 8083 | Переводы |
| notifications-service | 8084 | Уведомления |
| front-app | 8085 | UI |

## Запуск

Для зупуска локально:

```bash
mvn clean package -DskipTests
docker-compose up --build
```

UI: http://localhost:8085

Пользователи: `user` / `user2` / `user3`, пароль `password`.

Keycloak admin: `admin` / `admin`, http://localhost:8180

## Сборка и тесты

Нужен **JDK 21**.

```bash
java -version
mvn -version
mvn clean verify
```

## Схема

```
front-app → gateway → accounts / cash / transfer / notifications
postgres, keycloak, discovery, config
```
