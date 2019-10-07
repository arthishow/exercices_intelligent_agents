package template;

import java.util.*;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double discount;
	private int numActions;
	private Agent myAgent;
	private TaskDistribution taskDistribution;
	private Topology topology;
	private Map<State, Decision> bestDecisions;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.discount = discount;
		this.numActions = 0;
		this.myAgent = agent;
		this.taskDistribution = td;
		this.topology = topology;
		if(agent.name().equals("reactive-rla")) {
			this.bestDecisions = computeBestDecisions(0.001);
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		String name = myAgent.name();

		if(name.equals("reactive-rla")) {
			State state;
			if (availableTask == null) {
				state = new State(vehicle, vehicle.getCurrentCity(), null);
			} else {
				state = new State(vehicle, vehicle.getCurrentCity(), availableTask.deliveryCity);
			}
			Decision decision = bestDecisions.get(state);

			if (decision.pickup) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(decision.destinationCity);
			}
		} else if(name.equals("reactive-random")){
			if (availableTask == null || random.nextDouble() > discount) {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			} else {
				action = new Pickup(availableTask);
			}
		} else {
			if (availableTask == null) {
				City currentCity = vehicle.getCurrentCity();
				double maxReward = 0;
				City nextCity = null;
				for(City city: currentCity.neighbors()){
					double reward = 0;
					for(City city2: topology.cities()){
						reward += taskDistribution.probability(city, city2)*taskDistribution.reward(city, city2);
					}
					// goes to the neighboring city with the highest expected reward
					if(reward > maxReward){
						maxReward = reward;
						nextCity = city;
					}
				}
				action = new Move(nextCity);
			} else {
				action = new Pickup(availableTask);
			}
		}

		if (numActions >= 1) {
			System.out.println("The total profit for "+name+" after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}

	private List<State> generateStates(){
		List<State> states = new ArrayList<>();
		for(Vehicle vehicle: myAgent.vehicles()){
			for(City city: topology.cities()){
				for(City taskDestinationCity: topology.cities()){
					if(!taskDestinationCity.equals(city)) {
						states.add(new State(vehicle, city, taskDestinationCity));
					}
				}
				states.add(new State(vehicle, city, null));
			}
		}
		return states;
	}

	private Map<State, List<Decision>> generateDecisionsForEveryStates(List<State> states){
		HashMap<State, List<Decision>> decisions = new HashMap<>();
		for(State state: states) {
			decisions.put(state, generateDecisionsForState(state));
		}
		return decisions;
	}

	private List<Decision> generateDecisionsForState(State state){
		List<Decision> decisions = new ArrayList<>();
		for(City city: topology.cities()){
			if(!city.equals(state.city)) {
				if(state.taskDestinationCity != null && state.taskDestinationCity.equals(city)) {
					decisions.add(new Decision(city, true));
				}
				if(state.city.hasNeighbor(city)) {
					decisions.add(new Decision(city, false));
				}
			}
		}
		return decisions;
	}

	private Map<StateDecisionKey, Double> generateRewards(List<State> states, Map<State, List<Decision>> decisionsForEveryStates){
		HashMap<StateDecisionKey, Double> rewards = new HashMap<>();

		for(State state: states){
			for(Decision decision: decisionsForEveryStates.get(state)) {
				StateDecisionKey sdk = new StateDecisionKey(state, decision);
				rewards.put(sdk, reward(state, decision));
			}
		}

		return rewards;
	}

	private Map<State, Decision> computeBestDecisions(double epsilon){
		List<State> states = generateStates();
		Map<State, List<Decision>> decisionsForEveryStates = generateDecisionsForEveryStates(states);
		Map<StateDecisionKey, Double> rewards = generateRewards(states, decisionsForEveryStates);

		Map<StateDecisionKey, Double> qValues = new HashMap<>();
		Map<State, Double> vValues = new HashMap<>();
		Map<State, Decision> bestDecisions = new HashMap<>();

		int count = 0;
		double difference;
		Map<State, Double> oldVValues;
		do {
			oldVValues = new HashMap<>(vValues);
			for (State state: states) {
				for (Decision decision: decisionsForEveryStates.get(state)) {
					double transitionSum = 0;
					for (State state2: states) {
						transitionSum += transitionProbability(state, decision, state2) * vValues.getOrDefault(state2, 0.0);
					}
					StateDecisionKey sdk = new StateDecisionKey(state, decision);
					qValues.put(sdk, rewards.get(sdk) + discount*transitionSum);
				}

				double max = 0;
				StateDecisionKey bestSDK = new StateDecisionKey();
				for(Map.Entry<StateDecisionKey, Double> entry : qValues.entrySet()){
					if(entry.getKey().state.equals(state) && entry.getValue() > max){
						max = entry.getValue();
						bestSDK = entry.getKey();
					}
				}
				bestDecisions.put(bestSDK.state, bestSDK.decision);
				vValues.put(bestSDK.state, max);
			}
			difference = computeDifference(oldVValues, vValues);
			count++;
		} while(difference > epsilon);

		System.out.println("Value iteration converged after "+count+" iterations.");

		return bestDecisions;
	}

	private double computeDifference(Map<State, Double> oldVValues, Map<State, Double> vValues){
		double maxDifference = 0;
		for(Map.Entry<State, Double> entry : vValues.entrySet()){
			double difference = Math.abs(entry.getValue() - oldVValues.getOrDefault(entry.getKey(), 0.0));
			if(difference > maxDifference){
				maxDifference = difference;
			}
		}
		return maxDifference;
	}

	private Double reward(State state, Decision decision){

		City startCity = state.city;
		City endCity = decision.destinationCity;

		if(decision.pickup){
			List<City> path = startCity.pathTo(endCity);
			double cost = 0;
			for (City city : path) {
				double distance = startCity.distanceTo(city);
				cost -= distance*state.vehicle.costPerKm();
				startCity = city;
			}
			return taskDistribution.reward(startCity, endCity) - cost;
		} else {
			return -startCity.distanceTo(endCity)*state.vehicle.costPerKm();
		}
	}

	private double transitionProbability(State state1, Decision decision, State state2){
		if(decision.destinationCity.equals(state2.city)){
			return taskDistribution.probability(state2.city, state2.taskDestinationCity);
		}
		return 0;
	}

	private class State {
		private Vehicle vehicle;
		private City city;
		private City taskDestinationCity;

		public State(Vehicle vehicle, City city, City taskDestinationCity) {
			this.vehicle = vehicle;
			this.city = city;
			this.taskDestinationCity = taskDestinationCity;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj != null && obj instanceof State) {
				State s = (State)obj;
				if(taskDestinationCity != null && s.taskDestinationCity != null) {
					return vehicle.equals(s.vehicle) && city.equals(s.city) && taskDestinationCity.equals(s.taskDestinationCity);
				} else if (taskDestinationCity == null && s.taskDestinationCity == null){
					return vehicle.equals(s.vehicle) && city.equals(s.city);
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			if(taskDestinationCity != null) {
				return Objects.hash(vehicle.hashCode(), city.hashCode(), taskDestinationCity.hashCode());
			}else{
				return Objects.hash(vehicle.hashCode(), city.hashCode());
			}
		}
	}

	private class Decision {
		private City destinationCity;
		private boolean pickup;

		public Decision(City city, boolean pickup) {
			this.destinationCity = city;
			this.pickup = pickup;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj != null && obj instanceof Decision) {
				Decision d = (Decision)obj;
				return destinationCity.equals(d.destinationCity) && pickup == d.pickup;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(destinationCity.hashCode(), pickup);
		}
	}

	private class StateDecisionKey {
		private State state;
		private Decision decision;

		public StateDecisionKey(){
		}

		public StateDecisionKey(State state, Decision decision) {
			this.state = state;
			this.decision = decision;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj != null && obj instanceof StateDecisionKey) {
				StateDecisionKey sdk = (StateDecisionKey)obj;
				return state.equals(sdk.state) && decision.equals(sdk.decision);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(state.hashCode(), decision.hashCode());
		}
	}
}
