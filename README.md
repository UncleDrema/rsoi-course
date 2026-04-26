# RSOI Course Project

Курсовой проект состоит из микросервисов бронирования авиабилетов, собственного OpenID Connect Identity Provider, сервиса статистики и SPA-фронтенда.

## Архитектура

- `frontend` - Svelte SPA, внешний UI, service port `80`.
- `gateway` - единая API-точка `/api/v1`, service port `8080`.
- `flights` - рейсы и аэропорты, service port `8060`.
- `tickets` - покупка и отмена билетов, service port `8070`.
- `privileges` - бонусная программа, service port `8050`.
- `identity-provider` - OIDC provider, service port `8090`.
- `statistics` - Kafka consumer и Admin-only отчеты, service port `8091`.
- `postgres` - общая PostgreSQL, service port `5432`.
- `redpanda` - single-node Kafka API, service port `9092`.

Все backend-сервисы валидируют JWT через issuer/JWKS `identity-provider`. События действий отправляются в Redpanda topic `rsoi.actions`.

## Helm Charts

- `charts/postgres` - PostgreSQL и init DB.
- `charts/redpanda` - single-node Redpanda.
- `charts/rsoi` - универсальный chart для HTTP-сервисов.

Values-файлы сервисов:

- `charts/rsoi/values-identity-provider.yaml`
- `charts/rsoi/values-statistics.yaml`
- `charts/rsoi/values-flights.yaml`
- `charts/rsoi/values-privileges.yaml`
- `charts/rsoi/values-tickets.yaml`
- `charts/rsoi/values-gateway.yaml`
- `charts/rsoi/values-frontend.yaml`

## Что Настроить Перед Деплоем

### 1. Images

Dockerfiles/local build не входят в этот репозиторий. Перед деплоем соберите и опубликуйте образы:

- `identity-provider`
- `statistics`
- `flights`
- `privileges`
- `tickets`
- `gateway`
- `frontend`

В каждом values-файле обновите:

```yaml
image:
  repository: "<registry>/<image>"
  tag: "<tag>"
imagePullSecrets: []
```

### 2. Ingress И Публичные URL

Для backend-валидации внутри кластера достаточно issuer:

```text
http://identity-provider:8090
```

Но для SPA login flow браузер должен видеть тот же issuer, который указан в OIDC metadata и JWT. Поэтому для полноценной авторизации в браузере нужен публичный URL `identity-provider`, доступный через Ingress.

Практически нужно согласовать:

- `auth.issuerUri` в `values-identity-provider.yaml`.
- `auth.issuerUri` в `values-gateway.yaml`, `values-flights.yaml`, `values-tickets.yaml`, `values-privileges.yaml`, `values-statistics.yaml`, `values-frontend.yaml`.
- `IDENTITY_PROVIDER_ISSUER`.
- `IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_1`.
- `IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_2`.
- `VITE_OIDC_AUTHORITY`.
- Ingress host/path для `frontend`, `gateway`, `identity-provider`.

Пример для одного host:

```text
Frontend: http://130.193.57.16.nip.io/
Gateway API: http://130.193.57.16.nip.io/api/v1
IdP issuer: http://130.193.57.16.nip.io/idp
SPA callback: http://130.193.57.16.nip.io/auth/callback
```

Тогда в values нужно выставить:

```yaml
auth:
  issuerUri: "http://130.193.57.16.nip.io/idp"
```

И для `identity-provider`:

```yaml
env:
  - name: IDENTITY_PROVIDER_ISSUER
    value: "http://130.193.57.16.nip.io/idp"
  - name: IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_1
    value: "http://130.193.57.16.nip.io/auth/callback"
  - name: IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_2
    value: "http://130.193.57.16.nip.io/auth/callback"
```

Для `frontend`:

```yaml
env:
  - name: VITE_OIDC_AUTHORITY
    value: "http://130.193.57.16.nip.io/idp"
  - name: VITE_OIDC_CLIENT_ID
    value: "spa-client"
  - name: VITE_API_BASE_PATH
    value: "/api/v1"
```

Важно: если IdP публикуется под path `/idp`, Ingress должен корректно проксировать все OIDC endpoints:

- `/idp/.well-known/openid-configuration`
- `/idp/oauth2/authorize`
- `/idp/oauth2/token`
- `/idp/oauth2/jwks`
- `/idp/userinfo`

Если path rewrite настроить сложно, проще дать `identity-provider` отдельный host, например:

```text
http://idp.130.193.57.16.nip.io
```

### 3. PostgreSQL

`charts/postgres` по умолчанию создает:

- `flights`
- `tickets`
- `privileges`
- `identity_provider`
- `statistics`

Параметры:

```yaml
postgresql:
  username: "postgres"
  password: "postgres"
  applicationUser:
    name: "program"
    password: "test"
```

Для production-like стенда лучше заменить пароли и прокинуть их в сервисные values:

- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_URL`

### 4. Redpanda

`charts/redpanda` поднимает single-node брокер. Внутренний адрес:

```text
redpanda:9092
```

Сервисные параметры:

- `FLIGHTS_KAFKA_BOOTSTRAP_SERVERS`
- `TICKETS_KAFKA_BOOTSTRAP_SERVERS`
- `PRIVILEGES_KAFKA_BOOTSTRAP_SERVERS`
- `STATISTICS_KAFKA_BOOTSTRAP_SERVERS`
- topic: `rsoi.actions`

## Установка В Кластер

Пример для namespace `rsoi`:

```powershell
kubectl create namespace rsoi
```

Рекомендуемый порядок:

```powershell
helm upgrade --install postgres charts/postgres -n rsoi
helm upgrade --install redpanda charts/redpanda -n rsoi

helm upgrade --install identity-provider charts/rsoi -n rsoi -f charts/rsoi/values-identity-provider.yaml
helm upgrade --install statistics charts/rsoi -n rsoi -f charts/rsoi/values-statistics.yaml
helm upgrade --install flights charts/rsoi -n rsoi -f charts/rsoi/values-flights.yaml
helm upgrade --install privileges charts/rsoi -n rsoi -f charts/rsoi/values-privileges.yaml
helm upgrade --install tickets charts/rsoi -n rsoi -f charts/rsoi/values-tickets.yaml
helm upgrade --install gateway charts/rsoi -n rsoi -f charts/rsoi/values-gateway.yaml
helm upgrade --install frontend charts/rsoi -n rsoi -f charts/rsoi/values-frontend.yaml
```

## Проверка Через Lens

1. Откройте cluster в Lens и выберите namespace `rsoi`.
2. В `Workloads -> Pods` дождитесь `Running` и `Ready`:
   - `postgres`
   - `redpanda`
   - `identity-provider`
   - `statistics`
   - `flights`
   - `privileges`
   - `tickets`
   - `gateway`
   - `frontend`
3. В `Network -> Services` проверьте порты:
   - `postgres:5432`
   - `redpanda:9092`
   - `identity-provider:8090`
   - `statistics:8091`
   - `flights:8060`
   - `privileges:8050`
   - `tickets:8070`
   - `gateway:8080`
   - `frontend:80`
4. В `Network -> Ingresses` проверьте host/path для `frontend`, `gateway`, `identity-provider`.
5. В `Pods -> Logs` проверьте:
   - ошибки подключения к PostgreSQL
   - ошибки подключения к `redpanda:9092`
   - ошибки issuer/JWKS/JWT
   - `connection refused` между сервисами
6. Через Lens можно сделать port-forward:
   - `identity-provider:8090` для проверки OIDC discovery/JWKS
   - `gateway:8080` для API
   - `frontend:80` для SPA

## Проверка OIDC

Через port-forward:

```powershell
kubectl -n rsoi port-forward svc/identity-provider 8090:8090
curl http://127.0.0.1:8090/.well-known/openid-configuration
curl http://127.0.0.1:8090/oauth2/jwks
```

Через публичный Ingress:

```powershell
curl http://<public-issuer>/.well-known/openid-configuration
curl http://<public-issuer>/oauth2/jwks
```

Проверьте, что поле `issuer` в discovery document совпадает с:

- `VITE_OIDC_AUTHORITY`
- `IDENTITY_PROVIDER_ISSUER`
- `auth.issuerUri`
- issuer в JWT

## Локальная Верификация

Helm:

```powershell
helm lint charts/postgres
helm lint charts/redpanda
helm lint charts/rsoi

helm template postgres charts/postgres
helm template redpanda charts/redpanda
helm template identity-provider charts/rsoi -f charts/rsoi/values-identity-provider.yaml
helm template statistics charts/rsoi -f charts/rsoi/values-statistics.yaml
helm template flights charts/rsoi -f charts/rsoi/values-flights.yaml
helm template privileges charts/rsoi -f charts/rsoi/values-privileges.yaml
helm template tickets charts/rsoi -f charts/rsoi/values-tickets.yaml
helm template gateway charts/rsoi -f charts/rsoi/values-gateway.yaml
helm template frontend charts/rsoi -f charts/rsoi/values-frontend.yaml
```

Backend:

```powershell
cd services/<service>
.\gradlew.bat compileJava
.\gradlew.bat test
```

Frontend:

```powershell
cd frontend/spa
npm ci
npm run check
npm run test
npm run build
```
