APP_NAME=order_processor
DOCKER_COMPOSE=docker compose

.PHONY: test docker-tests build up down logs clean run-local run-dev-docker stop-dev-docker deploy-local

test:
	mvn -B clean test

docker-tests:
	$(DOCKER_COMPOSE) --profile test run --rm tests

build:
	mvn -B -DskipTests package
	docker build -t $(APP_NAME):latest .

up:
	$(DOCKER_COMPOSE) up --build

down:
	$(DOCKER_COMPOSE) down

logs:
	$(DOCKER_COMPOSE) logs -f app

clean:
	mvn -B clean
	docker rmi $(APP_NAME):latest || true

run-local:
	mvn -Dspring-boot.run.profiles=local spring-boot:run

run-dev-docker:
	docker run --rm -it \
		--name $(APP_NAME)-dev \
		--mount type=bind,source="$(shell pwd)",target=/app \
		--mount type=bind,source="$(HOME)/.m2",target=/root/.m2 \
		-p 8080:8080 \
		-w /app \
		maven:3.9.4-eclipse-temurin-17 \
		mvn -Dspring-boot.run.profiles=dev spring-boot:run

stop-dev-docker:
	docker rm -f $(APP_NAME)-dev || true

deploy-local: test build up
