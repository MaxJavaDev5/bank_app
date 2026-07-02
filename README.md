# My Bank App

Учебный проект «Банк» 
Яндекс Практикум (спринты 9–12).

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

Backend-сервисы и observability-стек в кластере, Keycloak и front — в docker compose
(гибридный режим).

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

В `docker-compose.yml` для `front-app` уже заданы переменные observability для гибридного
режима — front шлёт трейсы и логи в NodePort'ы кластера через `host.docker.internal`:

| Переменная | Значение |
|------------|----------|
| `GATEWAY_URL` | `http://host.docker.internal:30080` |
| `ZIPKIN_ENDPOINT` | `http://host.docker.internal:30411/api/v2/spans` |
| `LOGSTASH_HOST` | `host.docker.internal` |
| `LOGSTASH_PORT` | `30500` |

Backend-поды в namespace `bank` ходят на observability по DNS внутри кластера:
`zipkin:9411`, `prometheus:9090`, `logstash:5000`.

### Деплой

```cmd
helm dependency update helm/bank-app
helm upgrade --install bank-app helm/bank-app --namespace bank --create-namespace
kubectl get pods -n bank
helm test bank-app -n bank
```

Gateway снаружи доступен на NodePort 30080. Front — http://localhost:8085.

Чарты лежат в `helm/bank-app/` (зонтичный чарт и сабчарты: postgres, kafka, backend-сервисы,
zipkin, prometheus, grafana, elk). Kafka разворачивается сабчартом `charts/kafka`
(StatefulSet с PVC, headless-сервис, режим KRaft). Бэкенд-сервисы получают адрес брокера через
`KAFKA_BOOTSTRAP_SERVERS` (`kafka:9092`) из своих ConfigMap'ов.

`helm test bank-app -n bank` проверяет доступность postgres, kafka, backend-сервисов,
zipkin, prometheus, grafana, elasticsearch, logstash и kibana.

### Observability

| UI | NodePort | URL |
|----|--------|----|
| Gateway | 30080 | http://localhost:30080 |
| Zipkin | 30411 | http://localhost:30411 |
| Prometheus | 30090 | http://localhost:30090 |
| Grafana | 30300 | http://localhost:30300 (admin / admin) |
| Logstash (TCP) | 30500 |
| Kibana | 30561 | http://localhost:30561 |

Grafana подключена к Prometheus и содержит дашборды JVM/HTTP и
бизнес-метрики (`bank_transfer_total`, `bank_withdraw_failed_total`, `bank_transfer_failed_total`,
`bank_notification_failed_total`). Логи всех сервисов уходят в Logstash (JSON с `traceId`/`spanId`)
и индексируются в Elasticsearch как `bank-logs-*`.

### Проверка observability

1. Поднять кластер и front (см. выше), дождаться `Running` всех pod'ов в `bank`.
2. Войти в UI (http://localhost:8085) под `user` / `password`.
3. Выполнить операции: пополнение, перевод, снятие.
4. **Zipkin** — http://localhost:30411: найти trace `front-app → gateway-service → …` с тем же
   `traceId`, что в логах.
5. **Grafana** — http://localhost:30300: дашборды с RPS, 4xx/5xx, p95 HTTP и счётчиками
   `bank_*_failed_total`; проверить настроенный alert.
6. **Kibana** — http://localhost:30561 (настройки кибаны описаны ниже): импорт дашборда,
   фильтры по `service`, `traceId`, `level`; убедиться, что `traceId` совпадает с Zipkin.
7. **Prometheus** — http://localhost:30090/targets: backend targets в состоянии UP; пример PromQL:
   `rate(http_server_requests_seconds_count[1m])`.

### Настройка Kibana

Предварительные условия:

1. ELK-стек развёрнут в кластере (`elk.enabled: true` в values Helm).
2. Сервисы отправляют логи в Logstash (индекс `bank-logs-YYYY.MM.dd`).
3. Init Job создал шаблон индекса `bank-logs-template`
   (см. `helm/bank-app/charts/elk/config/bank-logs-index-template.json`).

#### Импорт дашборда

1. Открыть Kibana: http://localhost:30561
2. **Stack Management → Saved Objects → Import**.
3. Загрузить файл `helm/bank-app/charts/elk/config/bank-logs-dashboard.ndjson`.
4. Подтвердить импорт (создаётся data view `bank-logs-*` и дашборд **Bank Logs Dashboard**).

#### Data view

Экспорт уже содержит data view для паттерна `bank-logs-*` с полем времени `@timestamp`.
Если создаёте вручную: **Stack Management → Data Views → Create** → паттерн `bank-logs-*`.

## Тесты

```cmd
mvn clean verify
```

Kafka-логику (продюсеры в accounts/cash/transfer и листенер в notifications) покрывают
интеграционные тесты на `@EmbeddedKafka` — отдельный брокер для них не нужен, всё
поднимается в рамках `mvn verify`.
