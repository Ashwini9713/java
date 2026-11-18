package parking.service;

import parking.model.*;
import java.util.HashMap;
import java.util.Map;

public class TicketService {
    private final Map<Integer, ParkingTicket> bySlot = new HashMap<>();   // autoboxing: int slot -> Integer key
    public void issue(ParkingTicket t) {
        if (bySlot.containsKey(t.getSlot())) throw new IllegalStateException("Slot taken: " + t.getSlot());
        bySlot.put(t.getSlot(), t);
    }
    public ParkingTicket find(int slot) { return bySlot.get(slot); }
    public int count() { return bySlot.size(); }
    // t.auditTag();   <- compile error here: package-private in parking.model
}
