import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	private static final int GRID_SIZE = 20;
	private static final int NUM_INIT_RABBITS = 20;
	private static final int NUM_INIT_GRASS = 20;
	private static final int GRASS_GROWTH_RATE = 20;
	private static final int BIRTH_THRESHOLD = 20;
	private static final int RABBIT_INIT_ENERGY = 20;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;
	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;
	private DisplaySurface displaySurface;
	private int gridSize = GRID_SIZE;
	private int numInitRabbits = NUM_INIT_RABBITS;
	private int numInitGrass = NUM_INIT_GRASS;
	private int rabbitInitEnergy = RABBIT_INIT_ENERGY;
	private int grassGrowthRate = GRASS_GROWTH_RATE;
	private int birthThreshold = BIRTH_THRESHOLD;

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	// responsible for initializing the simulation
	public void begin() {
		// TODO Auto-generated method stub
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurface.display();
	}

	private void buildModel(){
		System.out.println("Running BuildModel");
		rabbitsGrassSpace = new RabbitsGrassSimulationSpace(gridSize, gridSize);
		rabbitsGrassSpace.spreadGrass(numInitGrass);

		for(int i = 0; i < numInitRabbits; i++){
			addNewRabbit();
		}

		for (RabbitsGrassSimulationAgent rabbit : rabbitList) {
			rabbit.report();
		}
	}

	private void addNewRabbit(){
		RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(rabbitInitEnergy);
		rabbitList.add(rabbit);
		rabbitsGrassSpace.addRabbit(rabbit);
	}

	private void buildSchedule(){
		System.out.println("Running BuildSchedule");
	}

	private void buildDisplay(){
		System.out.println("Running BuildDisplay");
		ColorMap map = new ColorMap();

		map.mapColor(0, Color.white);
		map.mapColor(1, Color.green);

		Value2DDisplay displayGrass = new Value2DDisplay(rabbitsGrassSpace.getCurrentGrassSpace(), map);
		Object2DDisplay displayRabbits = new Object2DDisplay(rabbitsGrassSpace.getCurrentRabbitSpace());

		displayRabbits.setObjectList(rabbitList);

		displaySurface.addDisplayable(displayGrass, "Grass");
		displaySurface.addDisplayable(displayRabbits, "Rabbits");
	}

	// returns an array of String variables, each one listing the name of a particular parameter
	// that you want to be available to vary using the RePast control panel
	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitInitEnergy"};
		return params;
	}

	// returns the name of the simulation model being run
	// this name appears in some of the RePast toolbars (though not the one shown)
	public String getName() {
		// TODO Auto-generated method stub
		return "Rabbits simulation";
	}

	// return an object of type ‘Schedule.’
	// every RePast model will have at least one schedule object
	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return schedule;
	}

	// called when the button with the two curved arrows is pressed
	public void setup() {
		// TODO Auto-generated method stub
		rabbitsGrassSpace = null;
		rabbitList = new ArrayList();
		if (displaySurface != null){
			displaySurface.dispose();
		}
		displaySurface = null;
		displaySurface = new DisplaySurface(this, "Rabbits test 1");
		registerDisplaySurface("Rabbits test 1", displaySurface);

	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gridSize) {
		gridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		numInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		numInitGrass = numInitGrass;
	}

	public double getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(double grassGrowthRate) {
		grassGrowthRate = grassGrowthRate;
	}

	public double getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(double birthThreshold) {
		birthThreshold = birthThreshold;
	}

	public int getRabbitInitEnergy() {
		return rabbitInitEnergy;
	}

	public void setRabbitInitEnergy(int rabbitInitEnergy) {
		rabbitInitEnergy = rabbitInitEnergy;
	}
}
