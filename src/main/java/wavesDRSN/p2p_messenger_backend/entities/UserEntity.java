package wavesDRSN.p2p_messenger_backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"public_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    @NotBlank(message = "Username не может быть пустым")
    @Size(min = 5, max = 32, message = "Длина логина должна быть от 5 до 32 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Логин может содержать только латинские буквы, цифры и символ подчеркивания")
    private String username;

    @Column(name = "public_key", nullable = false, unique = true, columnDefinition = "BYTEA")
    @NotBlank(message = "Публичный ключ не может быть пустым")
    private byte[] publicKey;
}