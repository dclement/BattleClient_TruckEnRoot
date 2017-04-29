

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;

public class LauncherTest {
	private static final String ipServer = "192.168.3.184"; // a remplacer par l'adresse fournie dans le mail recu

	private static final long realteamId = 10; // mettre votre teamId envoyé dans le mail
	private static final String secret ="7WD2GPF572"; // mettre votre secret
	private static final long maxteamId = realteamId + 6; // ne pas toucher
	private static final int socketNumber = 2010; // mettre le port envoyé dans le mail

	private static long gameId;

	enum Dir {
		NORD("N"),SUD("S"),EST("E"),OUEST("O"),
        JUMP_NORD("JN"),JUMP_SUD("JS"),JUMP_EST("JE"),JUMP_OUEST("JO"),
        NORD2("N"),SUD2("S"),EST2("E"),OUEST2("O"),
        NORD3("N"),SUD3("S"),EST3("E"),OUEST3("O");

		String code;
		Dir(String dir) {
			code = dir;
		}
	}

	public static void main(String[] zero) throws IOException, URISyntaxException, InterruptedException {
		System.out.println("Demarrage du client de test");

		//Creation la partie
		HttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder()
				.setScheme("http")
				.setHost(ipServer+":8080/test")
				.setPath("/createBattle")
				.setParameter("teamId", Long.toString(realteamId))
				.setParameter("secret", secret)
				.build();

		System.out.println("uri " + uri);
		HttpGet httpget = new HttpGet(uri);
		
		System.out.println("http");
		ResponseHandler<String> handler = new BasicResponseHandler();

		gameId = Long.parseLong(httpclient.execute(httpget, handler));
		System.out.println(gameId);

		// Creation des joueurs et leur enregistrement a la partie
		// notre joueur
		new Thread(new Client(ipServer, realteamId, secret, socketNumber, gameId)).start();
		// les 5 autres IA random
		for (long i = realteamId+1; i < maxteamId; i++) {
			new Thread(new Client(ipServer, i, secret, socketNumber, gameId)).start();
		}

		// On attend une seconde pour être ser que les threads ont bien demarre
		Thread.sleep(1000);

		// Demarrage de la game
		// http://xxxxxx:8080/test/startBattle?gameId=xxxx&teamId=10&secret=bobsecret
		URI startUri = new URIBuilder()
				.setScheme("http")
				.setHost(ipServer+":8080")
				.setPath("/test/startBattle")
				.setParameter("gameId", Long.toString(gameId))
				.setParameter("teamId", Long.toString(realteamId))
				.setParameter("secret", secret)
				.build();

		HttpGet startGet = new HttpGet(startUri);
		ResponseHandler<String> handler2 = new BasicResponseHandler();
		httpclient.execute(startGet, handler2);

		// Pour voir le jeu
		// http://xxxxxx:8080/?gameId=votre game Id
	}
}

