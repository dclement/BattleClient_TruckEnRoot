

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang3.time.StopWatch;


public class Client implements Runnable {

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

	
	private boolean etat;
	public long teamIdTackle; //ID de l'équipe immunisée contre nous
	private boolean hasLogo;
	private int nbJumpleAvailable;
	
	public Point lastLogoWanted;
	
	private HashMap<Long, Concurrent> lConcurrent = new HashMap<Long, Concurrent>(5);
	
	private ArrayList<Point> lLogo = new ArrayList<Point>();
	
	private HashMap<Long, Point> lCaddies = new HashMap<Long, Point>();
	
	private int nbTours = 51;
	
	public static long timeToResponse = 1000;

	private boolean initDone = false;
	
	boolean Terrain[][];
	
	public Client(String ipServer, long teamId, String secret, int socketNumber, long gameId) {
		this.ipServer = ipServer;
		this.teamId = teamId;
		this.secret = secret;
		this.socketNumber = socketNumber;
		this.gameId = gameId;
		this.teamIdTackle = -1;
		setEtat(true);
		setHasLogo(false);
		setNbJumpleAvailable(3);

	}

	public void run() {
		System.out.println("Demarrage du client");
		Socket socket = null;
		String message;
		BufferedReader in;
		PrintWriter out;

		// Initialisation du Terrain
		Terrain = new boolean[16][13];
		for( int i = 0; i< Terrain.length; ++i){
			   for( int j = 0; j<Terrain[i].length; ++j){
				 Terrain[i][j] = true;
			   }
		}
		
		try {
			socket = new Socket(ipServer, socketNumber);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			System.out.println("Envoi de l'incription");
			out.println(secret + "%%inscription::" + gameId + ";" + teamId);
			out.flush();

			do {
				StopWatch sw = new StopWatch();
				sw.start();
				
				// Clean de la position des logos
				lLogo.clear();
				// Clean position des joueurs
				//lConcurrent.clear();
				
				message = in.readLine();
				System.out.println("Message recu : " + message);
				if (message != null) {
					if (message.equalsIgnoreCase("Inscription OK")) {
						System.out.println("Je me suis bien inscrit a la battle");
					} else if (message.startsWith("worldstate::")) {
						
//Compute Worldstate----------------------------------------------------------------------------------------						
						// get Round
						String[] components = message.substring("worldstate::".length()).split(";", -1);
						int round = Integer.parseInt(components[0]);
						System.out.println("components 1 : " + components[1]);
						System.out.println("components 2 : " + components[2]);
						System.out.println("components 3 : " + components[3]);
						
						// get position des caddies -- 1 unique MAJ
						if(lCaddies.isEmpty())
						{
							System.out.println("Pour voir la partie : http://" + ipServer + ":8080/?gameId=" + gameId);
							String[] listCaddies = components[3].split(":");
							for (String splitteur : listCaddies) {
								String[] infoCaddie = splitteur.split(",");
								Long idJCaddie = Long.valueOf(infoCaddie[0]);
								int posXC = Integer.valueOf(infoCaddie[1]);
								int posYC = Integer.valueOf(infoCaddie[2]);
								lCaddies.put(idJCaddie, new Point(posXC, posYC));
							}
						}
						
						// get liste logo
						String[] listLogo = components[2].split(":");
					
						for (String PosxPosY : listLogo) 
						{
							String[] logoPosition = PosxPosY.split(",");
							lLogo.add(new Point(Integer.valueOf(logoPosition[0]), Integer.valueOf(logoPosition[1])));
						}
						
						
						// get liste joueurs
						String[] listJoueur = components[1].split(":");
						for (String splitteurJ : listJoueur) {
							String[] infoJoueur = splitteurJ.split(",");
							long idJ = Long.valueOf(infoJoueur[0]);
							int posXJ = Integer.valueOf(infoJoueur[1]);
							int posYJ = Integer.valueOf(infoJoueur[2]);
							int scoreJ = Integer.valueOf(infoJoueur[3]);
							String etatJ = infoJoueur[4];
			
							// Initialisation info concurrents
							if(!initDone)
							{
								System.out.println("Init Joueur : " + idJ);
								lConcurrent.put(idJ, new Concurrent(3, true, false, new Point(posXJ,posYJ), new Point(posXJ,posYJ), new Point(lCaddies.get(idJ).x, lCaddies.get(idJ).y), idJ, etatJ, scoreJ));
								Terrain[posXJ][posXJ] = false; 
							}
							else
							{
								// MAj des infos de positions / statuts
								//System.out.println("Maj Joueur : " + idJ);
								Concurrent currentConcurrent = lConcurrent.get(idJ);
								//System.out.println("Maj Joueur Concurrent : " + currentConcurrent);
								currentConcurrent.setEtat(etatJ);	
								currentConcurrent.setScore(scoreJ);
								// Position précédente = position actuelle avant actualisation
								Terrain[currentConcurrent.getCurrentPoint().x][currentConcurrent.getCurrentPoint().y] = true;
								currentConcurrent.setLastPoint(currentConcurrent.getCurrentPoint());
								currentConcurrent.setCurrentPoint(new Point(posXJ, posYJ));
								currentConcurrent.setEtat(etatJ);
								currentConcurrent.setScore(scoreJ);
								
								Terrain[posXJ][posYJ] = false;
								
								// IsSafePlace ?
								if(currentConcurrent.getCaddyePoint().equals(currentConcurrent.getCurrentPoint()))
								{
									currentConcurrent.setSafeAtHome(true);
								}
								else
								{
									currentConcurrent.setSafeAtHome(false);
								}
								
								// Jump used ?
								int moveX = currentConcurrent.getCurrentPoint().x - currentConcurrent.getLastPoint().x;
								int moveY = currentConcurrent.getCurrentPoint().y - currentConcurrent.getLastPoint().y;
								if(moveX == 2 || moveX == -2 || moveY == 2 || moveY == -2)
								{
									currentConcurrent.setJumpAvailable(currentConcurrent.getJumpAvailable()-1);
								}
								
								// Reset has logo
								currentConcurrent.setHasLogo(false);
								
								// HasLogo ?
								for (Point PLogo : lLogo) {
									if(currentConcurrent.getCurrentPoint().equals(PLogo.getLocation()))
									{
										currentConcurrent.setHasLogo(true);
									}
								}
								
								
								
							}
						
						}
						
						// end for - tous concurrents créés ou maj
						initDone = true;
						
// Décision----------------------------------------------------------------------------------------						
						Concurrent ourTeam = lConcurrent.get(teamId);
						
						// Peut-on jouer ce tour-ci ?
						if(ourTeam.getEtat().equalsIgnoreCase("playing"))
						{
							// retour de toutes les directions possibles
							List<Client.Dir> lDispoDirection = DirectionAvailable(ourTeam.getCurrentPoint(), ourTeam.getJumpAvailable());
							System.out.println("Liste des directions dispo " + lDispoDirection.size());
							Dir actionD;
							
							// 1 - si on a un logo, retour au caddie si le nb de tour restant est suffisant
							// sinon on frappe !
							if(ourTeam.isHasLogo())
							{
								Utilitaire UtilCaddie = getUtilCaddie(lCaddies.get(teamId).getLocation(), ourTeam.getCurrentPoint());
								if ( nbTours - round < UtilCaddie.getMouvement() )
								{
									// Go tapper
									actionD = goTOConcurrent(lConcurrent, ourTeam.getCurrentPoint(), lDispoDirection, teamIdTackle);
								}
								else
								{
									// On ramene le précieux
									actionD = goBackCaddy(lCaddies.get(teamId).getLocation(),ourTeam.getCurrentPoint(), lDispoDirection, lConcurrent);
								}
								
							}
							
							// 2 - si on n'a pas de logo, on va en chercher un si possible
							// sinon on frappe !
							else
							{
								int nbLogos = nbLogoAvailable(lLogo, lCaddies);
								Utilitaire UtilLogo = findBestLogo(lLogo,ourTeam);			
								if(nbTours - round < UtilLogo.getMouvement())
								{
									// Go tapper
									actionD = goTOConcurrent(lConcurrent, ourTeam.getCurrentPoint(), lDispoDirection, teamIdTackle);
								}
								else
								{
									// On va chercher un logo
									actionD = goTOLogo(UtilLogo, lLogo, ourTeam.getCurrentPoint(), lDispoDirection, lConcurrent);
								}								
							}
							System.out.println("Action choisie " + actionD);
							
							// maj de l'équipe tacklée si 1 et 1 seule
						    long tTackle = setTeamIdTackle(ourTeam.getCurrentPoint(), actionD, lConcurrent);
						    System.out.println("Concurrent tackle " + tTackle);
						    
						    teamIdTackle = tTackle;
						    
							sw.stop();
							System.out.println("End time " + sw.getTime());
							
							
							// On joue
	//						String action = secret + "%%action::" + teamId + ";" + gameId + ";" + round + ";"
	//								+ computeDirection().code;
							String action = secret + "%%action::" + teamId + ";" + gameId + ";" + round + ";"
									+ actionD.code;
							
							System.out.println(action);
							out.println(action);
							out.flush();
						}
					} else if (message.equalsIgnoreCase("Inscription KO")) {
						System.out.println("inscription KO");
					} else if (message.equalsIgnoreCase("game over")) {
						System.out.println("game over");
						System.exit(0);
					} else if (message.equalsIgnoreCase("action OK")) {
						System.out.println("Action bien prise en compte");
					}
				}
				
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
	
	/**
	 * Fonction qui permet de trouver le logo le plus proche
	 * @param lLogo
	 * @param ourPosition
	 * @return position de logo à viser
	 */
	public Utilitaire findBestLogo(ArrayList<Point> lLogo, Concurrent ourTeam) {
		Point ourPosition = ourTeam.getCurrentPoint();
		Point ourCaddie = ourTeam.getCaddyePoint();
		
		//logo le plus proche de nous
		Utilitaire closerLOGO = findCloserPoint(lLogo, ourPosition);
		
		//logo le plus proche du caddie
		Utilitaire closerLOGOCaddie = findCloserPoint(lLogo, ourCaddie);
		
		//si le logo le plus proche du caddie n'est pas le plus proche de nous, mais qu'il n'y a personne autour 
		//et qu'il n'est pas porté, alors on le choisi
		if ( !closerLOGOCaddie.equals(closerLOGO) 
				&& isLogoWithConcurrent(closerLOGOCaddie.getP(), getlConcurrent()) == null
				&& nbConcurrentWithoutLogoAroundPoint(closerLOGOCaddie.getP(), getlConcurrent(), closerLOGOCaddie.getMouvement()) == 0) {
			return closerLOGOCaddie;
		}
		
		//sinon on analyse le plus proche de nous		
		System.out.println("teamIdTackle : " + teamIdTackle);
		// si le logo est porté
		Concurrent concurrentWithLogo = isLogoWithConcurrent(closerLOGO.getP(), getlConcurrent());
		if (concurrentWithLogo != null ) {
			System.out.println(concurrentWithLogo.getIdTeam());
		} else {
			System.out.println("le logo que je vise n'est pas porté");
		}
		
		if ( concurrentWithLogo != null && teamIdTackle != concurrentWithLogo.getIdTeam()) {
			// on veut vérifier si on peut le tackler avant qu'il soit à son caddie 
			
			// calcul du nombre de mvt entre le concurrent et son caddie
			double concurrentCaddie = distanceMoves(concurrentWithLogo.getCurrentPoint(), concurrentWithLogo.getCaddyePoint());
			// calcul du nombre de mvt entre nous et son caddie
			double nousCaddie = distanceMoves(ourPosition, concurrentWithLogo.getCaddyePoint());
			// calcul du nombre de mvt entre nous et le concurrent
			double nousConcurrent = distanceMoves(ourPosition, concurrentWithLogo.getCurrentPoint());
			// si on peut intercepter : OK
			if ( nousCaddie > concurrentCaddie || nousConcurrent > concurrentCaddie ) {
				// on va boucler tant qu'on n'a pas trouvé un bon logo à cibler
				ArrayList<Point> lLogoUpdated = lLogo;
				Boolean foundOK = false;
				Point pointToRemove = closerLOGO.getP();
				while ( !foundOK && lLogoUpdated.size() > 1 ) {
					lLogoUpdated.remove(pointToRemove);
					Utilitaire newCloser = findCloserPoint(lLogoUpdated, ourPosition);
					concurrentWithLogo = isLogoWithConcurrent(newCloser.getP(), getlConcurrent());
					if ( concurrentWithLogo != null && teamIdTackle != concurrentWithLogo.getIdTeam()) {
						double concurrentCaddie2 = distanceMoves(concurrentWithLogo.getCurrentPoint(), concurrentWithLogo.getCaddyePoint());
						double nousCaddie2 = distanceMoves(ourPosition, concurrentWithLogo.getCaddyePoint());
						double nousConcurrent2 = distanceMoves(ourPosition, concurrentWithLogo.getCurrentPoint());
						if ( nousCaddie2 < concurrentCaddie2 && nousConcurrent2 < concurrentCaddie2 ) {
							foundOK = true;
							closerLOGO = newCloser;
						} else {
							pointToRemove = newCloser.getP();
						}
					} else {
						foundOK = true;
						closerLOGO = newCloser;
					}
				}
				
			}
			// else on garde cette cible
		} else if (concurrentWithLogo != null && teamIdTackle == concurrentWithLogo.getIdTeam()) {
			//on change de logo
			// on va boucler tant qu'on n'a pas trouvé un bon logo à cibler
			ArrayList<Point> lLogoUpdated = lLogo;
			Boolean foundOK = false;
			Point pointToRemove = closerLOGO.getP();
			while ( !foundOK && lLogoUpdated.size() > 1 ) {
				lLogoUpdated.remove(pointToRemove);
				Utilitaire newCloser = findCloserPoint(lLogoUpdated, ourPosition);
				concurrentWithLogo = isLogoWithConcurrent(newCloser.getP(), getlConcurrent());
				if ( concurrentWithLogo != null && teamIdTackle != concurrentWithLogo.getIdTeam()) {
					double concurrentCaddie2 = distanceMoves(concurrentWithLogo.getCurrentPoint(), concurrentWithLogo.getCaddyePoint());
					double nousCaddie2 = distanceMoves(ourPosition, concurrentWithLogo.getCaddyePoint());
					double nousConcurrent2 = distanceMoves(ourPosition, concurrentWithLogo.getCurrentPoint());
					if ( nousCaddie2 < concurrentCaddie2 && nousConcurrent2 < concurrentCaddie2 ) {
						foundOK = true;
						closerLOGO = newCloser;
					} else {
						pointToRemove = newCloser.getP();
					}
				} else {
					foundOK = true;
					closerLOGO = newCloser;
				}
			}
		}
		// else: on garde cette cible
		
		// Logo choisi
		System.out.println("Logo choisi : " + closerLOGO.getP().x + " - " + closerLOGO.getP().y + " en " + closerLOGO.getMouvement() + " déplacements ");
		System.out.println("Ma position: " + ourPosition.x + " - " + ourPosition.y);
		return closerLOGO;
	}
	
	/**
	 * 
	 * @param p
	 * @param getlConcurrent
	 * @param radius
	 * @return
	 */
	private int nbConcurrentWithoutLogoAroundPoint(Point p, HashMap<Long, Concurrent> lConcurrent, double radius) {
		int nbConcurrent = 0;
		
		for (Entry<Long, Concurrent> concurrent : lConcurrent.entrySet()) {
			Point concurrentPos = concurrent.getValue().getCurrentPoint();
			if (!concurrent.getValue().isHasLogo() && concurrentPos.x > p.x-radius && concurrentPos.x < p.x+radius && concurrentPos.y > p.y-radius && concurrentPos.y < p.y+radius) {
				nbConcurrent++;
			}
		}
		return nbConcurrent;
	}

	/**
	 * Trouve le point le plus proche dans une liste de points
	 * @param lPoint
	 * @param ourPosition
	 * @return
	 */
	public Utilitaire findCloserPoint(ArrayList<Point> lPoint, Point ourPosition) {
		Point closer = new Point();
		double nbMoves = 400;
		
		// calcul du point le plus proche
		for (Point point : lPoint) {
			double tmp = distanceMoves(ourPosition, point);
			if( nbMoves > tmp ) 
			{
				nbMoves = tmp;
				closer = point;
			}
		}
		
		return new Utilitaire(closer, nbMoves);
	}
	
	
	/**
	 * Pour savoir si un concurrent est présent sur un point donné
	 * @param point
	 * @param lConcurrent
	 * @return concurrent
	 */
	public Concurrent isLogoWithConcurrent(Point point, HashMap<Long, Concurrent> lConcurrent) {
		// si le logo est un concurrent
		for (Entry<Long, Concurrent> concurrent : lConcurrent.entrySet()) {
			if (concurrent.getValue().getCurrentPoint().equals(point)) {
				return concurrent.getValue();
			}
		}
		System.out.println("inside isLogoWithConcurrent: null");
		return null;
	}
	
	/**
	 * Fonction qui permet de trouver notre caddy
	 * @param lLogo
	 * @param ourPosition
	 * @return nb mouvement pour le retour
	 */
	public Utilitaire getUtilCaddie(Point caddie, Point ourPosition) {
		Point closerCaddie = new Point();
		
		double nbMoves = distanceMoves(ourPosition, caddie);
		closerCaddie = caddie;
	
		return new Utilitaire(closerCaddie, nbMoves);
	}

	
	/**
	 * Fonction qui cherche le joueur le plus proche a tackler
	 * @param lLogo
	 * @param ourPosition
	 * @return Point. Ne peut pas être nul car on a 5 concurrent et un seul peut être immunisé
	 */
	public Point findCloserConcurrentToBeat(HashMap<Long, Concurrent> lConcurrent, Point ourPosition, long teamIdTackle) {
		Point closerconcurrent = new Point();
		double nbMoves = 400;

		// Calcul du concurrent le plus proche hors nous et qui n'est pas immunisé
		for (Entry<Long, Concurrent> ListConcurrent : lConcurrent.entrySet()) {
			// si le concurrent n'est pas nous et qu'il n'est pas immunisé contre nous
			if ( ListConcurrent.getKey().compareTo(teamId) != 0 && ListConcurrent.getKey().compareTo(teamIdTackle) != 0 ) {
				double tmp = distanceMoves(ourPosition, ListConcurrent.getValue().getCurrentPoint());
				if( nbMoves > tmp ) 
				{
					nbMoves = tmp;
					closerconcurrent = ListConcurrent.getValue().getCurrentPoint();
				}
			}
		}
		
		// Concurrent choisi
		System.out.println("closerconcurrent choisi : " + closerconcurrent.x + " - " + closerconcurrent.y + " en " + nbMoves + " déplacements ");
		return closerconcurrent;
	}
	
	
	/***
	 * lLogo: list of logo available
	 * ourPosition: the current position of the team 
	 * @return Dir direction to be sent to server
	 */
	public Dir goTOLogo(Utilitaire Util, ArrayList<Point> lLogo, Point ourPosition, List<Dir> lDirectionDispo, HashMap<Long, Concurrent> lConcurrent){
		
		// déterminer le chemin à prendre ------------------------------------
		int deltaX = Util.getP().x - ourPosition.x;
		int deltaY = Util.getP().y - ourPosition.y;
		
		return chooseAction (deltaX, deltaY, lDirectionDispo, ourPosition, lConcurrent, false);
	}
	
	
	 /**
	  * 
	  * @param ourCaddy
	  * @param ourPosition
	  * @param lDirectionDispo
	  * @param lConcurrent
	  * @return
	  */
	public Dir goBackCaddy(Point ourCaddy, Point ourPosition, List<Client.Dir> lDirectionDispo, HashMap<Long, Concurrent> lConcurrent){
		System.out.println("Notre position: " + ourPosition.x + " - " + ourPosition.y);
		System.out.println("Notre caddie: " + ourCaddy.x + " - " + ourCaddy.y);

		// Direction choisie
		int deltaX = ourCaddy.x - ourPosition.x;
		int deltaY = ourCaddy.y - ourPosition.y;		

		// choisi la direction
		return chooseAction (deltaX, deltaY, lDirectionDispo, ourPosition, lConcurrent, true);
	}

	/**
	 * Analyse in a deeper way the direction to choose
	 * @param DirectionAvailable 
	 * @param deltaX
	 * @param deltaY
	 * @param lDirectionDispo
	 * @param withJump 
	 * @return
	 */
	public Dir chooseAction (int deltaX, int deltaY, List<Client.Dir> lDirectionDispo, Point ourPosition, HashMap<Long, Concurrent> lConcurrent, boolean withJump) {
		List<Client.Dir> DirectionAvailable = new ArrayList<Client.Dir>();
		List<Client.Dir> DirectionSouhaite = new ArrayList<Client.Dir>();
		
		//mise à jour du boolean withJump pour vérifier si nos jump sont possibles
		withJump = withJump && (lConcurrent.get(teamId).getJumpAvailable() > 0);
		
		// Ajout de la direction souhaitée
		if ( deltaX > 0)
		{
			DirectionSouhaite.add(Dir.EST);
		}
		else if ( deltaX < 0)
		{
			DirectionSouhaite.add(Dir.OUEST);
		}
		
		if( deltaY > 0)
		{
			DirectionSouhaite.add(Dir.SUD);
		}
		else if ( deltaY < 0)
		{
			DirectionSouhaite.add(Dir.NORD);
		}
		
		// Ajout de la direction possible
		if ( deltaX > 0 && lDirectionDispo.contains(Dir.EST) )
		{
			DirectionAvailable.add(Dir.EST);
			
		}
		else if ( deltaX < 0 && lDirectionDispo.contains(Dir.OUEST))
		{
			DirectionAvailable.add(Dir.OUEST);
		}
		
		if( deltaY > 0  && lDirectionDispo.contains(Dir.SUD) )
		{
			DirectionAvailable.add(Dir.SUD);
		}
		else if ( deltaY < 0 && lDirectionDispo.contains(Dir.NORD) )
		{
			DirectionAvailable.add(Dir.NORD);
		}
		
		if (withJump) {
			if ( deltaX > 1 && lDirectionDispo.contains(Dir.JUMP_EST) )
			{
				DirectionAvailable.add(Dir.JUMP_EST);
				
			}
			else if ( deltaX < -1 && lDirectionDispo.contains(Dir.JUMP_OUEST))
			{
				DirectionAvailable.add(Dir.JUMP_OUEST);
			}
			
			if( deltaY > 1  && lDirectionDispo.contains(Dir.JUMP_SUD) )
			{
				DirectionAvailable.add(Dir.JUMP_SUD);
			}
			else if ( deltaY < -1 && lDirectionDispo.contains(Dir.JUMP_NORD) )
			{
				DirectionAvailable.add(Dir.JUMP_NORD);
			}
		}
				
		Dir action = null;

		// si aucune action n'est possible parmi nos directions d'objectif
		// on effectue l'action qui se rapproche de notre objectif
		if ( DirectionAvailable.size() == 0 ) {
			action = bestActionDispo(lDirectionDispo, DirectionSouhaite, ourPosition, lConcurrent);
		}
		else if ( DirectionAvailable.size() == 1 ) {
			//si une seule action possible
			action = DirectionAvailable.get(0);
		} else {	
			// si plusieurs possibilités, on analyse + finement. Par défaut action = la premiere de la liste
			action = DirectionAvailable.get(0);
			
			// 1- choix de la direction permettant de baffer le + (banzaï !)

			int nbTackle = 0;
			for (Client.Dir dir: DirectionAvailable) {
				//System.out.println("chooseAction: ourPosition=("+ourPosition.x+";"+ourPosition.y+") - deltaX:" + deltaX+ " deltaY:"+deltaY+ " direction:"+dir.code);
				if ( isDirHorizontal(dir) ) {
					if (isDirJump(dir)) {
						System.out.println("nombreTeamTackle: action="+dir.code+" ourPosition:"+ourPosition.x+"-"+ourPosition.y+" ajout:"+2*deltaX/Math.abs(deltaX));
						if (nbTackle < nombreTeamTackle(new Point(ourPosition.x + 2*deltaX/Math.abs(deltaX), ourPosition.y), lConcurrent)) {
							action = dir;
							nbTackle = nombreTeamTackle(new Point(ourPosition.x + 2*deltaX/Math.abs(deltaX), ourPosition.y), lConcurrent);
						}
					} else {
						System.out.println("nombreTeamTackle: action="+dir.code+" ourPosition:"+ourPosition.x+"-"+ourPosition.y+" ajout:"+deltaX/Math.abs(deltaX));
						if (nbTackle < nombreTeamTackle(new Point(ourPosition.x + deltaX/Math.abs(deltaX), ourPosition.y), lConcurrent)) {
							action = dir;
							nbTackle = nombreTeamTackle(new Point(ourPosition.x + deltaX/Math.abs(deltaX), ourPosition.y), lConcurrent);
						}
					}
					
				}
				else {
					if (isDirJump(dir)) {
						System.out.println("nombreTeamTackle: action="+dir.code+" ourPosition:"+ourPosition.x+"-"+ourPosition.y+" ajout:"+2*deltaY/Math.abs(deltaY));
						if (nbTackle < nombreTeamTackle(new Point(ourPosition.x, ourPosition.y + 2*deltaY/Math.abs(deltaY)), lConcurrent)) {
							action = dir;
							nbTackle = nombreTeamTackle(new Point(ourPosition.x, ourPosition.y + 2*deltaY/Math.abs(deltaY)), lConcurrent);
						}
					} else {
						System.out.println("nombreTeamTackle: action="+dir.code+" ourPosition:"+ourPosition.x+"-"+ourPosition.y+" ajout:"+deltaY/Math.abs(deltaY));
						if (nbTackle < nombreTeamTackle(new Point(ourPosition.x, ourPosition.y + deltaY/Math.abs(deltaY)), lConcurrent)) {
							action = dir;
							nbTackle = nombreTeamTackle(new Point(ourPosition.x, ourPosition.y + deltaY/Math.abs(deltaY)), lConcurrent);
						}
					}
				}
			} // end for calcul du nombre de Tackles
			
			// si aucun mouvement ne permet de donner des baffes
			if ( nbTackle == 0 ) {
				//2 - si aucune baffe donnable on cherche à se protéger = on s'éloigne des autres (sauve qui peut!)
				List<Dir> safeDir = safePlaces(ourPosition, lDirectionDispo, lConcurrent);
				for (Dir dir : DirectionAvailable) {
					if (safeDir.contains(dir)) {
						if (!isDirJump(dir)) {
							action = dir;
						}
					}
				}
			}
		}

		return action;
	}
	
	/**
	 * 
	 * @param ourPosition
	 * @param lDirectionDispo
	 * @param lConcurrent
	 * @return
	 */
	public List<Dir> safePlaces(Point ourPosition, List<Dir> lDirectionDispo, HashMap<Long, Concurrent> lConcurrent) {	
		List<Dir> safeDir = new ArrayList<Dir>(lDirectionDispo);
		
		for (Dir dir : lDirectionDispo) {
			Point futurPosition = calculPositionFutur(ourPosition, dir);
			List<Point> adjFuturPosition = adjacentPoints(futurPosition); //les positions des cases bafeurs
			for (Entry<Long, Concurrent> concurrent : lConcurrent.entrySet()) {
				//si pas nous !
				if ( concurrent.getKey() != teamId) {
					//TODO rajouter cas de test si on est immunisé contre le coco
					
					List<Dir> concurrentDirectionAvailable = DirectionAvailable(concurrent.getValue().getCurrentPoint(), concurrent.getValue().getJumpAvailable());
					for (Dir dir2 : concurrentDirectionAvailable) {
						if(adjFuturPosition.contains(calculPositionFutur(concurrent.getValue().getCurrentPoint(), dir2))) {
							safeDir.remove(dir);
						}
					}
				}
			}
		}
		return safeDir;
	}
	
	/**
	 * Retourne la liste des 4 points adjacents dans le tableau !!!!
	 * @param point
	 * @return
	 */
	public List<Point> adjacentPoints(Point point) {
		List<Point> adj = new ArrayList<Point>();
		if (point.x+1<16)
			adj.add(new Point(point.x+1, point.y));
		
		if (point.x-1>=0)
			adj.add(new Point(point.x-1, point.y));
		
		if (point.y+1<13)
			adj.add(new Point(point.x, point.y+1));
		
		if (point.y-1>=0)
			adj.add(new Point(point.x, point.y-1));
		return adj;
	}
	
	/**
	 * 
	 * @param dir
	 * @return
	 */
	public Boolean isDirJump(Client.Dir dir) {
		Boolean isJump = false;
		switch (dir) {
		case JUMP_EST:
			isJump = true;
			break;
		case JUMP_OUEST:
			isJump = true;
			break;
		case JUMP_NORD:
			isJump = true;
			break;
		case JUMP_SUD:
			isJump = true;
			break;
		default:
			break;
		}
		return isJump;
	}
	
	/**
	 * 
	 * @param dir
	 * @return
	 */
	public Boolean isDirHorizontal (Client.Dir dir) {
		Boolean isHorizontal = false;
		switch (dir) {
		case EST:
			isHorizontal = true;
			break;
		case JUMP_EST:
			isHorizontal= true;
			break;
		case OUEST:
			isHorizontal = true;
			break;
		case JUMP_OUEST:
			isHorizontal = true;
			break;
		default:
			isHorizontal = false;
			break;
		}
		return isHorizontal;
	}

	/**
	 * Calcul de la 2eme meilleure action disponible
	 * @param lDirectionDispo
	 * @param lDirectionSouhaite
	 * @return
	 */
	public Dir bestActionDispo(List<Client.Dir> lDirectionDispo, List<Client.Dir> lDirectionSouhaite, Point ourPosition, HashMap<Long, Concurrent> concurrent)
	{
		Dir dirChoisie = null;
		
		// Point possible après déplacement
		Point PositionE = calculPositionFutur(ourPosition, Dir.EST);
		Point PositionO = calculPositionFutur(ourPosition, Dir.OUEST);
		Point PositionS = calculPositionFutur(ourPosition, Dir.SUD);
		Point PositionN = calculPositionFutur(ourPosition, Dir.NORD);
		Point PositionJE = calculPositionFutur(ourPosition, Dir.JUMP_EST);
		Point PositionJO = calculPositionFutur(ourPosition, Dir.JUMP_OUEST);
		Point PositionJS = calculPositionFutur(ourPosition, Dir.JUMP_SUD);
		Point PositionJN = calculPositionFutur(ourPosition, Dir.JUMP_NORD);
		
		
		Concurrent nosInfo = lConcurrent.get(teamId);
		
		if(!lDirectionSouhaite.isEmpty())
		{
			// On choisi dans l'ordre de priorité
			// 1- Un jump dans la meme direction 
			// 2- Une direction perpendiculaire à celle souhaitée +1
			// 3- Une direction perpendiculaire à celle souhaitée avec jump +2
			// 4- La direction opposée à celle souhaité +1
			// 5- La direction opposée à celle souhaité +2
			switch (lDirectionDispo.get(rand.nextInt(lDirectionSouhaite.size())).code) {
			case "S":
				// Test 1
				// case valide et encore au moins jump dispo ?
				if(isCaseAvailable(PositionJS) && nosInfo.getJumpAvailable()>0)
				{
					dirChoisie = Dir.JUMP_SUD;
				}
				// Test 2
				else if(isCaseAvailable(PositionE) || isCaseAvailable(PositionO))
					{
						if(isCaseAvailable(PositionE))
						{
							dirChoisie = Dir.EST;
						}
						else
						{
							dirChoisie = Dir.OUEST;
						}
					}
				// Test 3
					else if(isCaseAvailable(PositionJE) || isCaseAvailable(PositionJO))
						{
							if(isCaseAvailable(PositionJE))
							{
								dirChoisie = Dir.JUMP_EST;
							}
							else
							{
								dirChoisie = Dir.JUMP_OUEST;
							}
						}
				// Test 4
						else if(isCaseAvailable(PositionN))
						{
							dirChoisie = Dir.NORD;
						}
				// Test 5
						else if(isCaseAvailable(PositionJN))
						{
							dirChoisie = Dir.JUMP_NORD;
						}
						else
						{
							// Aucune direction possible
							dirChoisie = Dir.NORD;
						}
				break;
			case "N":
				// Test 1
				// case valide et encore au moins jump dispo ?
				if(isCaseAvailable(PositionJN) && nosInfo.getJumpAvailable()>0)
				{
					dirChoisie = Dir.JUMP_NORD;
				}
				// Test 2
				else if(isCaseAvailable(PositionE) || isCaseAvailable(PositionO))
					{
						if(isCaseAvailable(PositionE))
						{
							dirChoisie = Dir.EST;
						}
						else
						{
							dirChoisie = Dir.OUEST;
						}
					}
				// Test 3
					else if(isCaseAvailable(PositionJE) || isCaseAvailable(PositionJO))
						{
							if(isCaseAvailable(PositionJE))
							{
								dirChoisie = Dir.JUMP_EST;
							}
							else
							{
								dirChoisie = Dir.JUMP_OUEST;
							}
						}
				// Test 4
						else if(isCaseAvailable(PositionS))
						{
							dirChoisie = Dir.SUD;
						}
				// Test 5
						else if(isCaseAvailable(PositionJS))
						{
							dirChoisie = Dir.JUMP_SUD;
						}
						else
						{
							// Aucune direction possible
							dirChoisie = Dir.SUD;
						}
				break;
			case "E":
				// Test 1
				// case valide et encore au moins jump dispo ?
				if(isCaseAvailable(PositionJE) && nosInfo.getJumpAvailable()>0)
				{
					dirChoisie = Dir.JUMP_EST;
				}
				// Test 2
				else if(isCaseAvailable(PositionN) || isCaseAvailable(PositionS))
					{
						if(isCaseAvailable(PositionN))
						{
							dirChoisie = Dir.NORD;
						}
						else
						{
							dirChoisie = Dir.SUD;
						}
					}
				// Test 3
					else if(isCaseAvailable(PositionJN) || isCaseAvailable(PositionJS))
						{
							if(isCaseAvailable(PositionJN))
							{
								dirChoisie = Dir.JUMP_NORD;
							}
							else
							{
								dirChoisie = Dir.JUMP_SUD;
							}
						}
				// Test 4
						else if(isCaseAvailable(PositionO))
						{
							dirChoisie = Dir.OUEST;
						}
				// Test 5
						else if(isCaseAvailable(PositionJO))
						{
							dirChoisie = Dir.JUMP_OUEST;
						}
						else
						{
							// Aucune direction possible
							dirChoisie = Dir.EST;
						}
				break;
			case "O":
				// Test 1
				// case valide et encore au moins jump dispo ?
				if(isCaseAvailable(PositionJO) && nosInfo.getJumpAvailable()>0)
				{
					dirChoisie = Dir.JUMP_OUEST;
				}
				// Test 2
				else if(isCaseAvailable(PositionN) || isCaseAvailable(PositionS))
					{
						if(isCaseAvailable(PositionN))
						{
							dirChoisie = Dir.NORD;
						}
						else
						{
							dirChoisie = Dir.SUD;
						}
					}
				// Test 3
					else if(isCaseAvailable(PositionJN) || isCaseAvailable(PositionJS))
						{
							if(isCaseAvailable(PositionJN))
							{
								dirChoisie = Dir.JUMP_NORD;
							}
							else
							{
								dirChoisie = Dir.JUMP_SUD;
							}
						}
				// Test 4
						else if(isCaseAvailable(PositionE))
						{
							dirChoisie = Dir.EST;
						}
				// Test 5
						else if(isCaseAvailable(PositionJE))
						{
							dirChoisie = Dir.JUMP_EST;
						}
						else
						{
							// Aucune direction possible
							dirChoisie = Dir.OUEST;
						}
				break;
			default:
				break;
			}
		}

		return dirChoisie;
	}
	
	/**
	 * Fonction qui calcul la position apres la direction renseignée
	 * @param currentPosition
	 * @param action
	 * @return
	 */
	public Point calculPositionFutur(Point currentPosition, Dir action)
	{
		Point futurPosition = (Point)currentPosition.clone();
		switch (action.code) {
		case "S":
			futurPosition.y = futurPosition.y + 1;
			break;
		case "N":
			futurPosition.y = futurPosition.y - 1;
			break;
		case "E":
			futurPosition.x = futurPosition.x + 1;
			break;
		case "O":
			futurPosition.x = futurPosition.x - 1;
			break;
		case "JS":
			futurPosition.y = futurPosition.y + 2;
			break;
		case "JN":
			futurPosition.y = futurPosition.y - 2;
			break;
		case "JO":
			futurPosition.x = futurPosition.x - 2;
			break;
		case "JE":
			futurPosition.x = futurPosition.x + 2;
			break;
		default:
			break;
		}
		return futurPosition;
	}
	
	
	/**
	 * Ajouter la team que l'on a tackler en dernier
	 * @param currentPoint
	 * @param d
	 * @param concurrent
	 * @return
	 */
	public long setTeamIdTackle(Point currentPoint, Dir d, HashMap<Long, Concurrent> lConcurrent)
	{
		Point futurPosition = (Point) currentPoint.clone();
		switch(d.code)
		{
			case "N": futurPosition.y = futurPosition.y-1;
				break;
			case "S": futurPosition.y = futurPosition.y+1;
				break;
			case "E": futurPosition.x = futurPosition.x+1;
				break;
			case "O": futurPosition.x = futurPosition.x-1;
				break;
			case "JN": futurPosition.y = futurPosition.y-2;
				break;
			case "JS": futurPosition.y = futurPosition.y+2;
				break;	
			case "JE": futurPosition.x = futurPosition.x+2;
				break;
			case "JO": futurPosition.x = futurPosition.x-2;
				break;
		}
		
		// Si >0 je vais tackler someone
		int nbTeamTackler = nombreTeamTackle(futurPosition, lConcurrent);
		
		// Multibaffle
		if(nbTeamTackler>2)
		{
			teamIdTackle = -1;
		}
		else
		{
			// 1 baffle
			if(nbTeamTackler==1)
			{
				teamIdTackle = WhoIsAdjacent(futurPosition, lConcurrent).getIdTeam();
			}
			// 0 baffe, on garde le teamIdTackle du tour précédent
		}

		return teamIdTackle;
	}
	
	/**
	 * Retour le teamId de l'equipe adjacente à nous
	 * @param concurrent
	 * @return
	 */
	public Concurrent WhoIsAdjacent(Point ourFuturPosition, HashMap<Long, Concurrent> concurrent)
	{
		Concurrent tmp =null;
		for ( Entry<Long, Concurrent> Listconcurrent : concurrent.entrySet()) {
			// Concurrent a coté de nous 
			if(isConcurrentAdjacent(ourFuturPosition, Listconcurrent.getValue()))
			{
				tmp = Listconcurrent.getValue();
			}
		}
		return tmp;
	}
	
	/**
	 * Calcul le nombre d'équipe pouvant etre tacklé sur la futur position
	 * @param ourFuturPosition
	 * @param concurrent
	 * @return
	 */
	public int nombreTeamTackle(Point ourFuturPosition, HashMap<Long, Concurrent> concurrent)
	{
		int nbTackleTeam= 0;
		for ( Entry<Long, Concurrent> Listconcurrent : concurrent.entrySet()) {
			// Concurrent a coté de nous mais pas nous !!!!
			if(!Listconcurrent.getKey().equals(teamId) && isConcurrentAdjacent(ourFuturPosition, Listconcurrent.getValue()))
			{
				// Concurrent deja tacklé ou concurrent sur son caddie ?
				if(!Listconcurrent.getKey().equals(teamIdTackle) && !Listconcurrent.getValue().getCurrentPoint().equals(Listconcurrent.getValue().getCaddyePoint())) 
				{
					nbTackleTeam++;
				}
			}
		}
		return nbTackleTeam;
	}
	
	/**
	 * Est ce qu'un concurrent est juste à coté de nous ?
	 * @param ourFuturPosition
	 * @param c
	 * @return
	 */
	public boolean isConcurrentAdjacent(Point ourFuturPosition, Concurrent c)
	{
		boolean isAdjacent=false;
		
		// Test concurrent à l'EST
		if( new Point(ourFuturPosition.x + 1, ourFuturPosition.y).equals(c.getCurrentPoint()))
		{
			isAdjacent = true;
		}
		
		// Test concurrent à l'OUEST
		if( new Point(ourFuturPosition.x - 1, ourFuturPosition.y).equals(c.getCurrentPoint()))
		{
			isAdjacent = true;
		}
		
		// Test concurrent au Nord
		if( new Point(ourFuturPosition.x, ourFuturPosition.y-1).equals(c.getCurrentPoint()))
		{
			isAdjacent = true;
		}
		
		// Test concurrent au SUD
		if( new Point(ourFuturPosition.x, ourFuturPosition.y+1).equals(c.getCurrentPoint()))
		{
			isAdjacent = true;
		}
		
		return isAdjacent;
	}
	
	/***
	 * lConcurrent: list of concurrent
	 * ourPosition: the current position of the team 
	 * @return Dir direction to be sent to server
	 */
	public Dir goTOConcurrent(HashMap<Long, Concurrent> lConcurrent, Point ourPosition, List<Dir> lDirectionDispo, long teamIdTackle){
		
		// déterminer quel concurrent viser
		Point closerConcurrent = findCloserConcurrentToBeat(lConcurrent, ourPosition, teamIdTackle);
		
		// déterminer le chemin à prendre
		int deltaX = closerConcurrent.x - ourPosition.x;
		int deltaY = closerConcurrent.y - ourPosition.y;		
		
		return chooseAction (deltaX, deltaY, lDirectionDispo, ourPosition, lConcurrent, true);
	}
	
	/**
	 * Distance basique
	 * @param p1
	 * @param p2
	 * @return
	 */
	public double distance(Point p1, Point p2){
		double dx = p2.getX()-p1.getX();
		double dy = p2.getY()-p1.getY();
		double dist = java.lang.Math.sqrt(dx*dx+dy*dy);
		return dist;
	}
	
	/**
	 * Distance en terme de nombre de déplacements nécessaires
	 * @param p1
	 * @param p2
	 * @return
	 */
	public double distanceMoves(Point p1, Point p2){
		double dx = Math.abs(p2.getX()-p1.getX());
		double dy = Math.abs(p2.getY()-p1.getY());
		return dx + dy;
	}
	
	/**
	 * Fonction qui check si la case donnée est libre
	 * @param p
	 * @return
	 */
	public boolean isCaseAvailable(Point p)
    {
        if(p.x>15 || p.y>12 || p.x>-1 || p.y >-1)
        {
            return false;
        }
        else
        {
            if(Terrain[p.x][p.y])
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
	
	/**
	 * Listes des directions possibles pour le tour
	 * @param currentPoint
	 * @return
	 */
	public List<Client.Dir> DirectionAvailable(Point currentPoint, int jumpAvailable)
	{
		List<Client.Dir> DirectionAvailable = new ArrayList<Client.Dir>();
		
		// déplacements classiques
		if(currentPoint.x+1<16)
		{
			if(Terrain[currentPoint.x+1][currentPoint.y])
			{
				DirectionAvailable.add(Dir.EST);
			}
		}
		if(currentPoint.y+1<13)
		{
			if(Terrain[currentPoint.x][currentPoint.y+1])
			{
				DirectionAvailable.add(Dir.SUD);
			}
		}
		if(currentPoint.x-1>-1)
		{
			if(Terrain[currentPoint.x-1][currentPoint.y])
			{
				DirectionAvailable.add(Dir.OUEST);
			}
		}
		if(currentPoint.y-1>-1)
		{
			if(Terrain[currentPoint.x][currentPoint.y-1])
			{
				DirectionAvailable.add(Dir.NORD);
			}
		}
		
		// déplacements jump
		if ( jumpAvailable > 0 ) {
			if(currentPoint.x+2<16)
			{
				if(Terrain[currentPoint.x+2][currentPoint.y])
				{
					DirectionAvailable.add(Dir.JUMP_EST);
				}
			}
			if(currentPoint.y+2<13)
			{
				if(Terrain[currentPoint.x][currentPoint.y+2])
				{
					DirectionAvailable.add(Dir.JUMP_SUD);
				}
			}
			if(currentPoint.x-2>-1)
			{
				if(Terrain[currentPoint.x-2][currentPoint.y])
				{
					DirectionAvailable.add(Dir.JUMP_OUEST);
				}
			}
			if(currentPoint.y-2>-1)
			{
				if(Terrain[currentPoint.x][currentPoint.y-2])
				{
					DirectionAvailable.add(Dir.JUMP_NORD);
				}
			}
		}
		
		return DirectionAvailable;
	}
	
	/**
	 * Fonction qui calcul le nombre de logo encore dispo en jeu 
	 * donc ceux qui ne sont pas dans des caddies
	 * @param llogos
	 * @param lCaddies
	 * @return nb dispo de logos
	 */
	public int nbLogoAvailable(ArrayList<Point> llogos, HashMap<Long, Point> lCaddies)
	{
		int nbLogoDispo=10;
		for (Point point : llogos) {
			for (Entry<Long, Point> ListCaddie : lCaddies.entrySet()) {
				if(point.equals(ListCaddie.getValue()))
				{
					nbLogoDispo--;
				}
			}
		}
		return nbLogoDispo;
	}
	
	
	public Dir computeDirection() {
		return Dir.values()[rand.nextInt(Dir.values().length)];
	}

	public String getIpServer() {
		return ipServer;
	}

	public void setIpServer(String ipServer) {
		this.ipServer = ipServer;
	}

	public long getTeamId() {
		return teamId;
	}

	public void setTeamId(long teamId) {
		this.teamId = teamId;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public int getSocketNumber() {
		return socketNumber;
	}

	public void setSocketNumber(int socketNumber) {
		this.socketNumber = socketNumber;
	}

	public long getGameId() {
		return gameId;
	}

	public void setGameId(long gameId) {
		this.gameId = gameId;
	}

	public Random getRand() {
		return rand;
	}

	public void setRand(Random rand) {
		this.rand = rand;
	}

	public boolean isEtat() {
		return etat;
	}

	public void setEtat(boolean etat) {
		this.etat = etat;
	}

	public long getTeamIdTackle() {
		return teamIdTackle;
	}

	public boolean isHasLogo() {
		return hasLogo;
	}

	public void setHasLogo(boolean hasLogo) {
		this.hasLogo = hasLogo;
	}

	public int getNbJumpleAvailable() {
		return nbJumpleAvailable;
	}

	public void setNbJumpleAvailable(int nbJumpleAvailable) {
		this.nbJumpleAvailable = nbJumpleAvailable;
	}

	public HashMap<Long, Concurrent> getlConcurrent() {
		return lConcurrent;
	}

	public void setlConcurrent(HashMap<Long, Concurrent> lConcurrent) {
		this.lConcurrent = lConcurrent;
	}

	public static long getTimeToResponse() {
		return timeToResponse;
	}

	public static void setTimeToResponse(long timeToResponse) {
		Client.timeToResponse = timeToResponse;
	}

}
