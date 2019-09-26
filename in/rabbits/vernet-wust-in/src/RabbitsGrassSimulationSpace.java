import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid rabbitsGrassSpace;

    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        rabbitsGrassSpace = new Object2DGrid(xSize, ySize);
        for(int i = 0; i < xSize; i++){
            for(int j = 0; j < ySize; j++){
                rabbitsGrassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
    }

}
