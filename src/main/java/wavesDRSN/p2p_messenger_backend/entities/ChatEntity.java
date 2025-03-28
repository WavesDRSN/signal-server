package wavesDRSN.p2p_messenger_backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import wavesDRSN.p2p_messenger_backend.enums.ChatType;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "???chats???")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

// сущность чата для хранения инфы о беседе, типе и участниках и тд
public class ChatEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "chatType")
    private ChatType chatType;

    @Column(name = "chatName")
    private String chatName;

    // для связи соо и чата
    // @OneToMany("название тб")
    // private List<MessageEntity> messages;

    // private int participants - опционально кол-во участников чата
    //------------------------------------------------------------
    // createdAt - когда чат создан
    // updatedAt - изменение чата (например новый участник пришел, поменялось название)
    // lastMessageAt - дата последнего соо для сортировки чато
    //------------------------------------------------------------
    // ...
}
