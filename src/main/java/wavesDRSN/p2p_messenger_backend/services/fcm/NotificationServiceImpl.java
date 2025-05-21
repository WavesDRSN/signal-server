package wavesDRSN.p2p_messenger_backend.services.fcm;

import com.google.firebase.messaging.FirebaseMessagingException;
import gRPC.v1.Notification.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    private final FcmService fcmService;

    public NotificationServiceImpl(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @Override
    public void sendNotification(
            NotificationRequest request,
            StreamObserver<NotificationResponse> responseObserver
    ) {
        try {
            String messageId;

            switch (request.getTargetCase()) {
                case FCM_TOKEN:
                    messageId = fcmService.sendToToken(
                        request.getFcmToken(),
                        request.getPayload().getTitle(),
                        request.getPayload().getBody(),
                        request.getPayload().getDataMap()
                    );
                    break;

                case TOPIC:
                    messageId = fcmService.sendToTopic(
                        request.getTopic(),
                        request.getPayload().getTitle(),
                        request.getPayload().getBody(),
                        request.getPayload().getDataMap()
                    );
                    break;

                default:
                    responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("No target specified")
                        .asRuntimeException());
                    return;
            }

            NotificationResponse response = NotificationResponse.newBuilder()
                .setSuccess(true)
                .setMessageId(messageId)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (FirebaseMessagingException e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("FCM error: " + e.getErrorCode())
                .withCause(e)
                .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
    }

}