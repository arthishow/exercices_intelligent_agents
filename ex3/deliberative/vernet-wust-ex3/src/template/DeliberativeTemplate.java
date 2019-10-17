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
	int capacity;
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
        final List<State> goalStates = evaluateGoalStates(tasks);
        int numberOfGoalStates = goalStates.size();

		State initialState = new State(vehicle.getCurrentCity(), null, initialTasks);

        //BFS Algorithm
        List<State> actions = bfsAlgorithm(initialState, initialTasks, goalStates);

		Plan plan = new Plan(initialState.city);

		return plan;
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

	private State pickup(State currentState, List<Task> remainingTasks, int totalWeight){

		State state = currentState;
		//check if multiple tasks can be picked up and if the capacity would not be exceeded
		for(Task task : currentState.remainingTasks){
			if(task.pickupCity.equals(currentState.city) && (totalWeight + task.weight) <= capacity){
				currentState.currentTasks.add(task);
				return state;
			}
		}
		return state;
	}

    private class State {
        private City city;
        private List<Task> currentTasks;
        private List<Task> remainingTasks;

        public State(City city, List<Task> currentTasks, List<Task> remainingTasks) {
            this.city = city;
            this.currentTasks = currentTasks;
            this.remainingTasks = remainingTasks;
        }

		//what if conditions must be met?
        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj instanceof State) {
                State s = (State)obj;
                if(currentTasks != null && s.currentTasks != null) {
                    return city.equals(s.city) && currentTasks.equals(s.currentTasks);
                } else if (currentTasks == null && s.currentTasks == null){
                    return city.equals(s.city);
                }
            }
            return false;
        }
    }

}
