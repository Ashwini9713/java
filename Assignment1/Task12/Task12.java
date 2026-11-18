import java.time.*;
import java.util.*;

interface Chargeable { int RATE_PER_KWH = 12; double chargingFee(int kWh); }

abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("Registration number required");
        this.regNo = regNo.trim().toUpperCase();
    }
    public final String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    public double calculateFee(int minutes) {                       // overload 1
        if (minutes < 0) throw new IllegalArgumentException("Negative duration: " + minutes);
        return ((minutes + 59) / 60) * ratePerHour();               // every started hour
    }
    public double calculateFee(LocalTime in, LocalTime out) {       // overload 2
        if (out.isBefore(in)) throw new IllegalArgumentException("Exit before entry");
        return calculateFee((int) Duration.between(in, out).toMinutes());
    }
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
    @Override public boolean equals(Object o) { return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
class TwoWheeler extends Vehicle { TwoWheeler(String r) { super(r); } @Override protected double ratePerHour() { return 10; } }
class Car extends Vehicle { Car(String r) { super(r); } @Override protected double ratePerHour() { return 30; } }
class ElectricCar extends Car implements Chargeable {
    ElectricCar(String r) { super(r); }
    @Override public double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("kWh negative");
        return kWh * RATE_PER_KWH;
    }
}
final class ParkingTicket {
    final Vehicle vehicle; final int floor, slot; final LocalTime entry;
    ParkingTicket(Vehicle v, int floor, int slot, LocalTime entry) { this.vehicle = v; this.floor = floor; this.slot = slot; this.entry = entry; }
}

class Check {
    static int pass = 0, fail = 0;
    static void eq(String name, Object exp, Object act) {
        boolean ok = String.valueOf(exp).equals(String.valueOf(act));
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-50s expected=%-10s actual=%s%n", ok ? "PASS" : "FAIL", name, exp, act);
    }
    static void throwsEx(String name, Runnable r) {
        String res; try { r.run(); res = "no exception"; } catch (RuntimeException e) { res = e.getClass().getSimpleName(); }
        boolean ok = !res.equals("no exception");
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-50s expected=exception actual=%s%n", ok ? "PASS" : "FAIL", name, res);
    }
    static void summary() { System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed"); }
}

/* ===== NEW classes only: nothing above (Vehicle, Car, ...) was edited ===== */
class EBike extends Vehicle {
    static final double FLAT_FEE = 15;
    EBike(String r) { super(r); }
    @Override protected double ratePerHour() { return FLAT_FEE; }
    @Override public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Negative duration");
        return FLAT_FEE;
    }
}
class DeliveryVan extends Vehicle {                                    // double car rate 9:00-17:00
    DeliveryVan(String r) { super(r); }
    @Override protected double ratePerHour() { return 30; }
    @Override public double calculateFee(LocalTime in, LocalTime out) {
        if (out.isBefore(in)) throw new IllegalArgumentException("Exit before entry");
        int hours = ((int) Duration.between(in, out).toMinutes() + 59) / 60;
        double total = 0;
        for (int h = 0; h < hours; h++) {
            int clock = in.plusMinutes(60L * h).getHour();
            total += (clock >= 9 && clock < 17) ? 2 * ratePerHour() : ratePerHour();
        }
        return total;
    }
}
interface Pass {
    String regNo(); double price(); boolean validOn(LocalDate d);
    default boolean covers(String reg, LocalDate d) { return regNo().equalsIgnoreCase(reg) && validOn(d); }
}
class OneDayPass implements Pass {
    private final String reg; private final LocalDate day;
    OneDayPass(String reg, LocalDate day) { this.reg = reg.trim().toUpperCase(); this.day = day; }
    public String regNo() { return reg; }
    public double price() { return 100; }
    public boolean validOn(LocalDate d) { return day.equals(d); }
}
class VehicleFactory {                        // the ONLY existing class modified: 2 new case lines
    static Vehicle create(String type, String reg) {
        switch (type.trim().toUpperCase()) {
            case "TWOWHEELER":  return new TwoWheeler(reg);
            case "CAR":         return new Car(reg);
            case "ELECTRICCAR": return new ElectricCar(reg);
            case "EBIKE":       return new EBike(reg);                // NEW
            case "VAN":         return new DeliveryVan(reg);          // NEW
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
/** 5 floors with different slot counts: jagged array + HashMap index for O(1) exit. */
class MultiFloorLot {
    private final ParkingTicket[][] floors;
    private final Map<String, ParkingTicket> active = new HashMap<>();
    private final Map<String, Pass> passes = new HashMap<>();
    private double collection;
    MultiFloorLot(int... slotsPerFloor) {
        floors = new ParkingTicket[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            if (slotsPerFloor[f] <= 0) throw new IllegalArgumentException("Floor needs slots");
            floors[f] = new ParkingTicket[slotsPerFloor[f]];           // each row its own length
        }
    }
    void sellPass(Pass p) { passes.put(p.regNo(), p); collection += p.price(); }
    String enter(Vehicle v, LocalTime at) {
        if (active.containsKey(v.getRegNo())) throw new IllegalStateException("Duplicate " + v.getRegNo());
        for (int f = 0; f < floors.length; f++)
            for (int s = 0; s < floors[f].length; s++)
                if (floors[f][s] == null) {
                    ParkingTicket t = new ParkingTicket(v, f + 1, s + 1, at);
                    floors[f][s] = t; active.put(v.getRegNo(), t);
                    return "F" + (f + 1) + "-S" + (s + 1);
                }
        throw new IllegalStateException("Facility full");
    }
    double exit(String reg, LocalDate day, LocalTime at, int kWh) {
        ParkingTicket t = active.remove(reg.trim().toUpperCase());
        if (t == null) throw new IllegalArgumentException("No active ticket: " + reg);
        floors[t.floor - 1][t.slot - 1] = null;
        Pass p = passes.get(t.vehicle.getRegNo());
        double fee = 0;
        if (p == null || !p.covers(t.vehicle.getRegNo(), day)) {
            fee = t.vehicle.calculateFee(t.entry, at);
            if (t.vehicle instanceof Chargeable c) fee += c.chargingFee(kWh);
        }
        collection += fee; return fee;
    }
    int capacity() { int n = 0; for (ParkingTicket[] f : floors) n += f.length; return n; }
    double collection() { return collection; }
}

public class Task12 {
    static LocalTime t(String s) { return LocalTime.parse(s); }
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 11, 18);
        MultiFloorLot lot = new MultiFloorLot(4, 2, 5, 3, 6);
        Check.eq("capacity (4+2+5+3+6)", 20, lot.capacity());
        Check.eq("EBike 10 min flat", 15.0, new EBike("E").calculateFee(10));
        Check.eq("EBike 500 min flat", 15.0, new EBike("E").calculateFee(500));
        Check.eq("Van 08:00-10:00 (30+60)", 90.0, new DeliveryVan("V").calculateFee(t("08:00"), t("10:00")));
        Check.eq("Van 10:00-12:00 (60+60)", 120.0, new DeliveryVan("V").calculateFee(t("10:00"), t("12:00")));
        Check.eq("Van 16:30-17:30 = 1 peak hour", 60.0, new DeliveryVan("V").calculateFee(t("16:30"), t("17:30")));
        Check.eq("Van 16:30-17:31 (60+30)", 90.0, new DeliveryVan("V").calculateFee(t("16:30"), t("17:31")));
        Check.eq("Van 18:00-19:00 off-peak", 30.0, new DeliveryVan("V").calculateFee(t("18:00"), t("19:00")));
        Check.eq("old types unchanged: Car 130 min", 90.0, new Car("C").calculateFee(130));
        Check.eq("first slot", "F1-S1", lot.enter(VehicleFactory.create("ebike", "E1"), t("09:00")));
        lot.enter(VehicleFactory.create("van", "V1"), t("08:00"));
        lot.enter(new Car("VIS1"), t("09:00"));
        lot.sellPass(new OneDayPass("VIS1", today));
        Check.eq("EBike exit", 15.0, lot.exit("E1", today, t("09:20"), 0));
        Check.eq("Van exit 08:00-10:00", 90.0, lot.exit("V1", today, t("10:00"), 0));
        Check.eq("Visitor with pass exits free", 0.0, lot.exit("VIS1", today, t("15:00"), 0));
        Check.eq("Collection 15+90+0+pass 100", 205.0, lot.collection());
        Check.throwsEx("unknown type still rejected", () -> VehicleFactory.create("BUS", "B"));
        for (int i = 0; i < 20; i++) lot.enter(new Car("F" + i), t("09:00"));
        Check.throwsEx("21st vehicle: facility full", () -> lot.enter(new Car("OVER"), t("09:00")));
        Check.summary();
        System.out.println("Existing classes modified: 1 (VehicleFactory, +2 case lines). New classes: EBike, DeliveryVan, Pass, OneDayPass, MultiFloorLot.");
    }
}
