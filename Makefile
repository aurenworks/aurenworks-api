.PHONY: dev build run docker
dev:
	./mvnw quarkus:dev
build:
	./mvnw -DskipTests package
run:
	java -jar target/*-runner.jar
docker:
	docker build -t aurenworks/aurenworks-api:dev .
