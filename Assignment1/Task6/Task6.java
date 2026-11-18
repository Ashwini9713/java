abstract class Vehicle {
    static boolean trace = false;
    private final String regNo;                                   // inherited via getter
    protected Vehicle(String regNo) {
        this.regNo = regNo;
        if (trace) System.out.println("  Vehicle(" + regNo + ") body done");
    }
    public String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    public double calculateFee(int minutes) { return ((minutes + 59) / 60) * ratePerHour(); }
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
}
class TwoWheeler extends Vehicle {
    TwoWheeler(String regNo) { super(regNo); if (trace) System.out.println("  TwoWheeler body done"); }
    @Override protected double ratePerHour() { return 10; }
}
class Car extends Vehicle {
    private final int seats;                                      // ADDED field
    Car(String regNo) { this(regNo, 4); }                         // this(...) chaining
    Car(String regNo, int seats) { super(regNo); this.seats = seats; if (trace) System.out.println("  Car body done"); }
    public int getSeats() { return seats; }                       // ADDED method
    @Override protected double ratePerHour() { return 30; }
}
class ElectricCar extends Car {
    private final int batteryKWh;                                 // ADDED field
    ElectricCar(String regNo, int batteryKWh) {
        super(regNo);                                             // super(...) constructor call, must be first
        this.batteryKWh = batteryKWh;
        if (trace) System.out.println("  ElectricCar body done");
    }
    public int getBatteryKWh() { return batteryKWh; }
    @Override public String toString() { return super.toString() + "{battery=" + batteryKWh + "kWh}"; }   // super.method()
    @Override protected double ratePerHour() { return super.ratePerHour(); }                              // reuse Car's rate
}
abstract class User {
    private final String id; private final String name;
    protected User(String id, String name) { this.id = id; this.name = name; }
    public String getId() { return id; } public String getName() { return name; }
    public abstract String category();
    @Override public String toString() { return category() + ":" + name; }
}
class Student extends User { Student(String id, String n) { super(id, n); } public String category() { return "Student"; } }
class Staff   extends User { Staff(String id, String n)   { super(id, n); } public String category() { return "Staff"; } }
class Visitor extends User { Visitor(String id, String n) { super(id, n); } public String category() { return "Visitor"; } }

/* ---------- ALTERNATIVE: composition (Vehicle HAS-A VehicleType) ---------- */
class VehicleType {
    private final String name; private final double rate;
    VehicleType(String name, double rate) { this.name = name; this.rate = rate; }
    String getName() { return name; } double getRate() { return rate; }
}
class ComposedVehicle {
    private final String regNo; private final VehicleType type;   // one extra reference per object
    ComposedVehicle(String regNo, VehicleType type) { this.regNo = regNo; this.type = type; }
    double calculateFee(int minutes) { return ((minutes + 59) / 60) * type.getRate(); }   // O(1), one extra indirection
    @Override public String toString() { return type.getName() + "[" + regNo + "]"; }
}

public class Task6 {
    public static void main(String[] args) {
        System.out.println("--- Constructor order for new ElectricCar(\"TN03C3\", 40) ---");
        Vehicle.trace = true;
        Vehicle ev = new ElectricCar("TN03C3", 40);
        Vehicle.trace = false;
        System.out.println("(Object() runs first, then Vehicle -> Car -> ElectricCar)");
        System.out.println(ev + "  fee(130 min) = " + ev.calculateFee(130));
        Vehicle[] all = { new TwoWheeler("TN01A1"), new Car("TN02B2"), ev };
        for (Vehicle v : all) System.out.println(v + " rate/h = " + v.calculateFee(60));
        User[] users = { new Student("S1", "Asha"), new Staff("T1", "Ravi"), new Visitor("V1", "Meena") };
        for (User u : users) System.out.println(u);

        System.out.println("\n--- Composition alternative ---");
        VehicleType car = new VehicleType("Car", 30);                    // shared by every car
        System.out.println(new ComposedVehicle("TN02B2", car) + " fee(130) = "
                + new ComposedVehicle("TN02B2", car).calculateFee(130));
        System.out.println("New type = 1 new VehicleType object, 0 new classes. Inheritance = 1 new class.");
    }
}
