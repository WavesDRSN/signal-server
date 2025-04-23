package wavesDRSN.p2p_messenger_backend.exceptions;

public class UsernameAlreadyExistsException extends RuntimeException{
    // Конструктор, принимающий сообщение об ошибке
    public UsernameAlreadyExistsException(String message) {
        super(message); // Передаем сообщение родительскому классу RuntimeException
    }

    // Конструктор, принимающий "причину" (cause) - другое исключение
    public UsernameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
