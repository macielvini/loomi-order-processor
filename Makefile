APP_NAME=order_processor
DOCKER_COMPOSE=docker compose

-include .env
export

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
	@echo ">>>> Make sure your variables are pointing to the correct database"
	@SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/order_processor; \
	echo "Current value: $$SPRING_DATASOURCE_URL"; \
	echo "Do you want to continue? (y/n)"; \
	read continue; \
	if [ "$$continue" != "y" ]; then \
		echo "Aborting..."; \
		exit 1; \
	fi; \
	mvn -B flyway:migrate
