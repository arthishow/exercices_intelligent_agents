import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private static final int RABBIT_MIN_ENERGY = 0;

	private int x;
	private int y;
	private int energy;

	public RabbitsGrassSimulationAgent(int energy){
		x = -1;
		y = -1;
		energy = energy;
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		
	}

	public int getX() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

}
