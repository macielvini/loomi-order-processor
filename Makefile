APP_NAME=order_processor
DOCKER_COMPOSE=docker compose

.PHONY: setup up down build test clean logs db-migrate

setup:
	$(DOCKER_COMPOSE) build

build:
	mvn -B -DskipTests package
	docker build -t $(APP_NAME):latest .

test:
	mvn -B clean test

up:
	$(DOCKER_COMPOSE) up --build

down:
	$(DOCKER_COMPOSE) down

logs:
	$(DOCKER_COMPOSE) logs -f app

clean:
	mvn -B clean
	$(DOCKER_COMPOSE) down -v
	docker rmi $(APP_NAME):latest || true

db-migrate:
	mvn -B flyway:migrate
