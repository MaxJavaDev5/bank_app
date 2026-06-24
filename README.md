# My Bank App

Микросервисное банковское приложение. 
Спринт 9-10, Яндекс Практикум.

## Стек

Java 21, Maven, Spring Boot 3.4.4, Spring Cloud Gateway, Keycloak, PostgreSQL, JPA, Resilience4j, Spring Cloud Contract, Helm, Kubernetes.

## Модули

| Модуль | Порт | Что делает |
|--------|------|------------|
| gateway-service | 8080 | Единая точка входа: принимает запросы от фронта и направляет их в нужный сервис |
| accounts-service | 8081 | Хранит счета пользователей и их баланс |
| cash-service | 8082 | Пополняет счёт и снимает деньги |
| transfer-service | 8083 | Переводит деньги между счетами |
| notifications-service | 8084 | Пишет в лог, какая операция произошла |
| front-app | 8085 | Веб-страница: вход и работа со счётом |

## Запуск локально (docker-compose)

Инструкции для **Windows** (cmd).

```cmd
mvn clean package -DskipTests
docker compose up --build
```

UI: http://localhost:8085

Пользователи: `user` / `user2` / `user3`, пароль `password`.

Keycloak admin: `admin` / `admin`, http://localhost:8180

## Kubernetes (Helm)

Backend в кластере, Keycloak и front — не в кластере.

**Требования:** Windows, JDK 21, Docker Desktop с Kubernetes, Helm 3, kubectl.

### 1. Сборка образов

```cmd
docker build -t local/accounts-service:latest ./accounts-service
docker build -t local/cash-service:latest ./cash-service
docker build -t local/transfer-service:latest ./transfer-service
docker build -t local/notifications-service:latest ./notifications-service
docker build -t local/gateway-service:latest ./gateway-service
```

### 2. Keycloak + front снаружи кластера

```cmd
set GATEWAY_URL=http://localhost:30080
docker compose up -d keycloak front-app
```

Front: http://localhost:8085 → Gateway NodePort http://localhost:30080

### 3. Деплой backend в K8s

```cmd
helm dependency update helm/bank-app
helm upgrade --install bank-app helm/bank-app --namespace bank --create-namespace
kubectl get pods -n bank
helm test bank-app -n bank
```

### Структура Helm

```
helm/bank-app/           — зонтичный чарт
  charts/postgres/       
  charts/accounts-service/
  charts/cash-service/
  charts/transfer-service/
  charts/notifications-service/
  charts/gateway-service/ — NodePort 30080
```

## Сборка и тесты

Нужен **JDK 21** (Windows).

```cmd
java -version
mvn -version
mvn clean verify
```

## Схема

```
front-app (снаружи) → gateway NodePort :30080 → accounts / cash / transfer / notifications (K8s)
Keycloak (снаружи), postgres (K8s StatefulSet)
```
