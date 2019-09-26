import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
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
	private static final double GRASS_GROWTH_RATE = 20;
	private static final double BIRTH_THRESHOLD = 20;
	private static final int RABBIT_INIT_ENERGY = 20;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;
	private ArrayList rabbitList;
	private DisplaySurface displaySurface;
	private int GridSize = GRID_SIZE;
	private int NumInitRabbits = NUM_INIT_RABBITS;
	private int NumInitGrass = NUM_INIT_GRASS;
	private int RabbitInitEnergy = RABBIT_INIT_ENERGY;
	private double GrassGrowthRate = GRASS_GROWTH_RATE;
	private double BirthThreshold = BIRTH_THRESHOLD;

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
		rabbitsGrassSpace = new RabbitsGrassSimulationSpace(GridSize, GridSize);
		rabbitsGrassSpace.spreadGrass(NumInitGrass);
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
		displaySurface.addDisplayable(displayGrass, "Money");
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
		return GridSize;
	}

	public void setGridSize(int gridSize) {
		GridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return NumInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		NumInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return NumInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		NumInitGrass = numInitGrass;
	}

	public double getGrassGrowthRate() {
		return GrassGrowthRate;
	}

	public void setGrassGrowthRate(double grassGrowthRate) {
		GrassGrowthRate = grassGrowthRate;
	}

	public double getBirthThreshold() {
		return BirthThreshold;
	}

	public void setBirthThreshold(double birthThreshold) {
		BirthThreshold = birthThreshold;
	}

	public int getRabbitInitEnergy() {
		return RabbitInitEnergy;
	}

	public void setRabbitInitEnergy(int rabbitInitEnergy) {
		RabbitInitEnergy = rabbitInitEnergy;
	}
}
