import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	private static final int RABBIT_MIN_ENERGY = 0;
	private static final int GRID_SIZE = 20;
	private static final int NUM_INIT_RABBITS = 1;
	private static final int NUM_INIT_GRASS = 40;
	private static final int GRASS_GROWTH_RATE = 30;
	private static final int BIRTH_THRESHOLD = 30;
	private static final int RABBIT_INIT_ENERGY = 20;
	private static final int GRASS_ENERGY = 25;
	private static final boolean DEBUG = false;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;
	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;
	private DisplaySurface displaySurface;
	private OpenSequenceGraph plot;

	private int GridSize = GRID_SIZE;
	private int NumInitRabbits = NUM_INIT_RABBITS;
	private int NumInitGrass = NUM_INIT_GRASS;
	private int RabbitInitEnergy = RABBIT_INIT_ENERGY;
	private int GrassGrowthRate = GRASS_GROWTH_RATE;
	private int BirthThreshold = BIRTH_THRESHOLD;
	private int GrassEnergy = GRASS_ENERGY;

	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return getSValue();
		}

		public double getSValue() {
			return countLivingRabbits();
		}
	}

	class grassPatchesInSpace implements DataSource, Sequence {

		public Object execute() { return getSValue(); }

		public double getSValue() { return countGrassPatches(); }
	}

	// called when the button with the two curved arrows is pressed
	public void setup() {

		//Tear down
		rabbitsGrassSpace = null;
		rabbitList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurface != null){
			displaySurface.dispose();
		}
		displaySurface = null;

		if (plot != null){
			plot.dispose();
		}
		plot = null;

		// Create Displays
		displaySurface = new DisplaySurface(this, "Rabbits simulation");
		plot = new OpenSequenceGraph("Rabbits Simulation",this);
		plot.setAxisTitles("Timesteps", "Amount");

		// Register Displays
		registerDisplaySurface("Rabbits simulation", displaySurface);
		this.registerMediaProducer("Population plot", plot);
		this.registerMediaProducer("Resources plot", plot);
	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurface.display();
		plot.display();
	}

	private void buildModel(){
		rabbitsGrassSpace = new RabbitsGrassSimulationSpace(GridSize, GridSize, GrassEnergy);
		rabbitsGrassSpace.spreadGrass(NumInitGrass);

		for(int i = 0; i < NumInitRabbits; i++){
			addNewRabbit();
		}
	}

	private void buildSchedule(){

		class WorldEvolution extends BasicAction {
			public void execute() {

				rabbitsGrassSpace.spreadGrass(GrassGrowthRate);

				duplicateRabbits();
				SimUtilities.shuffle(rabbitList);
				for(RabbitsGrassSimulationAgent rabbit: rabbitList){
					rabbit.step();
					if(DEBUG) {
						rabbit.report();
					}
				}
				reapDeadRabbits();

				displaySurface.updateDisplay();

				countLivingRabbits();
				countGrassPatches();

			}
		}

		schedule.scheduleActionBeginning(2, new WorldEvolution());

		class Plot extends BasicAction {
			public void execute(){
				plot.step();
			}
		}

		schedule.scheduleActionAtInterval(1, new Plot());
	}

	private void buildDisplay(){
		ColorMap map = new ColorMap();

		map.mapColor(0, Color.white);
		map.mapColor(1, Color.green);

		Value2DDisplay displayGrass = new Value2DDisplay(rabbitsGrassSpace.getCurrentGrassSpace(), map);
		Object2DDisplay displayRabbits = new Object2DDisplay(rabbitsGrassSpace.getCurrentRabbitSpace());

		displayRabbits.setObjectList(rabbitList);

		displaySurface.addDisplayable(displayGrass, "Grass");
		displaySurface.addDisplayable(displayRabbits, "Rabbits");

		plot.addSequence("Rabbits", new rabbitsInSpace());
		plot.addSequence("Grass", new grassPatchesInSpace());
	}

	public static void main(String[] args) {
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	private void addNewRabbit(){
		RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(RabbitInitEnergy);
		rabbitList.add(rabbit);
		rabbitsGrassSpace.addRabbit(rabbit);
	}

	private int reapDeadRabbits(){
		int count = 0;

		for(RabbitsGrassSimulationAgent rabbit : new ArrayList<>(rabbitList)){
			if(rabbit.getEnergy() <= RABBIT_MIN_ENERGY){
				rabbitsGrassSpace.removeRabbitAt(rabbit.getX(), rabbit.getY());
				rabbitList.remove(rabbit);
				count++;
			}
		}

		if(DEBUG){
			System.out.println(count + " rabbits have died of exhaustion.");
		}
		return count;
	}

	private int duplicateRabbits(){
		int count = 0;
		for(RabbitsGrassSimulationAgent rabbit : new ArrayList<>(rabbitList)){
			if(rabbit.getEnergy() >= BirthThreshold){
				count++;
			}
		}

		for(int i = 0; i < count; i++){
			addNewRabbit();
		}

		if(DEBUG) {
			System.out.println(count + " rabbits were born.");
		}

		return count;
	}

	public String[] getInitParam() {
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitInitEnergy", "GrassEnergy"};
		return params;
	}

	public String getName() {
		return "Rabbits simulation";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	private int countLivingRabbits(){
		int livingRabbits = 0;
		for(RabbitsGrassSimulationAgent rabbit: rabbitList){
			if(rabbit.getEnergy() > RABBIT_MIN_ENERGY) livingRabbits++;
		}

		if(DEBUG) {
			System.out.println("Number of living rabbits is: " + livingRabbits);
		}

		return livingRabbits;
	}

	private int countGrassPatches(){

		int grassPatches = GridSize * GridSize - rabbitsGrassSpace.getGrassFreeCellsCoordinates().get(0).size();

		if(DEBUG) {
			System.out.println("Number of grass patches is: " + grassPatches);
		}

		return grassPatches;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public int getGridSize() {
		return GridSize;
	}

	public void setGridSize(int gridSize) {
		this.GridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return NumInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		this.NumInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return NumInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		this.NumInitGrass = numInitGrass;
	}

	public int getGrassGrowthRate() {
		return GrassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.GrassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold() {
		return BirthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.BirthThreshold = birthThreshold;
	}

	public int getRabbitInitEnergy() {
		return RabbitInitEnergy;
	}

	public void setRabbitInitEnergy(int rabbitInitEnergy) {
		this.RabbitInitEnergy = rabbitInitEnergy;
	}

	public int getGrassEnergy() {
		return GrassEnergy;
	}

	public void setGrassEnergy(int grassEnergy) {
		this.GrassEnergy = grassEnergy;
	}
}
