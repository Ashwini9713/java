import java.util.*;
/** TASK 9 - Abstraction: abstract class Vehicle (state + shared code), interfaces for capabilities. */
interface Chargeable {
    int RATE_PER_KWH = 12;
    double chargingFee(int kWh);
    default String chargingLabel() { return "EV charging @ Rs " + RATE_PER_KWH + "/kWh"; }   // default method
    static boolean isValidEnergy(int kWh) { return kWh >= 0; }                               // static method
}
interface Payable {
    double amountDue();
    default boolean isSettled() { return amountDue() == 0; }
}
abstract class Vehicle {                                             // abstract class: has constructor + private state
    private final String regNo;
    protected Vehicle(String regNo) { this.regNo = regNo; }
    public String getRegNo() { return regNo; }
    protected abstract double ratePerHour();                         // WHAT (abstract) ...
    public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }   // ... HOW shared (concrete)
}
class Car extends Vehicle { Car(String r) { super(r); } protected double ratePerHour() { return 30; } }
/** Multiple behaviours via interfaces: ONE superclass but MANY interfaces. */
class ElectricCar extends Car implements Chargeable, Payable, Comparable<ElectricCar> {
    private double due;
    ElectricCar(String r) { super(r); }
    public double chargingFee(int kWh) {
        if (!Chargeable.isValidEnergy(kWh)) throw new IllegalArgumentException("negative kWh");
        due += kWh * RATE_PER_KWH; return kWh * RATE_PER_KWH;
    }
    public double amountDue() { return due; }
    public int compareTo(ElectricCar o) { return getRegNo().compareTo(o.getRegNo()); }
}
abstract class User { private final String name; User(String n) { name = n; } String getName() { return name; } }
class Student extends User { Student(String n) { super(n); } }
class Visitor extends User implements Payable {
    private double due; Visitor(String n) { super(n); }
    void charge(double a) { due += a; }
    public double amountDue() { return due; }
}

/* ---------- ALTERNATIVE: swap the roles ----------
 * interface Vehicle { String getRegNo(); double ratePerHour(); default double calculateFee(int m){...} }
 *   - CANNOT hold per-object state (regNo) : only public static final constants
 *   - CANNOT have a constructor (no validation at creation)
 *   - every member is public (no protected/private instance data)
 * abstract class Chargeable { abstract double chargingFee(int kWh); }
 *   - ElectricCar can extend only ONE class, so it could not be Chargeable AND anything else
 *   - cannot extend Car and Chargeable together (no multiple inheritance of classes)
 * Time per call: interface call = invokeinterface vs invokevirtual - both O(1), difference negligible (JIT).
 * Space per object: same (object header + fields); an interface adds no per-object space. */

public class Task9 {
    public static void main(String[] args) {
        // Vehicle v = new Vehicle("X");            // compile error: abstract class cannot be instantiated
        ElectricCar ev = new ElectricCar("TN03C3");
        System.out.println("Fee 130 min: " + ev.calculateFee(130));
        System.out.println(ev.chargingLabel());
        System.out.println("Charging 10 kWh: " + ev.chargingFee(10) + ", due = " + ev.amountDue() + ", settled? " + ev.isSettled());
        Visitor vis = new Visitor("Meena"); vis.charge(100);
        List<Payable> payers = List.of(ev, vis);                     // different classes, one capability
        double total = 0; for (Payable p : payers) total += p.amountDue();
        System.out.println("Total payable by all Payable objects: " + total);
        Chargeable c = ev;                                           // interface reference
        System.out.println("Via Chargeable reference: " + c.chargingFee(5));
    }
}
