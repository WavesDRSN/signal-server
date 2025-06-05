package wavesDRSN.p2p_messenger_backend.security;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays; // For Arrays.asList
import java.util.List;   // For List

@Slf4j
@GrpcGlobalServerInterceptor
@RequiredArgsConstructor
public class JwtAuthInterceptor implements ServerInterceptor {

    private final JwtTokenProvider tokenProvider;
    
    public static final Context.Key<String> USER_ID_CONTEXT_KEY = Context.key("userId");

    // Ключ для извлечения заголовка Authorization
    private static final Metadata.Key<String> AUTHORIZATION_HEADER_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String BEARER_PREFIX = "Bearer ";

    // Service names that do NOT require JWT authentication
    private static final List<String> PUBLIC_SERVICES = Arrays.asList(
            gRPC.v1.Authentication.AuthorisationGrpc.SERVICE_NAME,
            gRPC.v1.Notification.NotificationServiceGrpc.SERVICE_NAME // <--- ADD NotificationService HERE
    );

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        String serviceName = MethodDescriptor.extractFullServiceName(fullMethodName);

        log.debug("Intercepting call to method: {}", fullMethodName);

        // --- Skip JWT authentication for public services ---
        if (PUBLIC_SERVICES.contains(serviceName)) {
            log.debug("Skipping JWT authentication for public service: {}", serviceName);
            // For public services, we still might want to extract user ID if a token IS provided
            // This is optional and depends on whether public services might sometimes benefit
            // from knowing the user if a token is present (e.g., for logging or optional features)
            // If not needed, this block can be removed.
            try {
                String token = extractTokenFromMetadata(headers);
                if (token != null && tokenProvider.validateToken(token)) {
                    String userId = tokenProvider.getUserIdFromToken(token); // Assuming getUserIdFromToken exists
                    if (userId != null && !userId.isEmpty()) {
                        Context ctx = Context.current().withValue(USER_ID_CONTEXT_KEY, userId);
                        return Contexts.interceptCall(ctx, call, headers, next);
                    }
                }
            } catch (Exception e) {
                log.warn("Error processing optional token for public service {}: {}", serviceName, e.getMessage());
                // Don't fail the call, just log, as auth is not strictly required.
            }
            return next.startCall(call, headers); // Proceed without enforcing JWT
        }

        // --- Выполняем JWT аутентификацию для остальных сервисов ---
        log.debug("Attempting JWT authentication for protected service: {}", serviceName);
        String token = extractTokenFromMetadata(headers);

        try {
            if (token != null && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                String userId = tokenProvider.getUserIdFromToken(token); // Assuming getUserIdFromToken exists

                if (userId == null || userId.isEmpty()) {
                    log.warn("User ID not found in token for username '{}'. JWT might be malformed or claim missing.", username);
                    call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT: User ID missing."), new Metadata());
                    return new ServerCall.Listener<ReqT>() {};
                }

                // Создаем Authentication только с именем пользователя (простой вариант)
                Authentication springAuth = new UsernamePasswordAuthenticationToken(
                        username,        // principal
                        null,            // credentials
                        new ArrayList<>() // authorities (пусто, если роли не нужны)
                );
                SecurityContextHolder.getContext().setAuthentication(springAuth);
                log.debug("User '{}' (ID: {}) authenticated successfully via JWT for method: {}", username, userId, fullMethodName);

                // Store userId in gRPC Context for access in service methods
                Context ctx = Context.current().withValue(USER_ID_CONTEXT_KEY, userId);
                return Contexts.interceptCall(ctx, call, headers, next);

            } else {
                // Токен невалиден или отсутствует
                log.warn("JWT validation failed or token missing for protected method: {}", fullMethodName);
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing JWT token"), new Metadata());
                return new ServerCall.Listener<ReqT>() {
                }; // Пустой listener
            }
        } catch (Exception e) {
            // Ловим любые другие ошибки при обработке токена/аутентификации
            log.error("Error during JWT authentication processing: {}", e.getMessage(), e);
            call.close(Status.INTERNAL.withDescription("Authentication processing failed: " + e.getMessage()), new Metadata());
            return new ServerCall.Listener<ReqT>() {
            }; // Пустой listener
        } finally {
            // Очищаем SecurityContextHolder после обработки запроса
            SecurityContextHolder.clearContext();
            log.trace("SecurityContextHolder cleared for method: {}", fullMethodName);
        }
    }

    private String extractTokenFromMetadata(Metadata headers) {
        String authHeader = headers.get(AUTHORIZATION_HEADER_KEY);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        log.trace("Authorization header missing or does not start with Bearer prefix"); // Менее важно, можно trace
        return null;
    }
}