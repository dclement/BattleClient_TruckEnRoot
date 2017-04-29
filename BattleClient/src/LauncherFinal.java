

import java.io.IOException;
import java.net.URISyntaxException;

public class LauncherFinal {
	
	private static final String ipServer = "192.168.3.150"; // donne le jour de la battle
	private static final long teamId = 10; // a renseigner par votre valeur
	private static final String secret = "7WD2GPF572"; // a renseigner par votre valeur
	private static final int socketNumber = 3000; // variable par partie le jour de la battle
	private static long gameId = 100; // variable par partie le jour de la battle

	public static void main(String[] zero) throws IOException, URISyntaxException, InterruptedException {
		new Client(ipServer, teamId, secret, socketNumber, gameId).run();
	}
}
