# MOEX Alert — Сервис мониторинга и оповещения о движениях цены акции по данным MOEX

## Возможности
1. **Виртуальные потоки**
2. **Управление тикерами и доступ к истории оповещений:** CRUD через REST API
   - OpenAPI UI: `http://localhost:8082/swagger-ui.html` (порт настраивается через `APP_PORT` в `docker.env`)
3. **Интеграция с MOEX ISS:** Периодический сбор котировок отслеживаемых акций
4. **Анализ трендов:** Нативная SQL-логика генерации алертов при выходе цены за границы коридора
5. **WebSocket оповещения:** Потоковая доставка сигналов в реальном времени
   - WebSocket: `ws://localhost:8082/ws/alerts`
6. **Проверка здоровья:** Эндпоинт мониторинга состояния `/actuator/health` (Spring Boot Actuator)
7. **Автоматический сидинг:** Автоматическое добавление тикеров `SBER`, `YDEX`, `X5` с порогом `0.1` при первом запуске через Flyway-миграцию

---

## Стек технологий
- **бэкенд:** Java 25, Spring Boot 4 / Actuator / Data JPA / WebMVC / WebSocket, Flyway, Lombok
- **база данных:** PostgreSQL 15
- **контейнеризация:** Docker, Docker Compose (multi-stage build)

---

## Конфигурация (`docker.env`)
Перед запуском вы можете настроить параметры окружения в файле `docker.env`:
```env
DB_HOST=postgres
DB_PORT=5433
DB_NAME=moex_alert
DB_USERNAME=postgres
DB_PASSWORD=secret
APP_PORT=8082
```

---

## Развертывание через Docker Compose

### 1. Запуск проекта
Для сборки и запуска приложения (вместе с PostgreSQL) используйте:
```bash
docker compose --env-file docker.env up --build -d
```

### 2. Проверка статуса и здоровья
Проверить статус контейнеров:
```bash
docker compose ps
```
Проверить здоровье приложения:
```bash
curl http://localhost:8082/actuator/health
```

### 3. Полный сброс базы данных (при необходимости)
Если потребовалось пересоздать базу данных с нуля (например, при изменении пароля):
```bash
docker compose down -v
docker compose --env-file docker.env up --build -d
```

---

## Примеры запросов

- **Создание тикера:**
  ```bash
  curl -X POST http://localhost:8082/api/tickers \
    -H "Content-Type: application/json" \
    -d '{"name": "GAZP", "threshold": 2.0, "currency": "RUB"}'
  ```

- **Получение списка тикеров:**
  ```bash
  curl http://localhost:8082/api/tickers
  ```

## Планы по развитию
1. Покрыть тестами
2. Авторизация
