import java.awt.Point;


public class Concurrent {

	private Long idTeam;
	private int jumpAvailable;
	private boolean safeAtHome;
	private boolean hasLogo;
	private Point lastPoint;
	private Point currentPoint;
	private Point caddyePoint;
	public String getEtat() {
		return etat;
	}


	public void setEtat(String etat) {
		this.etat = etat;
	}


	public int getScore() {
		return score;
	}


	public void setScore(int score) {
		this.score = score;
	}


	private String etat;
	private int score;
	
	public Concurrent(int _jumpAvailable, boolean _safeAtHome, boolean _hasLogo, Point _lastPoint, Point _currentPoint, Point _caddyePoint, Long _idTeam, String _etat, int _score)
	{
		jumpAvailable = _jumpAvailable;
		safeAtHome = _safeAtHome;
		hasLogo = _hasLogo;
		lastPoint = _lastPoint;
		currentPoint = _currentPoint;
		caddyePoint = _caddyePoint;
		idTeam = _idTeam;
		etat = _etat;
		score=_score;
	}
	
	
	public int getJumpAvailable() {
		return jumpAvailable;
	}
	public void setJumpAvailable(int jumpAvailable) {
		this.jumpAvailable = jumpAvailable;
	}
	public boolean isSafeAtHome() {
		return safeAtHome;
	}
	public void setSafeAtHome(boolean safeAtHome) {
		this.safeAtHome = safeAtHome;
	}
	public boolean isHasLogo() {
		return hasLogo;
	}
	public void setHasLogo(boolean hasLogo) {
		this.hasLogo = hasLogo;
	}
	public Point getLastPoint() {
		return lastPoint;
	}
	public void setLastPoint(Point lastPoint) {
		this.lastPoint = lastPoint;
	}
	public Point getCurrentPoint() {
		return currentPoint;
	}
	public void setCurrentPoint(Point currentPoint) {
		this.currentPoint = currentPoint;
	}
	public Point getCaddyePoint() {
		return caddyePoint;
	}
	public void setCaddyePoint(Point caddyePoint) {
		this.caddyePoint = caddyePoint;
	}


	public Long getIdTeam() {
		return idTeam;
	}


	public void setIdTeam(Long idTeam) {
		this.idTeam = idTeam;
	}
}
