package com.starlingbank.service;

import com.google.inject.Singleton;
import com.starlingbank.model.AssignmentCollection;
import java.util.Map;

@Singleton
public class AssignmentState {
    private volatile AssignmentCollection last = new AssignmentCollection(Map.of(), Map.of());

    public AssignmentCollection get() { return last; }
    public void set(AssignmentCollection assignment) { last = assignment; }
}
