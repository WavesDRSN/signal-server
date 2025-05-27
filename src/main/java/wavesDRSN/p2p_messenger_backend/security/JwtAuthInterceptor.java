package wavesDRSN.p2p_messenger_backend.security;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

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

    private static final String AUTH_SERVICE_NAME = gRPC.v1.Authentication.AuthorisationGrpc.SERVICE_NAME; // <-- ПРОВЕРЬТЕ И ИСПРАВЬТЕ ЭТО!

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        String serviceName = MethodDescriptor.extractFullServiceName(fullMethodName);

        log.debug("Intercepting call to method: {}", fullMethodName);

        // --- Пропускаем сервисы, не требующие аутентификации ---
        if (AUTH_SERVICE_NAME.equals(serviceName)) {
            log.debug("Skipping JWT authentication for public service: {}", serviceName);
            return next.startCall(call, headers); // Просто передаем дальше
        }

        // --- Выполняем JWT аутентификацию для остальных сервисов ---
        log.debug("Attempting JWT authentication for protected service: {}", serviceName);
        String token = extractTokenFromMetadata(headers);
        Authentication authentication = null; // Объявим здесь для finally

        try {
            if (token != null && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);

                // Создаем Authentication только с именем пользователя (простой вариант)
                authentication = new UsernamePasswordAuthenticationToken(
                        username,        // principal
                        null,            // credentials
                        new ArrayList<>() // authorities (пусто, если роли не нужны)
                );

                // Устанавливаем аутентификацию в контекст Spring Security
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("User '{}' authenticated successfully via JWT for method: {}", username, fullMethodName);

                // Успешно аутентифицированы, передаем вызов дальше
                return next.startCall(call, headers);

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