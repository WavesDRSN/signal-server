package wavesDRSN.p2p_messenger_backend;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class P2pMessengerBackendApplication {

	// провайдер реализует криптографические алгоритмы и сервисы, интегрируется через JCA
	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
			System.out.println("BouncyCastle провайдер зарегистрирован");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(P2pMessengerBackendApplication.class, args);
	}

}
