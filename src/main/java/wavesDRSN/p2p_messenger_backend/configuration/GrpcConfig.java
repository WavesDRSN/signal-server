package wavesDRSN.p2p_messenger_backend.configuration;

import io.grpc.netty.NettyServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcConfig {

    @Bean
    public GrpcServerConfigurer keepAliveConfig() {
        return serverBuilder -> {
            if (serverBuilder instanceof NettyServerBuilder) {
                ((NettyServerBuilder) serverBuilder)
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(5, TimeUnit.SECONDS);
            }
        };
    }
}
