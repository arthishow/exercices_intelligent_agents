import uchicago.src.sim.space.Object2DTorus;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {

    private static final int MAX_NUM_TRY = 10;

    private Object2DTorus grassSpace;
    private Object2DTorus rabbitSpace;
    private int grassEnergy;

    public RabbitsGrassSimulationSpace(int xSize, int ySize, int grassEnergy){
        grassSpace = new Object2DTorus(xSize, ySize);
        rabbitSpace = new Object2DTorus(xSize, ySize);
        this.grassEnergy = grassEnergy;

        for(int i = 0; i < xSize; i++){
            for(int j = 0; j < ySize; j++){
                grassSpace.putObjectAt(i, j, 0);
            }
        }
    }

    public boolean spreadGrass(int grass){

        List<List<Integer>> coordinates = getGrassFreeCellsCoordinates();
        int numGrass = Math.min(coordinates.get(0).size(), grass);
        for(int i = 0; i < numGrass; i++){
            int n = (int) (Math.random() * coordinates.get(0).size());
            int x = coordinates.get(0).get(n);
            int y = coordinates.get(1).get(n);
            coordinates.get(0).remove(n);
            coordinates.get(1).remove(n);
            grassSpace.putObjectAt(x, y, 1);
        }

        return numGrass == grass;
    }

    public boolean addRabbit(RabbitsGrassSimulationAgent rabbit) {

        List<List<Integer>> coordinates = getRabbitsFreeCellsCoordinates();
        int numFreeCells = coordinates.get(0).size();
        if (numFreeCells > 0) {
            int n = (int) (Math.random() * numFreeCells);
            int x = coordinates.get(0).get(n);
            int y = coordinates.get(1).get(n);
            rabbitSpace.putObjectAt(x, y, rabbit);
            rabbit.setXY(x, y);
            rabbit.setRabbitsGrassSpace(this);
            return true;
        }
        return false;
    }

    private List<List<Integer>> getRabbitsFreeCellsCoordinates(){
        List<Integer> xs = new ArrayList();
        List<Integer> ys = new ArrayList();

        for(int x = 0; x < rabbitSpace.getSizeX(); x++) {
            for (int y = 0; y < rabbitSpace.getSizeY(); y++) {
                if(!isRabbitCellOccupied(x, y)){
                    xs.add(x);
                    ys.add(y);
                }
            }
        }
        List<List<Integer>> coordinates = new ArrayList<>();
        coordinates.add(xs);
        coordinates.add(ys);

        return coordinates;
    }

    public List<List<Integer>> getGrassFreeCellsCoordinates(){
        List<Integer> xs = new ArrayList();
        List<Integer> ys = new ArrayList();

        for(int x = 0; x < rabbitSpace.getSizeX(); x++) {
            for (int y = 0; y < rabbitSpace.getSizeY(); y++) {
                if(!isGrassCellOccupied(x, y)){
                    xs.add(x);
                    ys.add(y);
                }
            }
        }
        List<List<Integer>> coordinates = new ArrayList<>();
        coordinates.add(xs);
        coordinates.add(ys);

        return coordinates;
    }


    private boolean isGrassCellOccupied(int x, int y){
        return grassSpace.getValueAt(x, y) == 1;
    }

    private boolean isRabbitCellOccupied(int x, int y){
        return rabbitSpace.getObjectAt(x, y) != null;
    }

    public void removeRabbitAt(int x, int y){
        rabbitSpace.putObjectAt(x, y, null);
    }

    public boolean moveRabbitAt(int x, int y, int newX, int newY){
        if(!isRabbitCellOccupied(newX, newY)){
            RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbitSpace.getObjectAt(x, y);
            removeRabbitAt(x,y);
            rabbit.setXY(newX, newY);
            rabbitSpace.putObjectAt(newX, newY, rabbit);
            return true;
        }
        return false;
    }

    public int removeGrassAt(int x, int y){
        if(isGrassCellOccupied(x, y)){
            grassSpace.putObjectAt(x, y, 0);
            return grassEnergy;
        }
        return 0;
    }

    public Object2DTorus getCurrentGrassSpace(){
        return grassSpace;
    }

    public Object2DTorus getCurrentRabbitSpace(){
        return rabbitSpace;
    }

}
