# My Bank App

Учебный проект «Банк» 
Яндекс Практикум (спринты 9–11).

Стек: Java 21, Spring Boot, Gateway, Keycloak, PostgreSQL, Apache Kafka, Helm/Kubernetes.

## Сервисы

| Сервис | Порт |
|--------|------|
| gateway-service | 8080 |
| accounts-service | 8081 |
| cash-service | 8082 |
| transfer-service | 8083 |
| notifications-service | 8084 |
| front-app | 8085 |
| kafka | 9092 |

## Уведомления через Kafka

С 11-го спринта уведомления ходят только через Apache Kafka.
REST между сервисами и notifications больше нет.

- `accounts-service`, `cash-service` и `transfer-service` после успешной операции отправляют
  сообщение в топик `bank.notifications` (`KafkaTemplate`, ключ — login, значение — JSON):
  - accounts — `PROFILE_UPDATE` при изменении профиля;
  - cash — `DEPOSIT` / `WITHDRAW`;
  - transfer — `TRANSFER_OUT` / `TRANSFER_IN`.
- `notifications-service` — Kafka-консьюмер (`@KafkaListener`): читает топик, пишет
  уведомление в лог и сохраняет в таблицу `notifications.notifications`. Дубли отсекаются по
  `event_id` (идемпотентность).

Топик `bank.notifications` (3 партиции, 1 реплика) создаётся автоматически через
`@Bean NewTopic` в notifications. Посмотреть пришедшие уведомления можно в логах
`notifications-service` или в БД.


Для сборки/запуска нужен JDK 21.

```cmd
mvn clean package -DskipTests
docker compose up --build
```

`docker compose` поднимает в том числе брокер Kafka (сервис `kafka`, один узел в режиме
KRaft, порт 9092). Бэкенд-сервисы внутри сети compose ходят на него по адресу `kafka:9092`
(переменная `KAFKA_BOOTSTRAP_SERVERS`).

UI: http://localhost:8085

Пользователи `user`, `user2`, `user3` — пароль `password`.

Keycloak: http://localhost:8180, admin / admin.

## Kubernetes

backend сервисы в кластере, Keycloak и front — в docker compose.

Понадобятся Docker Desktop (с включённым Kubernetes), Helm 3, kubectl.

Сборка образов (из корня проекта):

```cmd
docker build -t local/accounts-service:latest ./accounts-service
docker build -t local/cash-service:latest ./cash-service
docker build -t local/transfer-service:latest ./transfer-service
docker build -t local/notifications-service:latest ./notifications-service
docker build -t local/gateway-service:latest ./gateway-service
```

Keycloak и front снаружи кластера:

```cmd
set GATEWAY_URL=http://host.docker.internal:30080
docker compose up -d keycloak front-app
```
Деплой backend:

```cmd
helm dependency update helm/bank-app
helm upgrade --install bank-app helm/bank-app --namespace bank --create-namespace
kubectl get pods -n bank
helm test bank-app -n bank
```

Gateway снаружи доступен на NodePort 30080. Front ходит на http://localhost:8085.

Чарты лежат в `helm/bank-app/` (зонтичный чарт и сабчарты на postgres, kafka + сервисы).
Kafka в кластере разворачивается сабчартом `helm/bank-app/charts/kafka` (StatefulSet с PVC,
headless-сервис, режим KRaft). Бэкенд-сервисы получают адрес брокера через
`KAFKA_BOOTSTRAP_SERVERS` (`kafka:9092`) из своих ConfigMap'ов. `helm test bank-app -n bank`
в том числе проверяет доступность брокера.

## Тесты

```cmd
mvn clean verify
```

Kafka-логику (продюсеры в accounts/cash/transfer и листенер в notifications) покрывают
интеграционные тесты на `@EmbeddedKafka` — отдельный брокер для них не нужен, всё
поднимается в рамках `mvn verify`.
