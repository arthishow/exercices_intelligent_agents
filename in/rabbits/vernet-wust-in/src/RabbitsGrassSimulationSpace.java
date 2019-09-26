import uchicago.src.sim.space.Object2DTorus;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {

    private Object2DTorus grassSpace;
    private Object2DTorus agentSpace;

    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        grassSpace = new Object2DTorus(xSize, ySize);
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

    public boolean isGrassCellOccupied(int x, int y){
        if(grassSpace.getObjectAt(x, y)!=null) return true;
        return false;
    }

    public boolean isRabbitCellOccupied(int x, int y){
        if(agentSpace.getObjectAt(x, y)!=null) return true;
        return false;
    }

    public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

        while((retVal==false) && (count < countLimit)){
            int x = (int)(Math.random()*(agentSpace.getSizeX()));
            int y = (int)(Math.random()*(agentSpace.getSizeY()));
            if(isRabbitCellOccupied(x,y) == false){
                agentSpace.putObjectAt(x,y,agent);
                //agent.setXY(x,y);
                retVal = true;
            }
            count++;
        }
        return retVal;
    }

    public Object2DTorus getCurrentGrassSpace(){
        return grassSpace;
    }

}
