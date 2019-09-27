import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private static final int RABBIT_MIN_ENERGY = 0;
	private static int IDNumber = 0;

	private int x;
	private int y;
	private int energy;
	private int ID;

	public RabbitsGrassSimulationAgent(int energy){
		x = -1;
		y = -1;
		energy = energy;
		IDNumber++;
		ID = IDNumber;
	}

	public String getID(){
		return "A-" + ID;
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		arg0.drawFastRoundRect(Color.blue);
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public int getEnergy(){
		return energy;
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public void report(){
		System.out.println(getID() +
				" at (" +
				x + ", " + y +
				") has " +
				getEnergy() + " energy.");
	}

}
