import uchicago.src.sim.space.Object2DTorus;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {

    private static final int MAX_NUM_TRY = 10;

    private Object2DTorus grassSpace;
    private Object2DTorus rabbitSpace;

    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        grassSpace = new Object2DTorus(xSize, ySize);
        rabbitSpace = new Object2DTorus(xSize, ySize);

        for(int i = 0; i < xSize; i++){
            for(int j = 0; j < ySize; j++){
                grassSpace.putObjectAt(i, j, 0);
            }
        }
    }

    public boolean spreadGrass(int grass){

        int countLimit = MAX_NUM_TRY * grassSpace.getSizeX() * grassSpace.getSizeY();
        int numGrass = 0;

        for(int i = 0; i < grass; i++){
            int count = 0;
            while(count < countLimit) {
                int x = (int) (Math.random() * (grassSpace.getSizeX()));
                int y = (int) (Math.random() * (grassSpace.getSizeY()));

                if(!isGrassCellOccupied(x, y)) {
                    grassSpace.putObjectAt(x, y, 1);
                    count = countLimit;
                    numGrass++;
                } else {
                    count++;
                }
            }
        }
        return numGrass == grass;
    }

    private boolean isGrassCellOccupied(int x, int y){
        return grassSpace.getValueAt(x, y) == 1;
    }

    private boolean isRabbitCellOccupied(int x, int y){
        return rabbitSpace.getObjectAt(x, y) != null;
    }

    public boolean addRabbit(RabbitsGrassSimulationAgent rabbit){
        boolean retVal = false;
        int count = 0;
        int countLimit = MAX_NUM_TRY * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

        while(count < countLimit){
            int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
            int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
            if(!isRabbitCellOccupied(x,y)){
                rabbitSpace.putObjectAt(x, y, rabbit);
                rabbit.setXY(x,y);
                return true;
            }
            count++;
        }
        return false;
    }

    public Object2DTorus getCurrentGrassSpace(){
        return grassSpace;
    }

    public Object2DTorus getCurrentRabbitSpace(){
        return rabbitSpace;
    }

}
