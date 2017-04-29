import java.awt.Point;


public class Utilitaire {

	public Point getP() {
		return p;
	}
	public void setP(Point p) {
		this.p = p;
	}
	public double getMouvement() {
		return mouvement;
	}
	public void setMouvement(double mouvement) {
		this.mouvement = mouvement;
	}
	private Point p;
	private double mouvement;
	public Utilitaire(Point _p, double move)
	{
		p = _p;
		mouvement = move;
	}
	
}
