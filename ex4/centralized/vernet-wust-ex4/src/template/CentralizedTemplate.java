package template;

//the list of imports
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sun.source.doctree.ValueTree;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private Random rand = new Random();

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        Variable X = new Variable(vehicles);
        Domain D = new Domain(vehicles, tasks);
        Constraint C = new Constraint();

        System.out.println("Agent " + agent.id() + " has tasks " + tasks);

        List<Plan> plans = optimalPlans(X, D, C);

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }

    private List<Plan> optimalPlans(Variable X, Domain D, Constraint C){
        Assignment solution = stochasticLocalSearchTimeBased(X, D, C, timeout_plan);
        List<Plan> plans = plansFromVariableAssignment(solution);

        return plans;
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

    private Assignment stochasticLocalSearchTimeBased(Variable X, Domain D, Constraint C, double time){
        Assignment A = selectInitialSolution(X, D, C);
        Assignment localBestA = new Assignment(A);
        Assignment globalBestA = new Assignment(A);
        List<Assignment> oldAssignments = new LinkedList<>();

        double globalBestCost = Double.MAX_VALUE;
        int numberOfTries = 3;
        int timeMargin = 3000; //ms

        for(int i=1; i<=numberOfTries; i++) {
            long time_start = System.currentTimeMillis();
            double localBestCost = Double.MAX_VALUE;
            int maxIterationsOnAssignment = 50000;
            int maxBacktrackIterations = 20;
            int nbBacktracks = 0;
            int countCurrentAssignmentIterations = 0;

            while (System.currentTimeMillis() - time_start < time/numberOfTries - timeMargin/numberOfTries) {
                Assignment A_old = new Assignment(A);
                Set<Assignment> N = chooseNeighbors(A_old, D, 0.4);
                A = localChoice(A_old, N);
                double A_cost = A.cost();

                if (A_cost < localBestCost) {
                    localBestA = A;
                    localBestCost = A_cost;
                    nbBacktracks = 0;
                    oldAssignments.add(A);

                    System.out.println("Best cost: " + A_cost + " from "+ oldAssignments.size()+" Solutions in try: "+i);
                }

                countCurrentAssignmentIterations++;

                if (countCurrentAssignmentIterations > maxIterationsOnAssignment) {
                    nbBacktracks++;
                    if (nbBacktracks > maxBacktrackIterations) {
                        maxIterationsOnAssignment += 20000;
                        maxBacktrackIterations = (int) Math.ceil(maxBacktrackIterations / 2);
                        nbBacktracks = 0;
                    } else {
                        int index = oldAssignments.size() - nbBacktracks;
                        if (index <= 0) {
                            A = oldAssignments.get(0);
                        } else {
                            A = oldAssignments.get(index);
                        }

                        countCurrentAssignmentIterations = 0;
                        System.out.println("Backtracked: " + nbBacktracks);
                    }
                }
            }

            System.out.println("Best cost in try "+i+" found to be: " + localBestCost);

            oldAssignments.clear();
            A = selectInitialSolution(X, D, C);

            if(localBestCost < globalBestCost){
                globalBestCost = localBestCost;
                globalBestA = localBestA;
            }
        }
        System.out.println("Global Best Cost: " + globalBestCost);

        return globalBestA;
    }

    private Assignment localChoice(Assignment A_old, Set<Assignment> N){

        double bestCost = A_old.cost();
        Assignment bestAssignment = A_old;
        for(Assignment assignment: N){
            if(assignment.cost() < bestCost){
                bestCost = assignment.cost();
                bestAssignment = assignment;
            } else if(assignment.cost() == bestCost){ //if the cost is the same, we randomly assign a new best assignment
                if(rand.nextBoolean()){
                    bestCost = assignment.cost();
                    bestAssignment = assignment;
                }
            }
        }

        return bestAssignment;
    }

    private Set<Assignment> chooseNeighbors(Assignment A_old, Domain D, double probability){

        Set<Assignment> N = new HashSet<>();

        if(rand.nextDouble() < probability){
            N.add(A_old);
            return N;
        }


        Vehicle randomVehicle = D.vehicles.get(rand.nextInt(D.vehicles.size()));
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
            int i = rand.nextInt(randomVehicleTasks.size());
            int j = rand.nextInt(randomVehicleTasks.size());
            Assignment A3 = changingTaskOrder(A_old, randomVehicle, i, j);
            if (A3.isValid() && !A3.equals(A_old)) {
                N.add(A3);
            }
        }

        return N;
    }

    public Assignment changingTaskOrder(Assignment A_old, Vehicle randomVehicle, int i, int j){

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
            Task randomTask = randomVehicleTasks.get(rand.nextInt(randomVehicleTasks.size()));
            randomVehicleTasks.removeAll(Collections.singleton(randomTask));
            if(vehicleTasks.isEmpty()){
                vehicleTasks.add(0, randomTask);
                vehicleTasks.add(0, randomTask);
            } else {
                vehicleTasks.add(rand.nextInt(vehicleTasks.size()), randomTask);
                vehicleTasks.add(rand.nextInt(vehicleTasks.size()), randomTask);
            }

            A.X.nextAction.put(randomVehicle, randomVehicleTasks);
            A.X.nextAction.put(vehicle, vehicleTasks);
            return A;
        }

        return A_old;
    }

    private Assignment selectInitialSolution(Variable X, Domain D, Constraint C) {

        //Closest Tasks
        List<Task> tasks = new ArrayList<>(D.tasks);
        for(Vehicle vehicle: D.vehicles){
            List<Task> initialTasks = new ArrayList<>();
            List<Task> carriedTasks = new ArrayList<>();
            double load = 0;
            for(Task task: D.tasks){
                if(vehicle.homeCity().equals(task.pickupCity)){
                    load += task.weight;
                    if(load > vehicle.capacity()){
                        for(Task carriedTask: carriedTasks){
                            initialTasks.add(carriedTask);
                        }
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

            for(Task task: carriedTasks){
                initialTasks.add(task);
            }
            X.nextAction.put(vehicle, initialTasks);
        }

        int nbVehicles = D.vehicles.size();
        int vehicleIndex = 0;

        for(Task task: tasks){
            if (vehicleIndex == nbVehicles)
                vehicleIndex = 0;
            X.nextAction.get(D.vehicles.get(vehicleIndex)).add(task);
            X.nextAction.get(D.vehicles.get(vehicleIndex)).add(task);
            vehicleIndex++;
        }

        return new Assignment(X, D, C);
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
            this.D = A.D;
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
                    double vehicleCost = vehicle.homeCity().distanceTo(vehicleTasks.get(0).pickupCity);
                    City currentCity = vehicleTasks.get(0).pickupCity;

                    List<Task> tasksToDeliver = new ArrayList<>();
                    tasksToDeliver.add(vehicleTasks.get(0));

                    for (int i = 1; i < vehicleTasks.size(); i++) {
                        Task task = vehicleTasks.get(i);
                        if (tasksToDeliver.contains(task)) {
                            vehicleCost += currentCity.distanceTo(task.deliveryCity);
                            currentCity = task.deliveryCity;
                            tasksToDeliver.remove(task);
                        } else {
                            vehicleCost += currentCity.distanceTo(task.pickupCity);
                            currentCity = task.pickupCity;
                            tasksToDeliver.add(task);
                        }
                    }
                    cost += vehicleCost*vehicle.costPerKm();
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
            this.nextAction = new HashMap<>(X.nextAction);
        }
    }

    private static class Domain {

        private List<Task> tasks;
        private List<Vehicle> vehicles;

        private Domain(List<Vehicle> vehicles, TaskSet tasks){
            this.vehicles = vehicles;
            this.tasks = new ArrayList<>(tasks);
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