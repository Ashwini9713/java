import java.util.Objects;
/** TASK 7 - Overriding, @Override rules, final, toString/equals/hashCode. */
abstract class Vehicle implements Cloneable {
    public static final int MINUTES_PER_SLAB = 60;                  // final variable (constant)
    private final String regNo;                                     // final field: set once
    protected Vehicle(String regNo) { this.regNo = Objects.requireNonNull(regNo).trim().toUpperCase(); }
    public final String getRegNo() { return regNo; }               // final method: cannot be overridden
    protected final int billableHours(int minutes) {               // final: one rule for every subclass
        if (minutes < 0) throw new IllegalArgumentException("negative");
        return (minutes + MINUTES_PER_SLAB - 1) / MINUTES_PER_SLAB;
    }
    public abstract double calculateFee(int minutes);              // each subclass MUST specialise
    public Vehicle copy() { return this; }                          // for covariant-return demo
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        return regNo.equals(((Vehicle) o).regNo);                   // same registration => equal
    }
    @Override public int hashCode() { return regNo.hashCode(); }   // contract: equal objects, equal hash
}
class TwoWheeler extends Vehicle {
    TwoWheeler(String r) { super(r); }
    @Override public double calculateFee(int m) { return billableHours(m) * 10; }
}
class Car extends Vehicle {
    Car(String r) { super(r); }
    @Override public double calculateFee(int m) { return billableHours(m) * 30; }
    @Override public Car copy() { return new Car(getRegNo()); }    // covariant return type (Vehicle -> Car) is allowed
}
class ElectricCar extends Car {
    ElectricCar(String r) { super(r); }
    @Override public double calculateFee(int m) { return super.calculateFee(m) * 0.8; }   // 20% EV discount, reuses Car logic
    // @Override public final String getRegNo()  -> compile error: cannot override a final method
}
final class Receipt { }                                             // final class: cannot be extended
/* RULES: same name + same parameter list; return type same or covariant;
          access same or wider (protected -> public ok, public -> protected NOT ok);
          cannot throw broader checked exceptions; static/private/final methods are not overridden. */

/* ---------- ALTERNATIVE: single calculateFee with switch on a type field ---------- */
class SwitchVehicle {
    enum Type { TWO_WHEELER, CAR, ELECTRIC_CAR }
    private final Type type; private final String regNo;
    SwitchVehicle(Type t, String r) { type = t; regNo = r; }
    double calculateFee(int minutes) {
        int h = (minutes + 59) / 60;
        switch (type) {                                             // k cases: edit HERE for every new type
            case TWO_WHEELER:   return h * 10;
            case CAR:           return h * 30;
            case ELECTRIC_CAR:  return h * 30 * 0.8;
            default: throw new IllegalStateException("unknown type");
        }
    }
}

public class Task7 {
    public static void main(String[] args) {
        Vehicle[] vs = { new TwoWheeler("TN01A1"), new Car("TN02B2"), new ElectricCar("TN03C3") };
        for (Vehicle v : vs) System.out.println(v + " fee(130) = " + v.calculateFee(130));
        System.out.println("Car copy() returns Car without cast: " + new Car("TN02B2").copy().getClass().getSimpleName());
        System.out.println("equals same reg (Car vs ElectricCar): " + new Car("TN02B2").equals(new ElectricCar("TN02B2")));
        System.out.println("equals different reg: " + new Car("A1").equals(new Car("B1")));
        System.out.println("hashCodes equal: " + (new Car("TN02B2").hashCode() == new ElectricCar("tn02b2").hashCode()));
        java.util.Set<Vehicle> set = new java.util.HashSet<>();
        set.add(new Car("TN02B2")); set.add(new ElectricCar("TN02B2"));
        System.out.println("HashSet size after adding both (expect 1): " + set.size());
        System.out.println("Switch version: " + new SwitchVehicle(SwitchVehicle.Type.ELECTRIC_CAR, "X").calculateFee(130));
    }
}
