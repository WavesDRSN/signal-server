package wavesDRSN.p2p_messenger_backend.auth;

import com.google.protobuf.ByteString;
import gRPC.v1.Authentication.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.auth.model.*;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.exceptions.UsernameAlreadyExistsException;
import wavesDRSN.p2p_messenger_backend.security.CryptographyServiceImpl;
import wavesDRSN.p2p_messenger_backend.security.JwtTokenProvider;
import wavesDRSN.p2p_messenger_backend.security.JwtAuthInterceptor;
import wavesDRSN.p2p_messenger_backend.services.auth.*;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

@GrpcService
public class AuthServiceImpl extends AuthorisationGrpc.AuthorisationImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

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
    public void updateFcmToken(UpdateTokenRequest request,
                               StreamObserver<UpdateTokenResponse> responseObserver) {

        // USER_ID_CONTEXT_KEY хранит String ID пользователя
        String authenticatedUserId = JwtAuthInterceptor.USER_ID_CONTEXT_KEY.get(Context.current());

        if (authenticatedUserId == null || authenticatedUserId.trim().isEmpty()) {
            log.warn("Attempt to update FCM token without authentication context (userId missing from context).");
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("User must be authenticated to update FCM token.")
                .asRuntimeException());
            return;
        }

        String fcmToken = request.getFcmToken();

        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("Attempt to update FCM token with an empty token for authenticated userId: {}", authenticatedUserId);
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("FCM token must be provided.")
                .asRuntimeException());
            return;
        }

        log.info("Attempting to update FCM token for authenticated userId: {}", authenticatedUserId);
        try {
            // userService.updateFcmToken ожидает String userId
            boolean updated = userService.updateFcmToken(authenticatedUserId, fcmToken);

            if (updated) {
                log.info("Successfully updated FCM token for userId: {}", authenticatedUserId);
                responseObserver.onNext(UpdateTokenResponse.newBuilder().setSuccess(true).build());
            } else {
                log.warn("Failed to update FCM token for userId: {}. UserService indicated no update, user not found, or token conflict.", authenticatedUserId);
                responseObserver.onNext(UpdateTokenResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Failed to update FCM token. User not found, token conflict, or token was already up to date.")
                    .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error updating FCM token for userId: {}. Message: {}", authenticatedUserId, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error while updating FCM token: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
    }

    @Override
    public void reserveNickname(ReserveNicknameRequest request,
                               StreamObserver<ReserveNicknameResponse> responseObserver) {
        String nickname = request.getNickname();
        log.info("Attempting to reserve nickname: {}", nickname);
        try {
            if (nickname == null || nickname.trim().isEmpty()) {
                log.warn("Attempt to reserve an empty or blank nickname.");
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Nickname cannot be empty.")
                    .asRuntimeException());
                return;
            }

            if (isNicknameUnavailable(nickname)) {
                log.info("Nickname reservation failed for '{}': already taken or reserved.", nickname);
                responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Nickname '" + nickname + "' is already taken or reserved.")
                    .asRuntimeException());
                return;
            }

            NicknameReservation reservation = reservationService.reserve(nickname);
            log.info("Nickname '{}' reserved successfully with token '{}', expires at {}",
                     nickname, reservation.getToken(), reservation.getExpiresAt());

            responseObserver.onNext(ReserveNicknameResponse.newBuilder()
                .setReservationToken(reservation.getToken())
                .setExpiresAtUnix(reservation.getExpiresAt().getEpochSecond())
                .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
             log.warn("Nickname reservation failed for '{}': {}", nickname, e.getMessage());
             responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Internal error during nickname reservation for '{}': {}", nickname, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Reservation failed due to an internal error.")
                .withCause(e)
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
        String reservationToken = request.getReservationToken();
        log.info("Attempting to register user with reservation token: {}", reservationToken);
        try {
            if (reservationToken == null || reservationToken.trim().isEmpty()){
                log.warn("Registration attempt with empty reservation token.");
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Reservation token cannot be empty.").asRuntimeException());
                return;
            }
            if (request.getPublicKey().isEmpty()){
                log.warn("Registration attempt with empty public key. Token: {}", reservationToken);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Public key cannot be empty.").asRuntimeException());
                return;
            }

            PublicKey publicKey = cryptoService.parsePublicKey(request.getPublicKey().toByteArray());

            NicknameReservation reservation = reservationService.validateToken(reservationToken)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired reservation token provided for registration: {}", reservationToken);
                    return Status.FAILED_PRECONDITION.withDescription("Invalid or expired reservation token.").asRuntimeException();
                });

            log.info("Reservation token '{}' validated for nickname '{}'", reservationToken, reservation.getNickname());

            if (userService.existsByUsername(reservation.getNickname())) {
                log.warn("Registration failed for nickname '{}' (token '{}'): Nickname became unavailable after reservation.",
                         reservation.getNickname(), reservationToken);
                reservationService.removeReservation(reservationToken);
                responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Nickname '" + reservation.getNickname() + "' has been taken. Please reserve a new one.")
                    .asRuntimeException());
                return;
            }

            UserDTO registeredUser = userService.registerUser(reservation.getNickname(), publicKey.getEncoded());
            log.info("User '{}' (ID: {}) registered successfully.", registeredUser.getUsername(), registeredUser.getId());

            reservationService.removeReservation(reservationToken);
            log.info("Reservation token '{}' removed after successful registration for nickname '{}'.", reservationToken, reservation.getNickname());

            responseObserver.onNext(RegisterResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();

        } catch (InvalidKeySpecException e) {
            log.warn("Registration failed for token '{}' due to invalid public key format: {}", reservationToken, e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Invalid public key format: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());

        } catch (StatusRuntimeException e) {
            log.warn("Registration failed for token '{}' due to gRPC status error: {}", reservationToken, e.getStatus().getDescription(), e);
            responseObserver.onError(e);
        } catch (UsernameAlreadyExistsException e) {
             log.warn("Registration failed for token '{}': {}", reservationToken, e.getMessage());
             responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
        catch (Exception e) {
            log.error("Internal error during registration for token '{}': {}", reservationToken, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Registration failed due to an internal error.")
                .withCause(e)
                .asRuntimeException());
        }
    }

    @Override
    public void requestChallenge(ChallengeRequest request,
                                StreamObserver<ChallengeResponse> responseObserver) {
        String username = request.getUsername();
        log.info("Requesting challenge for username: {}", username);
        try {
            if (username == null || username.trim().isEmpty()){
                log.warn("Challenge request with empty username.");
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Username cannot be empty.").asRuntimeException());
                return;
            }

            UserDTO user = userService.getUserByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Challenge request failed: User not found '{}'", username);
                    return Status.NOT_FOUND.withDescription("User '" + username + "' not found.").asRuntimeException();
                });

            ChallengeService.Challenge challenge = challengeService.generateChallenge(username);
            log.info("Challenge generated for user '{}' (ID: {}), challenge ID: {}", user.getUsername(), user.getId(), challenge.id());

            responseObserver.onNext(ChallengeResponse.newBuilder()
                .setChallengeId(challenge.id())
                .setChallenge(ByteString.copyFrom(challenge.bytes()))
                .build());
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            log.warn("Challenge request failed for user '{}' due to gRPC status error: {}", username, e.getStatus().getDescription(), e);
            responseObserver.onError(e);
        } catch (Exception e) {
            log.error("Internal error during challenge generation for user '{}': {}", username, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Challenge generation failed due to an internal error.")
                .withCause(e)
                .asRuntimeException());
        }
    }

    @Override
    public void authentication(AuthenticationRequest request,
                              StreamObserver<AuthenticationResponse> responseObserver) {
        String username = request.getUsername();
        String challengeId = request.getChallengeId();
        log.info("Attempting authentication for username: {}, challengeId: {}", username, challengeId);

        try {
            if (username == null || username.trim().isEmpty() ||
                challengeId == null || challengeId.trim().isEmpty() ||
                request.getSignature().isEmpty()) {
                log.warn("Authentication attempt with missing parameters. Username present: {}, ChallengeId present: {}, Signature present: {}",
                    username != null && !username.isEmpty(), challengeId != null && !challengeId.isEmpty(), !request.getSignature().isEmpty());
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username, challengeId, and signature must be provided.")
                    .asRuntimeException());
                return;
            }

            UserDTO user = userService.getUserByUsername(username)
                .orElseThrow(() -> {
                     log.warn("Authentication failed: User not found '{}' for challengeId '{}'", username, challengeId);
                    return Status.UNAUTHENTICATED.withDescription("Authentication failed: User not found or invalid credentials.").asRuntimeException();
                });

            PublicKey publicKey = cryptoService.parsePublicKey(user.getPublicKey());

            challengeService.validateChallenge(
                challengeId,
                request.getSignature().toByteArray(),
                publicKey
            );
            log.info("Challenge '{}' validated successfully for user '{}' (ID: {})", challengeId, user.getUsername(), user.getId());

            // Преобразуем long ID пользователя (из UserDTO) в String для JwtTokenProvider и AuthenticationResponse
            String userIdAsString = String.valueOf(user.getId()); // user.getId() возвращает long

            // JwtTokenProvider.generateToken теперь ожидает (String username, String userId)
            String jwt = tokenProvider.generateToken(user.getUsername(), userIdAsString);
            log.info("JWT token generated for user '{}' (ID: {})", user.getUsername(), userIdAsString);

            responseObserver.onNext(AuthenticationResponse.newBuilder()
                .setSuccess(true)
                .setToken(jwt)
                // AuthenticationResponse.setUserId ожидает String
                .setUserId(userIdAsString)
                .build());
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
             log.warn("Authentication failed for user '{}', challengeId '{}': {}", username, challengeId, e.getStatus().getDescription(), e);
             responseObserver.onError(e);
        } catch (InvalidKeySpecException | IllegalArgumentException e) {
            log.warn("Authentication failed for user '{}', challengeId '{}' due to validation/key error: {}", username, challengeId, e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Authentication failed: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
        catch (Exception e) {
            log.error("Internal error during authentication for user '{}', challengeId '{}': {}", username, challengeId, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Authentication failed due to an internal server error.")
                .withCause(e)
                .asRuntimeException());
        }
    }
}
