package wavesDRSN.p2p_messenger_backend.auth;

import com.google.protobuf.ByteString;
import gRPC.v1.Authentication.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.auth.model.*;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.security.CryptographyServiceImpl;
import wavesDRSN.p2p_messenger_backend.security.JwtTokenProvider;
import wavesDRSN.p2p_messenger_backend.services.auth.*;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

@GrpcService
public class AuthServiceImpl extends AuthorisationGrpc.AuthorisationImplBase {

    private final NicknameReservationService reservationService;
    private final UserService userService;
    private final ChallengeService challengeService;
    private final JwtTokenProvider tokenProvider;
    private final CryptographyServiceImpl cryptoService;

    @Autowired
    public AuthServiceImpl(NicknameReservationService reservationService,
                          UserService userService,
                          ChallengeService challengeService,
                          JwtTokenProvider tokenProvider,
                          CryptographyServiceImpl cryptoService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.challengeService = challengeService;
        this.tokenProvider = tokenProvider;
        this.cryptoService = cryptoService;
    }

    @Override
    public void reserveNickname(ReserveNicknameRequest request,
                               StreamObserver<ReserveNicknameResponse> responseObserver) {
        try {
            String nickname = request.getNickname();

            // Проверяем занятость никнейма везде
            if (isNicknameUnavailable(nickname)) {
                responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Nickname already taken or reserved")
                    .asRuntimeException());
                return;
            }

            NicknameReservation reservation = reservationService.reserve(nickname);

            responseObserver.onNext(ReserveNicknameResponse.newBuilder()
                .setReservationToken(reservation.getToken())
                .setExpiresAtUnix(reservation.getExpiresAt().getEpochSecond())
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Reservation failed: " + e.getMessage())
                .asRuntimeException());
        }
    }

    private boolean isNicknameUnavailable(String nickname) {
        return userService.existsByUsername(nickname) ||
               reservationService.existsByNickname(nickname);
    }

    @Override
    public void register(RegisterRequest request,
                        StreamObserver<RegisterResponse> responseObserver) {
        try {
            String token = request.getReservationToken();
            PublicKey publicKey = cryptoService.parsePublicKey(request.getPublicKey().toByteArray());

            NicknameReservation reservation = reservationService.validateToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reservation token"));

            userService.registerUser(reservation.getNickname(), publicKey.getEncoded());
            reservationService.removeReservation(token);

            responseObserver.onNext(RegisterResponse.newBuilder()
                .setSuccess(true)
                .build());
            responseObserver.onCompleted();

        } catch (InvalidKeySpecException e) {
            responseObserver.onNext(RegisterResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Invalid public key format")
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onNext(RegisterResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void requestChallenge(ChallengeRequest request,
                                StreamObserver<ChallengeResponse> responseObserver) {
        try {
            String username = request.getUsername();
            UserDTO user = userService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            ChallengeService.Challenge challenge = challengeService.generateChallenge(username);

            responseObserver.onNext(ChallengeResponse.newBuilder()
                .setChallengeId(challenge.id())
                .setChallenge(ByteString.copyFrom(challenge.bytes()))
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Challenge generation failed: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void authentication(AuthenticationRequest request,
                              StreamObserver<AuthenticationResponse> responseObserver) {
        try {
            String username = request.getUsername();
            UserDTO user = userService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            PublicKey publicKey = cryptoService.parsePublicKey(user.getPublicKey());

            challengeService.validateChallenge(
                request.getChallengeId(),
                request.getSignature().toByteArray(),
                publicKey
            );

            String jwt = tokenProvider.generateToken(username);

            responseObserver.onNext(AuthenticationResponse.newBuilder()
                .setSuccess(true)
                .setToken(jwt)
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onNext(AuthenticationResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build());
            responseObserver.onCompleted();
        }
    }
}
