package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.security.Key;
import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

    enum Algorithm {NAIVE, BFS, ASTAR}

    /* Environment */
    Topology topology;
    TaskDistribution td;
    int numCities;

    /* the properties of the agent */
    Agent agent;
    Vehicle vehicle;
    int capacity = 30;
    int costPerKm;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        int costPerKm = agent.vehicles().get(0).costPerKm();
        int numCities = topology.cities().size();
        System.out.println("Number of cities: " + numCities);

        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

        // ...
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case NAIVE:
                // ...
                plan = naivePlan(vehicle, tasks);
                break;
            case ASTAR:
                // ...
                plan = astarPlan(vehicle, tasks);
                break;
            case BFS:
                // ...
                plan = bfsPlan(vehicle, tasks);
                break;
            default:
                throw new AssertionError("Should not happen.");
        }
        return plan;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    private Plan astarPlan(Vehicle vehicle, TaskSet tasks) {
        TaskSet initialTasks = tasks;
        TaskSet currentTasks = TaskSet.noneOf(tasks);
        State initialState = new State(vehicle.getCurrentCity(), currentTasks, initialTasks);
        Node root = new Node("0", initialState, null);
        Tree tree = new Tree(root);

        List<Node> Q = new ArrayList<>();
        Q.add(root);
        Map<State, Double> C = new HashMap<>();
        Node finalNode = null;
        while (!Q.isEmpty()) {
            Node n = Q.remove(0);
            if (n.state.isFinal()) {
                finalNode = n;
                break;
            } else if (!C.containsKey(n.state) || (n.distance() < C.get(n.state))) {
                C.put(n.state, n.distance());
                List<Node> S = successors(tree, n);
                Q.addAll(S);
                Q.sort((n1, n2) -> n1.aStarComparison(n2));
            }
        }

        Plan plan = planGivenFinalNode(finalNode);
        return plan;
    }

    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
        TaskSet initialTasks = tasks;
        TaskSet currentTasks = TaskSet.noneOf(tasks);
        State initialState = new State(vehicle.getCurrentCity(), currentTasks, initialTasks);
        Node root = new Node("0", initialState, null);
        Tree tree = new Tree(root);

        List<Node> finalNodes = new ArrayList<>();
        List<Node> Q = new ArrayList<>();
        Q.add(root);
        Map<State, Double> C = new HashMap<>();

        while (!Q.isEmpty()) {
            Node n = Q.remove(0);
            if (n.state.isFinal()) {
                finalNodes.add(n);
            } else if (!C.containsKey(n.state) || (n.distance() < C.get(n.state))) {
                C.put(n.state, n.distance());
                List<Node> S = successors(tree, n);
                Q.addAll(S);
            }
        }

        Node bestNode = null;
        double minDistance = Double.MAX_VALUE;
        for(Node node: finalNodes){
            double distance = node.distance();
            if(distance < minDistance){
                minDistance = distance;
                bestNode = node;
            }
        }

        //TODO convert best path into series of action
        Plan plan = planGivenFinalNode(bestNode);

        return plan;
    }

    public Plan planGivenFinalNode(Node node){

        Plan plan = null;
        return plan;
    }

    public List<Node> successors(Tree tree, Node node) {

        List<Node> successors = new ArrayList<>();
        State currentState = node.state;

        List<City> deliveries = new ArrayList<>();
        for (Task task : currentState.currentTasks) {
            deliveries.add(task.deliveryCity);
        }

        List<City> pickups = new ArrayList<>();
        for (Task task : currentState.remainingTasks) {
            pickups.add(task.pickupCity);
        }

        //is delivery possible? if yes, no need to add other states
        if (deliveries.contains(currentState.city) && currentState.currentTasks != null) {
            for (Task task : currentState.currentTasks) {
                if (task.deliveryCity.equals(currentState.city)) {
                    TaskSet taskCurrent = currentState.currentTasks.clone();
                    taskCurrent.remove(task);
                    State nextState = new State(currentState.city, taskCurrent, currentState.remainingTasks);
                    successors.add(tree.addNode(nextState, node));
                    break;
                }
            }
        }

        //is pickup possible?
        if (pickups.contains(currentState.city)) {
            int totalWeight = currentState.currentTasks.weightSum();
            for (Task task : currentState.remainingTasks) {
                if (task.pickupCity.equals(currentState.city) && (totalWeight + task.weight) <= capacity) {
                    TaskSet taskCurrent = currentState.currentTasks.clone();
                    taskCurrent.add(task);
                    TaskSet taskRemaining = currentState.remainingTasks.clone();
                    taskRemaining.remove(task);
                    State nextState = new State(currentState.city, taskCurrent, taskRemaining);
                    successors.add(tree.addNode(nextState, node));
                    break;
                }
            }
        }

        //visit all neighbouring cities
        for (City neighborCity : currentState.city.neighbors()) {
            State nextState = new State(neighborCity, currentState.currentTasks, currentState.remainingTasks);
            successors.add(tree.addNode(nextState, node));
        }

        return successors;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }
}

class Tree {
    public int numNode;
    public Node root;

    public Tree(Node root){
        this.root = root;
        this.numNode = 1;
    }

    public Node addNode(State nextState, Node parent) {
        Node node = new Node(parent.path+"-"+numNode, nextState, parent);
        parent.children.add(node);
        numNode++;
        return node;
    }
}

class Node {

    public String path;
    public State state;
    public Node parent;
    public List<Node> children;

    public Node(String path, State state, Node parent) {
        this.path = path;
        this.state = state;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public double distance(){
        double distance = 0;
        if(parent == null){
            return distance;
        }else{
            distance += state.city.distanceTo(parent.state.city) + parent.distance();
        }
        return distance;
    }

    public int aStarComparison(Node node2){
        double distance1 = distance() + state.heuristicDistanceToFinalState();
        double distance2 = node2.distance() + node2.state.heuristicDistanceToFinalState();
        return distance1 > distance2 ? 1 : (distance1 < distance2) ? -1 : 0;
    }
}

class State {
    public City city;
    public TaskSet currentTasks;
    public TaskSet remainingTasks;

    public State(City city, TaskSet currentTasks, TaskSet remainingTasks) {
        this.city = city;
        this.currentTasks = currentTasks;
        this.remainingTasks = remainingTasks;
    }

    public boolean isFinal(){
        return currentTasks.isEmpty() && remainingTasks.isEmpty();
    }

    public double heuristicDistanceToFinalState(){
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof State) {
            State s = (State) obj;
            if ((city != null && s.city != null) || (city == null && s.city == null)) {
                return city.equals(s.city) && currentTasks.equals(s.currentTasks) && remainingTasks.equals(s.remainingTasks);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if(city != null) {
            return Objects.hash(city.hashCode(), currentTasks.hashCode(), remainingTasks.hashCode());
        }else{
            return Objects.hash(currentTasks.hashCode(), remainingTasks.hashCode());
        }
    }
}
