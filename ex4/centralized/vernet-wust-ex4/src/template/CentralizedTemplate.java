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

        int nb_vehicles = vehicles.size();
        int nb_tasks = tasks.size();

        Variable X = new Variable(vehicles, tasks);
        Domain D = new Domain(vehicles, tasks);
        Constraint C = new Constraint();

        System.out.println("Agent " + agent.id() + " has tasks " + tasks);

        /*
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        */

        List<Plan> plans = optimalPlans(X, D, C);

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }

    private List<Plan> optimalPlans(Variable X, Domain D, Constraint C){
        Assignment solution = stochasticLocalSearch(X, D, C);
        List<Plan> plans = plansFromVariableAssignment(solution);

        return plans;
    }

    private Assignment stochasticLocalSearch(Variable X, Domain D, Constraint C){
        Assignment A = selectInitialSolution(X, D, C);
        return A;
    }

    private Assignment selectInitialSolution(Variable X, Domain D, Constraint C) {
        List<Task> tasks = D.nextTask;
        for(int i = 0; i < tasks.size() - 1; i++){
            X.nextTask_t.put(tasks.get(i), tasks.get(i+1));
            X.time.put(tasks.get(i), i + 1);
            X.vehicle.put(tasks.get(i), D.vehicle.get(0));
        }
        X.nextTask_t.put(tasks.get(tasks.size() - 1), null);
        X.time.put(tasks.get(tasks.size() - 1), tasks.size());

        X.nextTask_v.put(D.vehicle.get(0), tasks.get(0));

        return new Assignment(X, D, C);
    }

    //TODO
    private List<Plan> plansFromVariableAssignment(Assignment A){
        return null;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    private class Assignment{

        private Variable X;
        private Domain D;
        private Constraint C;
        private double cost;

        public Assignment(Variable X, Domain D, Constraint C){
            assert(isValid());
            this.X = X;
            this.D = D;
            this.C = C;
            this.cost = cost();
        }

        private boolean isValid(){

            boolean b1 = C.c1(X.nextTask_t);
            boolean b2 = C.c2(X.nextTask_v, X.time);
            boolean b3 = C.c3(X.nextTask_t, X.time);
            boolean b4 = C.c4(X.nextTask_v, X.vehicle);
            boolean b5 = C.c5(X.nextTask_t, X.vehicle);
            boolean b6 = C.c6(X.nextTask_t, X.nextTask_v, D.nextTask);
            boolean b7 = C.c7(X.vehicle);

            return b1 && b2 && b3 && b4 && b5 && b6 && b7;
        }

        //TODO: the cost function is most likely not adapted for vehicles carrying several tasks
        private double cost(){
            double cost = 0;

            for(Task task: D.nextTask){
                Task nextTask = X.nextTask_t.get(task);
                if(nextTask != null) {
                    double distance = task.deliveryCity.distanceTo(nextTask.pickupCity);
                    double length = nextTask.pickupCity.distanceTo(nextTask.deliveryCity);
                    cost += (distance + length) * X.vehicle.get(task).costPerKm();
                }
            }

            for(Vehicle vehicle: D.vehicle){
                Task nextTask = X.nextTask_v.get(vehicle);
                if(nextTask != null) {
                    double distance = vehicle.homeCity().distanceTo(nextTask.pickupCity);
                    double length = nextTask.pickupCity.distanceTo(nextTask.deliveryCity);
                    cost += (distance + length) * vehicle.costPerKm();
                }
            }

            return cost;
        }

    }

    private class Variable {

        private Map<Task, Task> nextTask_t;
        private Map<Vehicle, Task> nextTask_v;
        private Map<Task, Integer> time;
        private Map<Task, Vehicle> vehicle;

        public Variable(List<Vehicle> vehicles, TaskSet tasks) {

            this.nextTask_t = new HashMap<>(tasks.size());
            this.nextTask_v = new HashMap<>(vehicles.size());
            this.time = new HashMap<>(tasks.size());
            this.vehicle = new HashMap<>(tasks.size());

            for(Task task: tasks){
                nextTask_t.put(task, null);
                time.put(task, null);
                vehicle.put(task, null);
            }

            for(Vehicle vehicle: vehicles){
                nextTask_v.put(vehicle, null);
            }
        }
    }

    private class Domain {

        private List<Task> nextTask;
        private List<Integer> time;
        private List<Vehicle> vehicle;

        public Domain(List<Vehicle> vehicles, TaskSet tasks){
            this.nextTask = new ArrayList<>(tasks);
            this.time = IntStream.rangeClosed(1, tasks.size()).boxed().collect(Collectors.toList());
            this.vehicle = new ArrayList<>(vehicles);
        }

    }

    private class Constraint {

        //nextTask(task) != task
        private boolean c1(Map<Task, Task> nextTask_t) {

            for (Map.Entry<Task, Task> entry : nextTask_t.entrySet()) {
                if(entry.getValue().equals(entry.getKey())){
                    return false;
                }
            }

            return true;
        }

        //first task of every vehicle is at time 1
        private boolean c2(Map<Vehicle, Task> nextTask_v, Map<Task, Integer> time) {

            for (Task task : nextTask_v.values()) {
                if (time.get(task) != 1) {
                    return false;
                }
            }
            return true;
        }

        //time of nextTask = time of currentTask + 1
        private boolean c3(Map<Task, Task> nextTask_t, Map<Task, Integer> time) {

            for (Map.Entry<Task, Task> entry : nextTask_t.entrySet()) {
                    if(time.get(entry.getValue()) != time.get(entry.getKey()) + 1)
                        return false;
            }
            return true;
        }

        //redundant declaration of vehicle(task) = task(vehicle)
        private boolean c4(Map<Vehicle, Task> nextTask_v, Map<Task, Vehicle> vehicle) {

            for (Map.Entry<Vehicle, Task> entry : nextTask_v.entrySet()) {
                if(!vehicle.get(entry.getValue()).equals(entry.getKey())) {
                    return false;
                }
            }
            return true;
        }

        //nextTask(t1) = t2 -> vehicle(t1) = vehicle(t2)
        private boolean c5(Map<Task, Task> nextTask_t, Map<Task, Vehicle> vehicle) {

            for (Map.Entry<Task, Task> entry : nextTask_t.entrySet()) {
                if(!vehicle.get(entry.getValue()).equals(vehicle.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }

        //nextTask(t&v) contain all tasks + nbVehicles times a null argument
        private boolean c6(Map<Task, Task> nextTask_t, Map<Vehicle, Task> nextTask_v, List<Task> tasks) {

            Set<Task> tasksToCheck = new HashSet<>();
            tasksToCheck.addAll(tasks);

            int nullCount = 0;

            for (Vehicle v : nextTask_v.keySet()) {
                if (nextTask_v.get(v) == null) {
                    nullCount++;
                } else if (tasks.contains(nextTask_v.get(v))) {
                    tasksToCheck.remove(nextTask_v.get(v));
                }
            }

            for (Task t : nextTask_t.keySet()) {
                if (nextTask_t.get(t) == null) {
                    nullCount++;
                } else if (tasks.contains(nextTask_t.get(t))) {
                    tasksToCheck.remove(nextTask_t.get(t));
                }
            }

            return nullCount == nextTask_v.size() && tasksToCheck.size() == 0;
        }

        //No task will exceed the capacity of a vehicle
        private boolean c7(Map<Task, Vehicle> vehicle) {

            Map<Vehicle, Integer> load = new HashMap<>();

            for (Map.Entry<Task, Vehicle> entry : vehicle.entrySet()) {
                Task t = entry.getKey();
                Vehicle v = entry.getValue();
                load.put(v, load.get(v) + t.weight);
                if (load.get(v) > v.capacity()) {
                    return false;
                }
            }

            return true;
        }
    }
}
