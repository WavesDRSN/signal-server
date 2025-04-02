# Этап сборки
FROM gradle:8.7-jdk21-jammy AS build

WORKDIR /app
COPY . .

# Сборка
RUN gradle --no-daemon clean bootJar -x test

# Финальный образ
FROM bellsoft/liberica-openjre-alpine:21

# Настройка пользователя и прав
RUN adduser -S app-user && \
    mkdir -p /app && \
    chown app-user /app

WORKDIR /app
USER app-user

# Копируем JAR и миграции
COPY --from=build --chown=app-user /app/build/libs/p2p-messenger-backend-*.jar app.jar
COPY db ./db

EXPOSE 50051

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]