package com.starlingbank.model;

public class AssignmentScore {
    private double totalQapCost;
    private double teamCohesion;
    private double managerProximity;
    private double socialSatisfaction;

    public AssignmentScore() {}

    public AssignmentScore(double totalQapCost, double teamCohesion, double managerProximity,
                           double socialSatisfaction) {
        this.totalQapCost = totalQapCost;
        this.teamCohesion = teamCohesion;
        this.managerProximity = managerProximity;
        this.socialSatisfaction = socialSatisfaction;
    }

    public double getTotalQapCost() { return totalQapCost; }
    public double getTeamCohesion() { return teamCohesion; }
    public double getManagerProximity() { return managerProximity; }
    public double getSocialSatisfaction() { return socialSatisfaction; }
}
