

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;


public class ClientRandom implements Runnable {

	enum Dir {
		NORD("N"), SUD("S"), EST("E"), OUEST("O"), JUMP_NORD("JN"), JUMP_SUD("JS"), JUMP_EST("JE"), JUMP_OUEST(
				"JO"), NORD2("N"), SUD2("S"), EST2("E"), OUEST2("O"), NORD3("N"), SUD3("S"), EST3("E"), OUEST3("O");
		String code;
		Dir(String dir) {
			code = dir;
		}
	}
	
	private String ipServer;
	private long teamId;
	private String secret;
	private int socketNumber;
	private long gameId;

	Random rand = new Random();

	

	public ClientRandom(String ipServer, long teamId, String secret, int socketNumber, long gameId) {
		this.ipServer = ipServer;
		this.teamId = teamId;
		this.secret = secret;
		this.socketNumber = socketNumber;
		this.gameId = gameId;
	}

	public void run() {
		System.out.println("Demarrage du client");
		Socket socket = null;
		String message;
		BufferedReader in;
		PrintWriter out;
		try {
			socket = new Socket(ipServer, socketNumber);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			//System.out.println("Envoi de l'incription");
			out.println(secret + "%%inscription::" + gameId + ";" + teamId);
			out.flush();

			do {
				message = in.readLine();
				//System.out.println("Message recu : " + message);
				if (message != null) {
					if (message.equalsIgnoreCase("Inscription OK")) {
						//System.out.println("Je me suis bien inscrit a la battle");
					} else if (message.startsWith("worldstate::")) {
						String[] components = message.substring("worldstate::".length()).split(";", -1);
						int round = Integer.parseInt(components[0]);
						// On joue
						String action = secret + "%%action::" + teamId + ";" + gameId + ";" + round + ";"
								+ computeDirection().code;
						//System.out.println(action);
						out.println(action);
						out.flush();
					} else if (message.equalsIgnoreCase("Inscription KO")) {
						//System.out.println("inscription KO");
					} else if (message.equalsIgnoreCase("game over")) {
						//System.out.println("game over");
						System.exit(0);
					} else if (message.equalsIgnoreCase("action OK")) {
						//System.out.println("Action bien pris en compte");
					}
				}
				//System.out.println("Pour voir la partie : http://" + ipServer + ":8080/?gameId=" + gameId);
			} while (message != null);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Tant pis
				}
			}

		}
	}

	public Dir computeDirection() {
		return Dir.values()[rand.nextInt(Dir.values().length)];
	}

}
