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

	enum Algorithm { NAIVE, BFS, ASTAR }
	
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
        Plan plan = new Plan(vehicle.getCurrentCity());
		return plan;
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {

	    //define initial state & all possible goal states (city = delivery/pickup city, currentTasks = null, tasksLeft = 0)
        final List<Task> initialTasks = getInitialTasks(tasks);
        List<Task> currentTasks = new LinkedList<>();
        final List<State> goalStates = evaluateGoalStates(tasks);
        int numberOfGoalStates = goalStates.size();

		State initialState = new State(vehicle.getCurrentCity(), currentTasks, initialTasks);

		Node root = new Node(0, initialState, null);
        createTree(root, initialTasks, goalStates);

        //BFS Algorithm
        List<State> actions = bfsAlgorithm(initialState, initialTasks, goalStates);

		Plan plan = new Plan(initialState.city);

		return plan;
	}

	public void createTree(Node root, List<Task> remainingTasks, List<State> goalStates){

	    int depthLvl = 0;
        int nodeIndex = 1;
        int totalWeight = 0;

        List<City> neighbors;
        List<State> visitedStates = new ArrayList<>();
        List<Node> parents = new LinkedList<>();
        List<Node> nextParents = new LinkedList<>();

        //Lists with all pickup and delivery destinations
        List<City> deliveries = new ArrayList<>(remainingTasks.size());
        List<City> pickups = new ArrayList<>(remainingTasks.size());
        for(int index = 0; index < remainingTasks.size(); index++){
            Task task = remainingTasks.get(index);
            deliveries.add(task.deliveryCity);
            pickups.add(task.pickupCity);
        }

        System.out.println("TaskSet:");
        System.out.println(remainingTasks.toString());

        visitedStates.add(root.state);
        parents.add(root);
        nextParents.add(root);

        //makes tree

        while(nextParents.size()!=0) {

            nextParents.clear();
            depthLvl++;
            System.out.println("Depth of Tree: " + depthLvl);
            System.out.println("Size parents: "+ parents.size());

            for (Node node : parents) {

                State currentState = node.state;

                //is delivery possible?
                if (deliveries.contains(currentState.city) && currentState.currentTasks != null) {
                    for (Task task : currentState.currentTasks) {
                        //always make a delivery
                        if (task.deliveryCity.equals(currentState.city)) {
                            State nextState = new State(currentState.city, currentState.currentTasks, currentState.remainingTasks);
                            nextState.currentTasks.remove(task);
                            nextParents.add(addNode(nodeIndex, nextState, node, visitedStates));
                            nodeIndex++;
                        }
                    }
                }

                //is pickup possible?
                if(pickups.contains(currentState.city)) {
                    //sum of current weights
                    if(currentState.currentTasks != null) {
                        for (Task task : currentState.currentTasks) {
                            totalWeight += task.weight;
                        }
                    }

                    Iterator<Task> iterator = currentState.remainingTasks.iterator();
                    while (iterator.hasNext()) {
                        Task task = iterator.next();
                        if (task.pickupCity.equals(currentState.city) && (totalWeight + task.weight) <= capacity) {
                            State nextState = new State(currentState.city, currentState.currentTasks, currentState.remainingTasks);
                            nextState.remainingTasks.remove(task);
                            //List<Task> nextCurTasks = new LinkedList<>(nextState.currentTasks.add(task));
                            nextState.currentTasks.add(task);

                            //already visited?
                            if (!visitedStates.contains(nextState)) {
                                nextParents.add(addNode(nodeIndex, nextState, node, visitedStates));
                                nodeIndex++;
                            }
                        }
                    }
                }

                //Search Neighbouring Cities
                neighbors = currentState.city.neighbors();
                for (City neighborCity : neighbors) {
                    State nextState = new State(neighborCity, currentState.currentTasks, currentState.remainingTasks);
                    if (!visitedStates.contains(nextState)) {
                        nextParents.add(addNode(nodeIndex, nextState, node, visitedStates));
                        nodeIndex++;
                    }
                }
            }


            System.out.println("Size nextParents: "+ nextParents.size());
            for (Node node : nextParents){
                parents.add(node);
            }

        }
    }

    private Node addNode(int index, State nextState, Node parent, List<State> visitedStates){
        Node node = new Node(index, nextState, parent);
        visitedStates.add(nextState);
        parent.children.add(node);
        System.out.println("Node added, index: "+ index+", City: "+ nextState.city);
        return node;
    }

	public List<State> bfsAlgorithm(State currentState, List<Task> remainingTasks, List<State> goalStates){

		//Final list with all state transitions (= plan)
		Map<Integer, DeliberativeTemplate.State> nodes = new HashMap<>();

        List<DeliberativeTemplate.State> actions = new LinkedList<>();

		return actions;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}

	private List<State> evaluateGoalStates(TaskSet tasks){
        List<State> goalStates = new ArrayList<>();

        for (Task task : tasks) {
			State goalStateDel = new State(task.deliveryCity, null, null);
			State goalStatePU = new State(task.pickupCity, null, null);
            if(!goalStates.contains(goalStatePU))
                goalStates.add(goalStatePU);
			if(!goalStates.contains(goalStateDel))
				goalStates.add(goalStateDel);
        }
        return goalStates;
    }

    private List<Task> getInitialTasks(TaskSet tasks){
        List<Task> initialTasks = new ArrayList<>();

        for (Task task : tasks)
            initialTasks.add(task);

        return initialTasks;
    }

    private class Node {

	    private int index;
        private State state;
        private Node parent;
        private List<Node> children;

        public Node(int index, State state, Node parent) {
            this.index = index;
            this.state = state;
            this.parent = parent;
            this.children = new ArrayList<>();
        }
    }

    public class State {
        private City city;
        private List<Task> currentTasks;
        private List<Task> remainingTasks;

        public State(City city, List<Task> currentTasks, List<Task> remainingTasks) {
            this.city = city;
            this.currentTasks = currentTasks;
            this.remainingTasks = remainingTasks;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj instanceof State) {
                State s = (State)obj;
                if((currentTasks != null && s.currentTasks != null) && (remainingTasks != null && s.remainingTasks != null)) {
                    return city.equals(s.city) && currentTasks.equals(s.currentTasks) && remainingTasks.equals(s.remainingTasks);
                } else if ((remainingTasks == null && s.remainingTasks == null) && (currentTasks != null && s.currentTasks != null)) {
                    return city.equals(s.city) && currentTasks.equals(s.currentTasks);
                } else if ((currentTasks == null && s.currentTasks == null) && (remainingTasks != null && s.remainingTasks != null)){
                    return city.equals(s.city) && remainingTasks.equals(s.remainingTasks);
                }
            }
            return false;
        }
    }

}
