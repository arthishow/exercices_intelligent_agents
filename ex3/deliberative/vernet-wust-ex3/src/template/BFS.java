package template;

import logist.task.Task;
import logist.topology.Topology;

import java.util.*;

public class BFS {

    public static List<Node<State>> possibleTraversals(Tree<State> graph){

        List<Node<State>> goalStates = new ArrayList<>();

        List<Node<State>> Q = new ArrayList<>(graph.root);
        Set<Node> C = new HashSet<>();

        while(!Q.isEmpty()){
            Node<State> n = Q.remove(0);
            if(n.isFinal()){
                goalStates.append(n);
            } else if(!C.contains(n)){
                C.add(n);
                List<Node<State>> S = n.getChildren();
                Q.addAll(S);
            }
        }

        return goalStates;
    }


    public List<DeliberativeTemplate.State> bfsAlgorithm(DeliberativeTemplate.State currentState, List<Task> remainingTasks, List<DeliberativeTemplate.State> goalStates){

        //Final list with all state transitions (= plan)
        Map<Integer, DeliberativeTemplate.State> nodes = new HashMap<>();
        List<DeliberativeTemplate.State> actions = new LinkedList<>();

        int depthLvl = 0;
        int nodeIndex = 0;
        int totalWeight = 0;

        List<Topology.City> neighbors;

        //Lists with all pickup and delivery destinations
        List<Topology.City> deliveries = new ArrayList<>(remainingTasks.size());
        List<Topology.City> pickups = new ArrayList<>(remainingTasks.size());

        System.out.println("TaskSet:");
        System.out.println(remainingTasks.toString());

        for(int index = 0; index < remainingTasks.size(); index++){
            Task task = remainingTasks.get(index);
            deliveries.add(task.deliveryCity);
            pickups.add(task.pickupCity);
        }

        DeliberativeTemplate.State nextState = currentState;

        do {

            //is delivery possible?
            if(deliveries.contains(currentState.city)){
                for(Task task : currentState.currentTasks){
                    //always make a delivery
                    if(task.deliveryCity.equals(currentState.city)) {
                        nextState.currentTasks.remove(task);
                        nodes.put(nodeIndex, nextState);
                        nodeIndex++;
                    }
                }
            }

            //is pickup possible?
            if(pickups.contains(currentState.city)){
                for(Task task: currentState.currentTasks){
                    totalWeight += task.weight;
                }
                for(Task task : currentState.remainingTasks) {
                    //add all possible pickups to nodes
                    if (task.pickupCity.equals(currentState.city) && (totalWeight + task.weight) <= capacity) {
                        nextState.remainingTasks.remove(task);
                        nextState.currentTasks.add(task);
                        //already visited?
                        if(!nodes.containsValue(nextState)){
                            nodes.put(nodeIndex, nextState);
                            nodeIndex++;
                        }
                        else{

                        }
                    }
                }
            }

            neighbors = currentState.city.neighbors();
            while(neighbors.iterator().hasNext()){
                nextState.city = neighbors.iterator().next();
                if(!nodes.containsValue(nextState)){
                    nodes.put(nodeIndex, nextState);
                    nodeIndex++;
                }
            }

            depthLvl++;

        } while(!goalStates.contains(nextState));

        return actions;
    }

}