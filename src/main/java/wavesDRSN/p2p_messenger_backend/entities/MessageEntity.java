package wavesDRSN.p2p_messenger_backend.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import wavesDRSN.p2p_messenger_backend.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// инфа о тексте в соо, получателе, отправителе, времени отправки, статусе, к какому чату принадлежит. шифрование?
public class MessageEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID chatId;

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID senderId;

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID receiverId;

    @ManyToOne
    @JoinColumn(name = "chatId")
    private ChatEntity chat; // какому чату принадлежит

    //хз надо ли тут @Column(name = "")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "messageStatus")
    private MessageStatus status;

    private LocalDateTime createdAt; // время отправки соо
}
