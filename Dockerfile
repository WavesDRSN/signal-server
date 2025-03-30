# Этап сборки
FROM gradle:8.7-jdk21-alpine AS build
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
COPY src/ src/
COPY proto/ proto/

# Генерация Protobuf + сборка проекта
RUN gradle clean build -x test

# Этап извлечения слоев
FROM bellsoft/liberica-openjre-alpine:21 AS layers

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Извлечение слоев Spring Boot
RUN java -Djarmode=layertools -jar app.jar extract

# Этап подготовки окружения
FROM bellsoft/liberica-openjre-alpine:21
VOLUME /tmp
RUN adduser -S app-user
USER app-user
COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

# Копирование миграций Flyway
COPY db/migration ./db/migration/

# Порты
EXPOSE 50051

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]