FROM openjdk:17-jdk-slim as builder

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Финальный образ
FROM openjdk:17-jdk-slim

WORKDIR /app
# Создайте директорию при сборке
RUN mkdir -p /var/log/shatilo/ && \
    chmod 755 /var/log/shatilo/

# Убедитесь, что у пользователя есть права
USER root

# Копируем JAR из stage builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]