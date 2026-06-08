package com.starlingbank.service;

import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.model.Desk;

import java.util.*;

public class RandomAssignmentService implements AssignmentService {

    private final Random rng;

    public RandomAssignmentService() {
        this.rng = new Random();
    }

    RandomAssignmentService(long seed) {
        this.rng = new Random(seed);
    }

    @Override
    public AssignmentCollection assign(List<BookingRequest> bookings, List<Desk> desks) {
        List<Desk> shuffled = new ArrayList<>(desks);
        Collections.shuffle(shuffled, rng);

        Map<String, String> deskByEmployee = new LinkedHashMap<>();
        Map<String, String> employeeByDesk = new LinkedHashMap<>();
        for (int i = 0; i < bookings.size(); i++) {
            String empId = bookings.get(i).getEmployeeId();
            String deskId = shuffled.get(i).getId();
            deskByEmployee.put(empId, deskId);
            employeeByDesk.put(deskId, empId);
        }
        return new AssignmentCollection(deskByEmployee, employeeByDesk);
    }
}
