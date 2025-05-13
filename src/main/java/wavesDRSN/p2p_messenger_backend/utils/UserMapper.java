package wavesDRSN.p2p_messenger_backend.utils;

import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;

@Component
public class UserMapper {
    public UserDTO toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .publicKey(entity.getPublicKey())
                .build();
    }
}
