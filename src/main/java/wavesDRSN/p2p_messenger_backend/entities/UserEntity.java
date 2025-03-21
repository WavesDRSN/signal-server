package wavesDRSN.p2p_messenger_backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// сущность юзера для инфы о юзере, по мере необходимости дополню
public class UserEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "publicName", nullable = false) // то есть отображаемое имя для др пользователей
    private String publicName;

    @Column(name = "bio")
    private String bio; // описание

    @Column(nullable = false)
    private Boolean isOnline;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt; // дата регистрации

    @Column(nullable = false)
    private LocalDateTime lastActiveAt; // последняя активность


    // тк проект с упором в анонимность: email/телефон отсутствует
}
