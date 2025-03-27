package wavesDRSN.p2p_messenger_backend.utils;

import lombok.*;

@Getter
@AllArgsConstructor
public class OnlineUserSession { // Класс для хранения объекта соединения и времени последнего keepAlive
    private final Object connection;

    @Setter
    private volatile long lastKeepAlive;
}
