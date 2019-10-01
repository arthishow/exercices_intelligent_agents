import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private enum Direction {N, S, W, E};

	private static int IDNumber = 0;

	private int x;
	private int y;
	private Direction direction;
	private int energy;
	private int ID;
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;

	public RabbitsGrassSimulationAgent(int energy){
		this.x = -1;
		this.y = -1;
		this.energy = energy;
		IDNumber++;
		this.ID = IDNumber;
		setRandomDirection();
	}

	public String getID(){
		return "R-" + ID;
	}

	public Direction getDirection(){return this.direction;}

	private int getXDirection(){
		switch(direction){
			case E:
				return 1;
			case W:
				return -1;
			default:
				return 0;
		}
	}

	private int getYDirection(){
		switch(direction){
			case S:
				return 1;
			case N:
				return -1;
			default:
				return 0;
		}
	}


	public void step(){

		ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
		directions.remove(this.direction);

		do {
			int xDirection = getXDirection();
			int yDirection = getYDirection();
			int newX = x + xDirection;
			int newY = y + yDirection;

			if(tryMove(newX, newY)){
				this.energy += rabbitsGrassSpace.removeGrassAt(newX, newY);
				this.energy--;
				setRandomDirection();
				return;
			}else{
				Direction newDirection = directions.get(new Random().nextInt(directions.size()));
				directions.remove(newDirection);
				setDirection(newDirection);
			}
		}
		while(directions.size() > 0);

		this.energy = 0;
	}

	private boolean tryMove(int newX, int newY){
		newX = rabbitsGrassSpace.getCurrentRabbitSpace().xnorm(newX);
		newY = rabbitsGrassSpace.getCurrentRabbitSpace().xnorm(newY);
		return rabbitsGrassSpace.moveRabbitAt(x, y, newX, newY);
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

	private void setRandomDirection(){
		this.direction = Direction.values()[new Random().nextInt(Direction.values().length)];
	}

	private void setDirection(Direction direction){
		this.direction = direction;
	}

	public void setRabbitsGrassSpace(RabbitsGrassSimulationSpace rabbitsGrassSpace){
		this.rabbitsGrassSpace = rabbitsGrassSpace;
	}

	public int getEnergy(){
		return energy;
	}

	public void setEnergy(int newEnergy){
		energy = newEnergy;
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
				getEnergy() + " energy and is going " + getDirection() + ".");
	}

}
