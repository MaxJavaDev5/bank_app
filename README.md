# My Bank App

Учебный проект «Банк» 
Яндекс Практикум (спринты 9–10).

Стек: Java 21, Spring Boot, Gateway, Keycloak, PostgreSQL, Helm/Kubernetes.

## Сервисы

| Сервис | Порт |
|--------|------|
| gateway-service | 8080 |
| accounts-service | 8081 |
| cash-service | 8082 |
| transfer-service | 8083 |
| notifications-service | 8084 |
| front-app | 8085 |

Gateway принимает запросы с фронта и раздаёт их по микросервисам. Notifications просто пишет в лог, что произошло.

## Запуск локально

Нужен JDK 21.

```cmd
mvn clean package -DskipTests
docker compose up --build
```

UI: http://localhost:8085

Пользователи `user`, `user2`, `user3` — пароль `password`.

Keycloak: http://localhost:8180, admin / admin.

## Kubernetes

По заданию backend в кластере, Keycloak и front — в docker compose.

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
set GATEWAY_URL=http://localhost:30080
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

Чарты лежат в `helm/bank-app/` (зонтичный чарт и сабчарты на postgres + сервисы).

## Тесты

```cmd
mvn clean verify
```
