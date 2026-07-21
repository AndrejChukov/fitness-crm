# 🏋️ Fitness CRM — Multi-tenant REST API

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Testcontainers](https://img.shields.io/badge/Testcontainers-291A3B?style=for-the-badge&logo=docker&logoColor=white)

Модульный монолит для фитнес-клубов: multi-tenancy, абонементы, расписание и бронирование, финансы (счета / долги / оплата), подготовка payroll тренеров. Проект ориентирован на демонстрацию middle-level навыков: изоляция тенантов, фасады между модулями, optimistic locking, конкурентные сценарии, scheduled jobs и интеграционные тесты на реальной PostgreSQL.

## 📑 Оглавление
- [О проекте](#-о-проекте)
- [Бизнес-логика](#-бизнес-логика)
- [Технологический стек](#-технологический-стек)
- [Ключевые архитектурные решения](#-ключевые-архитектурные-решения)
- [Структура модулей](#-структура-модулей)
- [Быстрый запуск (Docker Compose + приложение)](#-быстрый-запуск-docker-compose--приложение)
- [Локальный запуск](#-локальный-запуск)
- [Учётные записи (seed)](#-учётные-записи-seed)
- [API Документация (краткая выжимка)](#-api-документация-краткая-выжимка)
- [Тестирование](#-тестирование)
- [Направления для развития (Roadmap)](#-направления-для-развития-roadmap)

---

## 🎯 О проекте

**Fitness CRM** — backend pet-project уровня junior → middle: не круд ради круда, а доменная модель фитнес-клуба с жёсткими инвариантами.

| Задача | Как решено |
|--------|------------|
| Несколько клубов в одной БД | Multi-tenancy через `TenantContext` + Hibernate `@Filter` |
| Не переполнить группу | Capacity check + `@Version` на `ClassSession` |
| Не увести абонемент в минус | `@Version` на `ClientMembership` + `deductClass` |
| Не брать долг дважды | Invoice `UNPAID → OVERDUE` в nightly job |
| Модули не «протекают» | `MembershipFacade` / `FinanceFacade` / `SchedulingFacade` |
| Доказуемость | Unit + integration tests (Testcontainers PostgreSQL) |

Репозиторий: [github.com/AndrejChukov/fitness-crm](https://github.com/AndrejChukov/fitness-crm)

---

## 💼 Бизнес-логика

### 1. Identity & multi-tenancy
- Регистрация и login с выдачей **JWT**
- Роли: `SUPER_ADMIN`, `TENANT_ADMIN`, `TRAINER`, `CLIENT`
- Изоляция данных тенанта: чужой ресурс → `404` / `403`, не «утечка» между клубами
- `SUPER_ADMIN` управляет тенантами; админ клуба — пользователями своего tenant

### 2. Memberships — жизненный цикл абонемента
- Шаблоны абонементов (`MembershipTemplate`) и выдача клиенту (`assign`)
- Статусы: `ACTIVE` → `FROZEN` / `DEPLETED` / `EXPIRED`
- **Freeze / unfreeze** с лимитом **14 дней** и **cap-политикой** (не застревать в `FROZEN`)
- **Списание занятия** (`deductClass`) с защитой `@Version`
- Ночной cron: просроченные `ACTIVE`/`FROZEN` → `EXPIRED`
- MapStruct-маппинг ответов API

### 3. Scheduling — расписание и бронирование
- Залы (`Facility`) и занятия (`ClassSession`)
- **Conflict resolver**: пересечение по тренеру / залу (смежные интервалы не конфликтуют)
- Бронирование: проверка абонемента + баланса + capacity
- **Optimistic locking** (`@Version` на `ClassSession`) при concurrent book
- Отмена: **> 12h** → `CANCELLED` (без списания); **≤ 12h** → `LATE_CANCELED` + списание класса через facade

### 4. Finance — счета, долги, оплата
- При `assign` создаётся `Invoice` (`UNPAID`, due = today + 3 дня) и `ClientAccount` (balance `0`)
- Nightly job: просроченные `UNPAID` → долг на балансе + статус **`OVERDUE`** (без повторного начисления)
- Отрицательный баланс блокирует бронирование (`FinanceFacade.canBookClasses`)
- Оплата: `POST .../invoices/{id}/pay` (CASH / CARD) → `Transaction`, `PAID`, идемпотентность (повторная оплата запрещена)
- Scheduled **trainer payroll**: base `$10` + `$2` × `ATTENDED` (через `SchedulingFacade`)

### 5. Audit (задел)
- Модуль `audit` + зависимость **Hibernate Envers** в classpath
- Полноценный audit trail критичных изменений — в roadmap

---

## 🛠 Технологический стек

* **Core:** Java 21, Spring Boot 3.5
* **Security:** Spring Security, JWT (JJWT)
* **Data:** Spring Data JPA, Hibernate, PostgreSQL 16, Flyway
* **Concurrency:** JPA `@Version` (optimistic locking)
* **Integration between modules:** Facade / Application Service pattern
* **Mapping:** MapStruct
* **Infrastructure:** Docker Compose (PostgreSQL, Redis, pgAdmin)
* **API docs:** Springdoc OpenAPI / Swagger UI
* **Testing:** JUnit 5, Mockito, Spring Boot Test, **Testcontainers** (PostgreSQL)
* **Async / jobs:** `@Scheduled` (expiration, unpaid invoices, payroll)

---

## ✨ Ключевые архитектурные решения

1. **Modular monolith** — пакеты `identity`, `memberships`, `scheduling`, `finance`, `audit`, `common` вместо одной кучи сервисов.
2. **Multi-tenancy** — `TenantContext` (ThreadLocal) заполняется из JWT; Hibernate filter на `tenant_id`.
3. **Facades на границах модулей** — Scheduling не ходит в membership repositories; Finance не знает про Booking напрямую.
4. **Optimistic locking** — защита capacity и списания занятий под конкурентной нагрузкой; конфликт мапится в HTTP **409**.
5. **Инварианты домена в сервисах** — state machine абонемента, 12h cancel policy, идемпотентная оплата, OVERDUE без double-debt.
6. **Flyway** — схема только миграциями (V1–V9).
8. **Интеграционные тесты на реальной БД** — Testcontainers, concurrent booking (`CountDownLatch`), stale `@Version`, finance jobs.

---

## 🗂 Структура модулей

```text
ru.fitnesscrm
├── identity      # auth, JWT, tenants, users, roles
├── memberships   # templates, client memberships, freeze, deduct, expiration job
├── scheduling    # facilities, class sessions, bookings, cancel policy
├── finance       # invoices, accounts, pay, unpaid job, trainer payroll
├── audit         # audit entities / Envers prep
├── common        # TenantContext, BaseEntity, exceptions, ApiResponse
└── config        # Security, tenant filter aspect, OpenAPI
```

---

## 🐳 Быстрый запуск (Docker Compose + приложение)

`docker-compose.yml` поднимает **инфраструктуру** (приложение запускается отдельно через Maven).

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/AndrejChukov/fitness-crm.git
   cd fitness-crm
   ```

2. Поднимите PostgreSQL, Redis и pgAdmin:
   ```bash
   docker compose up -d
   ```

3. Запустите приложение (JDK 21):
   ```bash
   ./mvnw spring-boot:run
   ```

4. Сервисы:
   * **API:** `http://localhost:8080`
   * **Swagger UI:** `http://localhost:8080/swagger-ui.html`
   * **OpenAPI JSON:** `http://localhost:8080/api-docs`
   * **pgAdmin:** `http://localhost:5050` (см. credentials в `docker-compose.yml`)

---

## 🚀 Локальный запуск

### Требования
* **JDK 21+**
* **Maven 3.9+** (или `./mvnw`)
* PostgreSQL на `localhost:5432`, БД `fitness_crm` / user `postgres` / password `postgres`
* Redis на `localhost:6379` (зависимость в classpath; в тестах Redis auto-config отключён)

### Запуск
```bash
./mvnw clean install
./mvnw spring-boot:run
```

Конфигурация: `src/main/resources/application.yaml`  
Секрет JWT: `JWT_SECRET` (env) или значение по умолчанию из yaml.

---

## 👤 Учётные записи (seed)

| Email | Роль | Пароль |
|-------|------|--------|
| `admin@fitnesscrm.local` | `SUPER_ADMIN` | `Admin123!` |

Дополнительные пользователи сидятся миграцией `V7` (`tenant@mail.ru`, `client@mail.ru`) — для них используйте известный вам локальный пароль / пересоздайте через API при необходимости.

---

## 📚 API Документация (краткая выжимка)

> Для защищённых эндпоинтов: заголовок `Authorization: Bearer <token>`.  
> Полная спецификация — в **Swagger UI**.

### 🔑 Auth & Tenants & Users
| Метод | Эндпоинт | Описание | Доступ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Регистрация | Публичный |
| `POST` | `/api/v1/auth/login` | JWT | Публичный |
| `GET/POST` | `/api/v1/tenants` | Управление тенантами | `SUPER_ADMIN` |
| `GET/POST` | `/api/v1/users` | Пользователи клуба | `TENANT_ADMIN` |

### 🎟 Memberships
| Метод | Эндпоинт | Описание | Доступ |
| :--- | :--- | :--- | :--- |
| `GET/POST` | `/api/v1/memberships/templates` | Шаблоны абонементов | чтение: staff+client / создание: admin |
| `POST` | `/api/v1/memberships/assign` | Выдача абонемента (+ invoice) | `TENANT_ADMIN`, `TRAINER` |
| `GET` | `/api/v1/memberships/clients/{clientId}` | Абонементы клиента | staff / свой CLIENT |
| `POST` | `/api/v1/memberships/{id}/freeze` | Заморозка | staff + client |
| `POST` | `/api/v1/memberships/{id}/unfreeze` | Разморозка | staff + client |
| `POST` | `/api/v1/memberships/{id}/deduct-class` | Списание занятия | `TENANT_ADMIN`, `TRAINER` |

### 📅 Scheduling
| Метод | Эндпоинт | Описание | Доступ |
| :--- | :--- | :--- | :--- |
| `GET/POST` | `/api/v1/scheduling/facilities` | Залы | CRUD по ролям |
| `POST` | `/api/v1/scheduling/sessions` | Создание занятия (conflict check) | admin / trainer |
| `POST` | `/api/v1/scheduling/bookings` | Бронь | admin / trainer / client |
| `POST` | `/api/v1/scheduling/bookings/{id}/cancel` | Отмена (12h policy) | admin / trainer / client |

### 💳 Finance
| Метод | Эндпоинт | Описание | Доступ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/finance/invoices/{id}/pay` | Оплата (CASH/CARD) | `TENANT_ADMIN` |

---

## 🧪 Тестирование

* **Unit-тесты** — бизнес-правила (freeze cap, deduct, jobs) с Mockito / Spring context где нужно  
* **Integration-тесты** — Testcontainers PostgreSQL:
  * concurrent booking + optimistic lock
  * cancel policy (в т.ч. ровно 12h)
  * invoice / OVERDUE job / pay idempotency
  * membership expiration

```bash
./mvnw test
```

Профиль тестов: `tests` (`application-tests.yaml` — Redis autoconfig исключён).

---

## 📈 Направления для развития (Roadmap)

- [ ] **Audit trail** — Hibernate Envers / EntityListeners на оплату и критичные изменения  
- [ ] **Trainer payroll** — идемпотентность по `classSessionId`, фильтр только прошедших занятий, довести AC TASK-3.3  
- [ ] **Redis cache** — кеш шаблонов / facilities с инвалидацией  
- [ ] **Dockerfile приложения** + полный `docker compose` (app + DB)  
- [ ] **CI/CD** — GitHub Actions (build + test + Testcontainers)  
- [ ] **Observability** — Actuator, метрики, structured logging  
- [ ] **Self-service покупка абонемента** клиентом (через Finance, не только `assign`)


---

*Developed by [Andrej Chuchkalov](https://t.me/Andrej_ch)*
