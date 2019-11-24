package template;

//the list of imports
import java.io.File;
import java.util.*;

import logist.LogistSettings;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;

    private static long timeout_auction;
    private static long timeout_plan;

    private Assignment currentA;
    private Assignment nextA;

    private List<Long> ourBids;
    private List<Long> opponentBids;
    private List<Long> wonBids;
    private List<Long> opponentWonBids;
    private double currentUtility;
    private double newCost;
    private boolean opponentSpeculates;


    @Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
            timeout_auction = ls.get(LogistSettings.TimeoutKey.BID);
            timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

        long seed = -9019554669489983951L * agent.vehicles().get(0).homeCity().hashCode() * agent.id();
        this.random = new Random(seed);

        this.currentUtility = 0;
        this.newCost = 0;
        this.wonBids = new ArrayList<>();
        this.ourBids = new ArrayList<>();
        this.opponentBids = new ArrayList<>();
        this.opponentWonBids = new ArrayList<>();
        this.opponentSpeculates = true;

        Variable X = new Variable(agent.vehicles());
        Domain D = new Domain(agent.vehicles());
        Constraint C = new Constraint();
        this.currentA = new Assignment(X,D,C);
    }

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

        if(bids.length > 1) {
            int opponentId = 0;
            if (bids[0].equals(ourBids.get(ourBids.size() - 1))) {
                opponentId = 1;
            }

            opponentBids.add(bids[opponentId]);
            if(winner != agent.id()){
                opponentWonBids.add(bids[opponentId]);
            }
        }

        if(winner == agent.id()){
            this.wonBids.add(bids[winner]);
            long totalWonBids = wonBids.stream().mapToLong(Long::longValue).sum();
            this.currentUtility = totalWonBids - this.newCost;
            this.currentA = this.nextA;
        }
    }

	@Override
	public Long askPrice(Task task) {

        System.out.println("    ----- Task No.: "+(ourBids.size()+1)+" -----     ");
        System.out.println(agent.name()+" - Number of tasks taken: "+wonBids.size());
        System.out.println(agent.name()+" - Utility: " + this.currentUtility);
        //Checks if at least one vehicle can pickup the task
        boolean canCarryTask = false;
        for(Vehicle vehicle : currentA.D.vehicles) {
            if (vehicle.capacity() > task.weight) {
                canCarryTask = true;
                break;
            }
        }

        if(!canCarryTask) {
            return null;
        }

        //Add task to domain and search for good solution
        nextA = initialAssignmentWithNewTask(currentA, task);
        nextA = stochasticLocalSearchTimeBased(nextA, timeout_auction);

        this.newCost = nextA.cost();
        long marginalCost = (long) (newCost - currentA.cost());
        System.out.println(agent.name()+" - New cost: "+nextA.cost()+", current cost: "+currentA.cost()+" -> marginal cost: "+marginalCost);

        long bid;
        //if(agent.name().equals("auction-main-80")) {
            bid = speculate(task, marginalCost);
        //}else{
        //    bid = Math.max(0, marginalCost);
        //}

        ourBids.add(bid);
        System.out.println(agent.name()+" - Bid: " + bid);

        return ourBids.get(ourBids.size()-1);
	}

    private long speculate(Task newTask, long marginalCost){

        double speculation = 0.3; // half the reward for the actual cost -> enough? (only on first task)

        if(topology.toString().contains("Amsterdam")) //dangerous to speculate too much on NL
            speculation = 0.5;

        double bid = marginalCost * speculation;

        double distanceTask = newTask.pickupCity.distanceTo(newTask.deliveryCity) * currentA.D.vehicles.get(0).costPerKm();

        //Average opponent bids
        long avgBidOpponent = opponentBids.stream().mapToLong(Long::longValue).sum();
        if(opponentBids.size() > 0) {
            avgBidOpponent = avgBidOpponent / opponentBids.size();
        }
        System.out.println(agent.name() + " - Opponent average bid: " + avgBidOpponent);

        //                                *** Early game ***
        //Try to get 1st & 2nd task with deficit as it makes the total distribution more efficient
        //Include more? Dangerous bc could only be 5 tasks
        if(ourBids.size() < 2) {
            if (wonBids.size() == 1) {
                speculation += 0.2; // go slightly higher for second task
                bid = marginalCost * speculation;
            }
            for (Vehicle v : currentA.D.vehicles) {
                if (v.homeCity().equals(newTask.pickupCity)) {
                    bid = bid * 0.8; //get it even cheaper if a vehicle starts at PU location
                }
            }
        }

        //                                  *** Mid Game ***
        //Try and find a fitting bid according to average of opponent
        else if(ourBids.size() < 4) {
            //Got more tasks than opponent, opponent likely goes with marginal cost strategy
            if(wonBids.size() > opponentWonBids.size()) {
                opponentSpeculates = false;
                System.out.println(agent.name()+" - Opponent doesn't speculate or not as much as us");
            }

            //if so: abort speculation and try to get close to opponents bid but stay below
            double deficitToCatchUp = -Math.min(currentUtility, 0); //the goal is to break even after 4 tasks so that we can get a positive utility before entering late game
            if(!opponentSpeculates){
                bid = Math.max(marginalCost + deficitToCatchUp/(4 - ourBids.size()), avgBidOpponent - distanceTask);// hopefully lower than opponents bid (except lucky task)
            }else{
                if(wonBids.size() < opponentWonBids.size()){ //opponent speculates even lower: try and get tasks at low price to make opponent keep deficit
                    bid = marginalCost + deficitToCatchUp/(4 - ourBids.size());
                }
            }
        }

        //                              *** Late Game ***
        // Make up for early deficit, opponent hopefully has very few tasks
        else {
            double deficitToCatchUp = -Math.min(currentUtility, 0);
            if(opponentWonBids.size() > wonBids.size()){
                bid = marginalCost + deficitToCatchUp*0.2;
            }else{
                speculation = 1.2;
                bid = Math.max(marginalCost * speculation, marginalCost + deficitToCatchUp);
                bid = Math.max(bid, avgBidOpponent - distanceTask); //reduce again to have bigger chance to get it
            }
        }

        return (long) Math.max(0, bid);
    }

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		System.out.println("Agent " + agent.name() + " has "+ tasks.size()+" tasks.");

		//try again to find better plan within timeout_plan
		Assignment finalA = new Assignment(new Variable(vehicles), new Domain(vehicles, tasks), new Constraint());
        finalA = selectInitialSolution(finalA.X, finalA.D, finalA.C);
        finalA = stochasticLocalSearchTimeBased(finalA, timeout_plan);

        if(currentA.cost() < finalA.cost()) {
            finalA = currentA;
            convertAssignmentTasks(finalA, tasks);
        }

        List<Plan> plans = plansFromVariableAssignment(finalA);

        long totalWonBids = wonBids.stream().mapToLong(Long::longValue).sum();
        System.out.println(agent.name()+" - Cost of plan: "+finalA.cost()+", total reward: "+totalWonBids+", utility: "+(totalWonBids-finalA.cost()));

        return plans;
	}

    private void convertAssignmentTasks(Assignment A, TaskSet tasks){

        List<Task> tasksList = new ArrayList<>(tasks);
        for(Map.Entry<Vehicle, List<Task>> entry: A.X.nextAction.entrySet()){
            List<Task> newTasks = new ArrayList<>();
            for(Task task: entry.getValue()){
                for(Task correctTask: tasksList){
                    if(correctTask.id == task.id){
                        newTasks.add(tasksList.get(correctTask.id));
                    }
                }
            }
            A.X.nextAction.put(entry.getKey(), newTasks);
        }
    }

    private List<Plan> plansFromVariableAssignment(Assignment A){

        Map<Vehicle, Plan> vehiclePlans = new HashMap<>();

        for(Map.Entry<Vehicle, List<Task>> entry: A.X.nextAction.entrySet()){
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            Plan plan = new Plan(currentCity);
            List<Task> carriedTasks = new ArrayList<>();
            for(Task task: entry.getValue()){
                if(!carriedTasks.contains(task)) {
                    for (City city : currentCity.pathTo(task.pickupCity)) {
                        plan.appendMove(city);
                    }
                    currentCity = task.pickupCity;
                    plan.appendPickup(task);
                    carriedTasks.add(task);
                }else{
                    for (City city : currentCity.pathTo(task.deliveryCity)) {
                        plan.appendMove(city);
                    }
                    currentCity = task.deliveryCity;
                    plan.appendDelivery(task);
                    carriedTasks.remove(task);
                }
            }
            vehiclePlans.put(vehicle, plan);
        }

        List<Plan> plans = new ArrayList<>();
        for(Vehicle vehicle: A.D.vehicles){
            plans.add(vehiclePlans.get(vehicle));
        }

        return plans;
    }

    private Assignment stochasticLocalSearchTimeBased(Assignment A, long time){
        Assignment localBestA = new Assignment(A);
        Assignment globalBestA = new Assignment(A);
        List<Assignment> oldAssignments = new ArrayList<>();

        double globalBestCost = Double.MAX_VALUE;
        int numberOfTries = 3;
        long timeMargin = (long) Math.min(4000, 0.1*time);
        long timePerTry = Math.round((double) (time-timeMargin)/numberOfTries); //ms

        for(int i = 0; i < numberOfTries; i++) {
            long time_start = System.currentTimeMillis();
            double localBestCost = Double.MAX_VALUE;
            int maxIterationsOnAssignment = 50000;
            int maxBacktrackIterations = 20;
            int nbBacktracks = 0;
            int countCurrentAssignmentIterations = 0;
            Assignment newA = new Assignment(A);

            while (System.currentTimeMillis() - time_start < timePerTry) {
                Assignment A_old = new Assignment(newA);
                Set<Assignment> N = chooseNeighbors(A_old, A.D);
                newA = localChoice(A_old, N);
                double newACost = newA.cost();

                if (newACost < localBestCost) {
                    localBestA = newA;
                    localBestCost = newACost;
                    nbBacktracks = 0;
                    countCurrentAssignmentIterations = 0;
                    oldAssignments.add(newA);
                    //System.out.println("Best cost: " + A_cost + " from "+ oldAssignments.size()+" Solutions in try: "+i);
                }

                countCurrentAssignmentIterations++;

                if (countCurrentAssignmentIterations > maxIterationsOnAssignment) {
                    nbBacktracks++;
                    if (nbBacktracks > maxBacktrackIterations) {
                        maxIterationsOnAssignment += 20000;
                        maxBacktrackIterations = maxBacktrackIterations / 2;
                        nbBacktracks = 0;
                    } else {
                        int index = oldAssignments.size() - nbBacktracks;
                        if (index <= 0) {
                            newA = oldAssignments.get(0);
                        } else {
                            newA = oldAssignments.get(index);
                        }

                        countCurrentAssignmentIterations = 0;
                        //System.out.println("Backtracked: " + nbBacktracks);
                    }
                }
            }

            //System.out.println(agent.name()+" - Best cost in try "+(i+1)+" found to be: " + localBestCost);

            oldAssignments.clear();

            if(localBestCost < globalBestCost){
                globalBestCost = localBestCost;
                globalBestA = localBestA;
            }
        }

        System.out.println(agent.name()+" - Global best cost: " + globalBestCost);

        return globalBestA;
    }

    //assign tasks to vehicles with home city equals to pickup location
    private Assignment selectInitialSolution(Variable X, Domain D, Constraint C) {

        List<Task> tasks = new ArrayList<>(D.tasks);
        for(Vehicle vehicle: D.vehicles){
            List<Task> initialTasks = new ArrayList<>();
            List<Task> carriedTasks = new ArrayList<>();
            double load = 0;
            for(Task task: D.tasks){
                if(vehicle.homeCity().equals(task.pickupCity)){
                    load += task.weight;
                    if(load > vehicle.capacity()){
                        initialTasks.addAll(carriedTasks);
                        load = task.weight;
                        initialTasks.add(task);
                        carriedTasks = new ArrayList<>();
                        carriedTasks.add(task);
                    } else {
                        load += task.weight;
                        carriedTasks.add(task);
                        initialTasks.add(task);
                    }
                    tasks.remove(task);
                }
            }

            initialTasks.addAll(carriedTasks);
            X.nextAction.put(vehicle, initialTasks);
        }

        int nbVehicles = D.vehicles.size();
        int vehicleIndex = 0;

        for(Task task: tasks){
            X.nextAction.get(D.vehicles.get(vehicleIndex)).add(task);
            X.nextAction.get(D.vehicles.get(vehicleIndex)).add(task);
            vehicleIndex = (vehicleIndex + 1) % nbVehicles;
        }

        return new Assignment(X, D, C);
    }

    private Assignment initialAssignmentWithNewTask(Assignment A, Task newTask) {

        Assignment newA = new Assignment(A);

        Domain D = new Domain(A.D);
        newA.D.tasks.add(newTask);

        //add the new Task to the first vehicle
        newA.X.nextAction.get(D.vehicles.get(0)).add(newTask);
        newA.X.nextAction.get(D.vehicles.get(0)).add(newTask);

        return newA;
    }

    private Assignment localChoice(Assignment A_old, Set<Assignment> N){

        double bestCost = A_old.cost();
        List<Assignment> bestAssignments = new ArrayList<>();
        bestAssignments.add(A_old);
        for(Assignment assignment: N){
            if(assignment.cost() < bestCost){
                bestCost = assignment.cost();
                bestAssignments.clear();
                bestAssignments.add(assignment);
            } else if(assignment.cost() == bestCost){
                bestAssignments.add(assignment);
            }
        }

        return bestAssignments.get(random.nextInt(bestAssignments.size()));
    }

    private Set<Assignment> chooseNeighbors(Assignment A_old, Domain D){

        Set<Assignment> N = new HashSet<>();

        Vehicle randomVehicle = D.vehicles.get(random.nextInt(D.vehicles.size()));
        for(Vehicle vehicle: D.vehicles){
            if(!vehicle.equals(randomVehicle)){
                Assignment A2 = transferringTask(A_old, randomVehicle, vehicle);
                if(A2.isValid() && !A2.equals(A_old)){
                    N.add(A2);
                }
            }
        }

        List<Task> randomVehicleTasks = A_old.X.nextAction.get(randomVehicle);
        if(!randomVehicleTasks.isEmpty()) {
            int i = random.nextInt(randomVehicleTasks.size());
            int j = random.nextInt(randomVehicleTasks.size());
            Assignment A3 = changingTaskOrder(A_old, randomVehicle, i, j);
            if (A3.isValid() && !A3.equals(A_old)) {
                N.add(A3);
            }
        }

        return N;
    }

    private Assignment changingTaskOrder(Assignment A_old, Vehicle randomVehicle, int i, int j){

        Assignment A = new Assignment(A_old);
        List<Task> randomVehicleTasks = new ArrayList<>(A_old.X.nextAction.get(randomVehicle));
        if(!randomVehicleTasks.get(i).equals(randomVehicleTasks.get(j))) {
            Collections.swap(randomVehicleTasks, i, j);
            A.X.nextAction.put(randomVehicle, randomVehicleTasks);
            return A;
        }

        return A_old;
    }

    //transfer a random task from a random vehicle to another vehicle as its first task
    private Assignment transferringTask(Assignment A_old, Vehicle randomVehicle, Vehicle vehicle) {

        Assignment A = new Assignment(A_old);
        List<Task> randomVehicleTasks = new ArrayList<>(A_old.X.nextAction.get(randomVehicle));
        List<Task> vehicleTasks = new ArrayList<>(A_old.X.nextAction.get(vehicle));

        //a vehicle without assigned tasks cannot transfer tasks, therefore we return the old assignment in order to discard it later on
        if(!randomVehicleTasks.isEmpty()) {
            Task randomTask = randomVehicleTasks.get(random.nextInt(randomVehicleTasks.size()));
            randomVehicleTasks.removeAll(Collections.singleton(randomTask));
            if(vehicleTasks.isEmpty()){
                vehicleTasks.add(0, randomTask);
                vehicleTasks.add(0, randomTask);
            } else {
                vehicleTasks.add(random.nextInt(vehicleTasks.size()), randomTask);
                vehicleTasks.add(random.nextInt(vehicleTasks.size()), randomTask);
            }

            A.X.nextAction.put(randomVehicle, randomVehicleTasks);
            A.X.nextAction.put(vehicle, vehicleTasks);
            return A;
        }

        return A_old;
    }

    private class Assignment{

        private Variable X;
        private Domain D;
        private Constraint C;

        private Assignment(Variable X, Domain D, Constraint C){
            this.X = X;
            this.D = D;
            this.C = C;
        }

        private Assignment(Assignment A){
            this.X = new Variable(A.X);
            this.D = new Domain(A.D);
            this.C = A.C;
        }

        private boolean isValid(){

            boolean b1 = C.c1(X, D);
            boolean b2 = C.c2(X, D);
            boolean b3 = C.c3(X, D);

            return b1 && b2 && b3;
        }

        private double cost(){
            double cost = 0;

            for(Map.Entry<Vehicle, List<Task>> entry: X.nextAction.entrySet()){
                Vehicle vehicle = entry.getKey();
                List<Task> vehicleTasks = entry.getValue();

                if(!vehicleTasks.isEmpty()) {
                    City currentCity = vehicleTasks.get(0).pickupCity;
                    double vehicleDistance = vehicle.homeCity().distanceTo(currentCity);

                    List<Task> tasksToDeliver = new ArrayList<>();
                    tasksToDeliver.add(vehicleTasks.get(0));

                    for (int i = 1; i < vehicleTasks.size(); i++) {
                        Task task = vehicleTasks.get(i);
                        if (tasksToDeliver.contains(task)) {
                            vehicleDistance += currentCity.distanceTo(task.deliveryCity);
                            currentCity = task.deliveryCity;
                            tasksToDeliver.remove(task);
                        } else {
                            vehicleDistance += currentCity.distanceTo(task.pickupCity);
                            currentCity = task.pickupCity;
                            tasksToDeliver.add(task);
                        }
                    }
                    cost += vehicleDistance*vehicle.costPerKm();
                }
            }

            return Math.round(cost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Objects.hash(X), Objects.hash(D), Objects.hash(C));
        }

    }

    private static class Variable {

        private Map<Vehicle, List<Task>> nextAction;

        private Variable(List<Vehicle> vehicles) {

            this.nextAction = new HashMap<>(vehicles.size());

            for(Vehicle vehicle: vehicles){
                nextAction.put(vehicle, new ArrayList<>());
            }
        }

        private Variable(Variable X){
            HashMap<Vehicle, List<Task>> copy = new HashMap<>();
            for (Map.Entry<Vehicle, List<Task>> entry : X.nextAction.entrySet())
            {
                copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            this.nextAction = copy;
        }
    }

    private static class Domain {

        private List<Task> tasks;
        private List<Vehicle> vehicles;

        private Domain(List<Vehicle> vehicles){
            this.vehicles = vehicles;
            this.tasks = new ArrayList<>();
        }

        private Domain(List<Vehicle> vehicles, TaskSet tasks){
            this.vehicles = vehicles;
            this.tasks = new ArrayList<>(tasks);
        }

        private Domain(Domain D){
            this.vehicles = new ArrayList<>(D.vehicles);
            this.tasks = new ArrayList<>(D.tasks);
        }
    }

    private static class Constraint {

        //same tasks are not assigned to different vehicles
        private boolean c1(Variable X, Domain D){

            Set<Task> tasks = new HashSet<>(D.tasks);

            for(Map.Entry<Vehicle, List<Task>> entry: X.nextAction.entrySet()){
                Set<Task> vehicleTasks = new HashSet<>(entry.getValue());
                for(Task vehicleTask: vehicleTasks) {
                    if(!tasks.remove(vehicleTask)){
                        return false;
                    }
                }
            }
            return tasks.isEmpty();
        }

        //each task is picked up and delivered only once
        private boolean c2(Variable X, Domain D){

            for(Map.Entry<Vehicle, List<Task>> entry: X.nextAction.entrySet()) {
                List<Task> vehicleTasks = new ArrayList<>(entry.getValue());
                while(!vehicleTasks.isEmpty()){
                    int listSize = vehicleTasks.size();
                    Task vehicleTask = vehicleTasks.remove(0);
                    vehicleTasks.removeAll(Collections.singleton(vehicleTask));
                    if(listSize - vehicleTasks.size() != 2){
                        return false;
                    }
                }
            }
            return true;
        }

        //load does not exceed capacity
        private boolean c3(Variable X, Domain D){

            for(Map.Entry<Vehicle, List<Task>> entry: X.nextAction.entrySet()) {

                List<Task> vehicleTasks = entry.getValue();
                List<Task> carriedTasks = new ArrayList<>();

                double load = 0;
                int capacity = entry.getKey().capacity();

                for(Task task: vehicleTasks){
                    if(carriedTasks.contains(task)){
                        carriedTasks.remove(task);
                        load -= task.weight;
                    } else {
                        carriedTasks.add(task);
                        load += task.weight;
                        if (load > capacity) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
}
