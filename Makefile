APP_NAME=order_processor
DOCKER_COMPOSE=docker compose

.PHONY: all

setup:
	$(DOCKER_COMPOSE) build

build:
	mvn -B -DskipTests package
	docker build -t $(APP_NAME):latest .

test:
	mvn -B clean test

coverage:
	mvn -B clean test jacoco:report
	@echo "Coverage report generated in: target/site/jacoco/index.html"

up:
	$(DOCKER_COMPOSE) up -d --build

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
