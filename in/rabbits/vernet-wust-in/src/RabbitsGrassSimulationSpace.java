import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {

    private Object2DGrid grassSpace;

    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        grassSpace = new Object2DGrid(xSize, ySize);
        for(int i = 0; i < xSize; i++){
            for(int j = 0; j < ySize; j++){
                grassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
    }

    // TODO
    public void spreadGrass(int grass){

        for(int i = 0; i < grass; i++){
            // Choose coordinates
            int x = (int)(Math.random()*(grassSpace.getSizeX()));
            int y = (int)(Math.random()*(grassSpace.getSizeY()));

            // Get the value of the object at those coordinates
            int I;
            if(grassSpace.getObjectAt(x,y)!= null){
                I = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
            } else {
                I = 0;
            }
            // Replace the Integer object with another one with the new value
            grassSpace.putObjectAt(x,y,new Integer(1));
        }
    }

    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }


}
