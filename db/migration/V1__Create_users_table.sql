CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY, -- автоинкрементный уник ID
                       login VARCHAR(32) NOT NULL, -- логин
                       public_key BYTEA NOT NULL UNIQUE, -- публичный ключ
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- дата регистрации юзера

    -- Ограничения
                       CONSTRAINT login_length CHECK (LENGTH(login) BETWEEN 5 AND 32),
                       CONSTRAINT login_format CHECK (login ~ '^[a-zA-Z0-9_]+$')
    );

-- Регистронезависимая уникальность логина (trippi_troppi = Trippi_Troppi = TRIPPI_TROPPI и тд)
-- Ник сохраняется в БД как введен пользователем.
CREATE UNIQUE INDEX idx_users_lower_login
    ON users (LOWER(login));