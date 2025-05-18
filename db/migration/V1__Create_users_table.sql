CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,       -- автоинкрементный уник ID
    username   VARCHAR(32) NOT NULL,        -- логин
    public_key BYTEA       NOT NULL UNIQUE, -- публичный ключ
    fcm_token TEXT NOT NULL UNIQUE,

    -- Ограничения
    CONSTRAINT username_length CHECK (LENGTH(username) BETWEEN 5 AND 32),
    CONSTRAINT username_format CHECK (username ~ '^[a-zA-Z0-9_]+$')
);

-- Регистронезависимая уникальность логина (trippi_troppi = Trippi_Troppi = TRIPPI_TROPPI и тд)
-- Ник сохраняется в БД как введен пользователем.
CREATE UNIQUE INDEX idx_users_lower_username
    ON users (LOWER(username));