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

    private List<Plan> optimalPlans(Variable X, Domain D, Constraint C){
        Assignment solution = stochasticLocalSearch(X, D, C);
        List<Plan> plans = plansFromVariableAssignment(solution);

        return plans;
    }

    private Assignment stochasticLocalSearch(Variable X, Domain D, Constraint C){
        Assignment A = selectInitialSolution(X, D, C);
        return A;
    }
    //TODO adapt to make better initial assignment

    private Assignment selectInitialSolution(Variable X, Domain D, Constraint C) {
        List<Task> tasks = D.tasks;
        List<Vehicle> vehicles = D.vehicles;

        //distribution
        for(Task t : tasks)
            X.assignedTasks.get(vehicles.iterator().next()).add(t);

                //ordering
        for(Vehicle v: vehicles){
            List<Task> ts = X.assignedTasks.get(v);

            List<Task> pu = new ArrayList<>();
            List<Task> del = new ArrayList<>();
            for(Task t : ts) {
                pu.add(t);
                pu.add(null);
                del.add(null);
                del.add(t);
            }
            X.pickUp.put(v, pu);
            X.delivery.put(v, del);

            //moveOrder
            List<City> order = new ArrayList<>();
            for(Task t : ts) {
                order.add(t.pickupCity);
                order.add(t.deliveryCity);
            }
            X.moveOrder.put(v, order);
        }

        return new Assignment(X, D, C);
    }
    //TODO

    private List<Plan> plansFromVariableAssignment(Assignment A){
        return null;
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

        private double cost(){
            double cost = 0;

            for(Vehicle v : D.vehicles){
                Task firstTask = X.pickUp.get(v).get(0);
                if (firstTask != null){
                    double distance = v.homeCity().distanceTo(firstTask.pickupCity);
                    cost += distance * v.costPerKm();
                }

                for(int i = 0; i < X.moveOrder.size(); i++){
                    double distance = X.moveOrder.get(v).get(i).distanceTo(X.moveOrder.get(v).get(i+1));
                    cost += distance * v.costPerKm();
                }
            }

            return cost;
        }

    }

    private class Variable {

        //either use pickUp & delivery order or moveOrder (or both)
        private Map<Vehicle, List<Task>> assignedTasks;
        private Map<Vehicle, List<Task>> current;
        private Map<Vehicle, List<Task>> done;
        private Map<Vehicle, List<Task>> pickUp;
        private Map<Vehicle, List<Task>> delivery;
        private Map<Vehicle, List<City>> moveOrder;

        public Variable(List<Vehicle> vehicles, TaskSet tasks) {

            this.assignedTasks = new HashMap<>(vehicles.size());
            this.current = new HashMap<>(tasks.size());
            this.done = new HashMap<>(tasks.size());
            this.pickUp = new HashMap<>(vehicles.size());
            this.delivery = new HashMap<>(vehicles.size());
            this.moveOrder = new HashMap<>(vehicles.size());

            /*for(Task task: tasks){
                time_pu.put(task, null);
                time_d.put(task, null);
            }*/

            for(Vehicle vehicle: vehicles){
                assignedTasks.put(vehicle, null);
                current.put(vehicle, null);
                done.put(vehicle, null);
                pickUp.put(vehicle, null);
                delivery.put(vehicle, null);
                moveOrder.put(vehicle, null);
            }
        }
    }

    private class Domain {

        private List<Task> tasks;
        private List<Vehicle> vehicles;
        private List<Integer> time;     //unnecessary?
        private List<City> cities;      //unnecessary?

        public Domain(List<Vehicle> vehicles, TaskSet tasks, Topology topology){
            this.vehicles = vehicles;
            this.tasks = new ArrayList<>(tasks);
            this.time = IntStream.rangeClosed(1, tasks.size()*2).boxed().collect(Collectors.toList());
            this.cities = new ArrayList<>(topology.cities());
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


    //probably not useful
    private class Distances {

        private Map<List<Task>, Double> td_tp;      //delivery -> pickup
        private Map<Task, Double> length_t;         //shortest length of task (pickup -> delivery)
        private Map<List<Task>, Double> tp_tp;      //pickup -> pickup
        private Map<List<Task>, Double> td_td;      //delivery -> delivery
        private Map<Vehicle, Double> v_tp;          //starting location -> first task

        public Distances(TaskSet tasks, List<Vehicle> vehicles, Topology topology){
            this.td_tp = new HashMap<>(tasks.size()*(tasks.size()-1));
            this.length_t = new HashMap<>(tasks.size());
            this.tp_tp = new HashMap<>(tasks.size()*(tasks.size()-1));
            this.td_td = new HashMap<>(tasks.size()*(tasks.size()-1));
            this.v_tp = new HashMap<>(vehicles.size());

        }
    }
}