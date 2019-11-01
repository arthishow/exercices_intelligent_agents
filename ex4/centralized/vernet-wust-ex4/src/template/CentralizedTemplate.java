package template;

//the list of imports
import java.io.File;
import java.util.*;

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

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
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
        }
        catch (Exception exc) {
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

        //List<Object> X = createVariables(vehicles, tasks);
        List<List<Object>> D = createDomains(vehicles, tasks);

        System.out.println("Agent " + agent.id() + " has tasks " + tasks);

        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }


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

    //private Plan SLS(variables, domains, constraints, costFunction)

    //domains: defined once and doesn't change
    private final List<List<Object>> createDomains(List<Vehicle> vehicles, TaskSet tasks){
        int nb_tasks = tasks.size();
        int nb_vehicles = vehicles.size();

        List<List<Object>> domains = new Vector<>(3);
        List<Object> nextTasks = new Vector<>(nb_tasks+1); //+1: NULL element
        List<Object> time = new Vector<>(nb_tasks);
        List<Object> vehicle = new Vector<>(nb_tasks);

        int i = 1;
        for(Task task : tasks) {
            nextTasks.add(task);
            time.add(i);
            if(i<=nb_vehicles)
                vehicle.add(i);
            i++;
        }
        nextTasks.add(null);

        domains.add(nextTasks);
        domains.add(time);
        domains.add(vehicle);

        return domains;
    }

    //constraints: 7 types (3.1)
    private final List<Object> createConstraints(List<Object> X){
        List<Object> C = new Vector<>(7);

        return C;
    }

    //arguments: all tasks & X.nextTask_t
    //TRUE: nextTask(task) != task
    public boolean c1(TaskSet tasks, Map<Task, Task> nextTask_t){

        for (Task task : tasks){
            if(!nextTask_t.get(task).equals(null) && nextTask_t.get(task).equals(task))
                return false;
        }
        return true;
    }

    //argument: X.nextTask_v
    //TRUE: first task of every vehicle is at time 1;
    public boolean c2(Map<Vehicle, Task> nextTask_v){

        for(Vehicle v: nextTask_v.keySet()){
            if(!nextTask_v.get(v).equals(null) && !nextTask_v.get(v).equals(1))
                return false;
        }
        return true;
    }

    //arguments: X.nextTask_t & X.time
    //TRUE: time of nextTask = time of currentTask + 1
    public boolean c3(Map<Task, Task> nextTask_t, Map<Task, Integer> time){

        for(Task t : nextTask_t.keySet()){
            if(!nextTask_t.get(t).equals(null)){
                if (time.get(nextTask_t.get(t)) != time.get(t) + 1)
                    return false;
            }
        }
        return true;
    }

    //arguments: X.nextTask_v & X.vehicles
    //TRUE: redundant declaration of vehicle(task) = task(vehicle)
    public boolean c4(Map<Vehicle, Task> nextTask_v, Map<Task, Vehicle> vehicles){

        for(Vehicle v : nextTask_v.keySet()){
            if(!nextTask_v.get(v).equals(null)){
                if(vehicles.get(nextTask_v.get(v)) != v)
                    return false;
            }
        }
        return true;
    }

    //arguments: X.nextTask_t & X.vehicles
    //TRUE: nextTask(t1) = t2 -> vehicle(t1) = vehicle(t2)
    public boolean c5(Map<Task, Task> nextTask_t, Map<Task, Vehicle> vehicles){

        for(Task t : nextTask_t.keySet()){
            if(!nextTask_t.get(t).equals(null)){
                if(vehicles.get(nextTask_t.get(t)) != vehicles.get(t))
                    return false;
            }
        }
        return true;
    }

    //arguments: X.nextTask_t & X.nextTask_v & all tasks
    //TRUE: nextTask(t&v) contain all tasks + nbVehicles times a null argument
    public boolean c6(Map<Task, Task> nextTask_t,  Map<Vehicle, Task> nextTask_v, TaskSet tasks){

        Set<Task> tasksToCheck = new HashSet<>();
        tasksToCheck.addAll(tasks);

        int nullCount = 0;

        for(Vehicle v : nextTask_v.keySet()){
            if(nextTask_v.get(v).equals(null))
                nullCount++;
            else if(tasks.contains(nextTask_v.get(v)))
                tasksToCheck.remove(nextTask_v.get(v));
        }

        for(Task t : nextTask_t.keySet()){
            if(nextTask_t.get(t).equals(null))
                nullCount++;
            else if(tasks.contains(nextTask_t.get(t)))
                tasksToCheck.remove(nextTask_t.get(t));
        }

        if(nullCount==nextTask_v.size() && tasksToCheck.size()==0)
            return true;

        return false;
    }

    //argument: X.vehicles
    //TRUE: No task will exceed the capacity of a vehicle
    //TODO: currently implemented for only one task
    public boolean c7(Map<Task, Vehicle> vehicles){

        for(Task t : vehicles.keySet()){
            if(t.weight > vehicles.get(t).capacity())
                return false;
        }

        return true;
    }
}

//Variables: class of 3*nb_tasks + nb_vehicles elements
class X {
    public Map<Task, Task> nextTask_t;
    public Map<Vehicle, Task> nextTask_v;
    public Map<Task, Integer> time;
    public Map<Task, Vehicle> vehicles;

    public X(List<Vehicle> v, TaskSet tasks){
        this.nextTask_t = createNextTask_tMap(tasks);
        this.nextTask_v = createNextTask_vMap(v);
        this.time = createTimeMap(tasks);
        this.vehicles = createVehicleMap(v, tasks);
    }

    //TODO Values are currently all null, only basic structure for mapping of varables
    private Map<Task, Task> createNextTask_tMap(TaskSet tasks){

        Map<Task, Task> nextTask = new HashMap<>(tasks.size());

        //next task after current task
        for(Task task : tasks)
            nextTask.put(task, null);

        return nextTask;
    }

    private Map<Vehicle, Task> createNextTask_vMap(List<Vehicle> v){

        Map<Vehicle, Task> nextTask_v = new HashMap<>(v.size());

        //first task for vehicle
        for(Vehicle vehicle : v)
            nextTask_v.put(vehicle, null);

        return nextTask_v;
    }

    private Map<Task, Integer> createTimeMap(TaskSet tasks){

        Map<Task, Integer> time = new HashMap<>(tasks.size());

        for(Task task : tasks)
            time.put(task, 0);

        return time;
    }

    private Map<Task, Vehicle> createVehicleMap(List<Vehicle> v, TaskSet tasks){

        Map<Task, Vehicle> vehicles = new HashMap<>(v.size());

        for (Task task : tasks)
            vehicles.put(task, null);

        return vehicles;
    }

}

class assignment {

}