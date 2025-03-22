package wavesDRSN.p2p_messenger_backend.services;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface OnlineUserService {
    /**
     * Регистрирует пользователю онлайн-статус
     * @param username имя пользователя
     * @param connection объект, представляющий gRPC-соединение
     */
    void registerOnlineUser(String username, Object connection);

    /**
     * Возвращает объект соединения для пользователя, если он онлайн.
     * @param username имя юзера
     * @return Object, содержащий объект соединения, или пустой, если пользователь не найден
     */
    Optional<Object> getConnection(String username);

    /**
     * Удаляет пользователя, если он офлайн
     * @param username имя юзера
     */
    void removeOnlineUser(String username);

    /**
     * Обновление статуса жизни
     * @param username имя юзера
     */
    void refreshKeepAlive(String username);
}
