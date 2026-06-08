package com.starlingbank.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.starlingbank.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ScoringServiceImpl implements ScoringService {

    private final OrgChartService orgChartService;

    @Inject
    public ScoringServiceImpl(OrgChartService orgChartService) {
        this.orgChartService = orgChartService;
    }

    @Override
    public AssignmentScore score(AssignmentCollection assignment, List<BookingRequest> bookings, List<Desk> desks) {
        Map<String, String> deskByEmployee = assignment.getDeskByEmployeeId();
        if (deskByEmployee.isEmpty() || bookings.isEmpty()) {
            return new AssignmentScore(0.0, 0.0, 0.0, 100.0, 100.0);
        }

        Map<String, Desk> deskById = desks.stream().collect(Collectors.toMap(Desk::getId, d -> d));

        List<BookingRequest> assigned = bookings.stream()
                .filter(b -> deskByEmployee.containsKey(b.getEmployeeId()))
                .collect(Collectors.toList());
        List<Desk> assignedDesks = assigned.stream()
                .map(b -> deskById.get(deskByEmployee.get(b.getEmployeeId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (assigned.size() != assignedDesks.size()) {
            assigned = assigned.subList(0, assignedDesks.size());
        }

        int n = assigned.size();
        double[][] dist = buildDistMatrix(assignedDesks);
        double meanDist = meanPairwiseDist(dist, n);
        Map<String, OrgNode> orgNodes = orgChartService.getOrgNodes();

        return new AssignmentScore(
                computeQapCost(assigned, dist, orgNodes, meanDist),
                computeTeamCohesion(assigned, dist, orgNodes, meanDist),
                computeManagerProximity(assigned, dist, orgNodes, meanDist),
                computeSocialSatisfaction(assigned, dist),
                100.0
        );
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    // Normalised QAP cost: 0 = perfect, 50 = same as random, 100 = twice as bad.
    // The frontend inverts this so lower cost shows as a higher displayed score.
    private double computeQapCost(List<BookingRequest> assigned, double[][] dist,
                                  Map<String, OrgNode> orgNodes, double meanDist) {
        int n = assigned.size();
        double totalWeight = 0;
        double actualCost = 0;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                OrgNode a = orgNodes.get(assigned.get(i).getEmployeeId());
                OrgNode b = orgNodes.get(assigned.get(j).getEmployeeId());
                double w = (a != null && b != null) ? 1.0 / Math.max(1, treeDistance(a, b)) : 0.0;
                actualCost += w * dist[i][j];
                totalWeight += w;
            }
        }

        if (totalWeight == 0 || meanDist == 0) return 0.0;
        // E[random cost] = totalWeight * meanDist
        double randomBaseline = totalWeight * meanDist;
        return clamp(50.0 * actualCost / randomBaseline, 0.0, 100.0);
    }

    // Weighted-average physical distance for close org-tree pairs, normalised to 0-100.
    private double computeTeamCohesion(List<BookingRequest> assigned, double[][] dist,
                                       Map<String, OrgNode> orgNodes, double meanDist) {
        int n = assigned.size();
        double totalWeight = 0;
        double weightedDist = 0;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                OrgNode a = orgNodes.get(assigned.get(i).getEmployeeId());
                OrgNode b = orgNodes.get(assigned.get(j).getEmployeeId());
                if (a == null || b == null) continue;
                int td = treeDistance(a, b);
                if (td <= 0 || td > 4) continue;
                double w = 1.0 / td;
                totalWeight += w;
                weightedDist += w * dist[i][j];
            }
        }

        if (totalWeight == 0 || meanDist == 0) return 50.0;
        double normDist = (weightedDist / totalWeight) / meanDist;
        return clamp(100.0 * (1.0 - normDist), 0.0, 100.0);
    }

    // Average distance between direct-report / manager pairs, normalised to 0-100.
    private double computeManagerProximity(List<BookingRequest> assigned, double[][] dist,
                                           Map<String, OrgNode> orgNodes, double meanDist) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < assigned.size(); i++) idx.put(assigned.get(i).getEmployeeId(), i);

        double totalDist = 0;
        int count = 0;
        for (int i = 0; i < assigned.size(); i++) {
            OrgNode node = orgNodes.get(assigned.get(i).getEmployeeId());
            if (node == null || node.getParentId() == null) continue;
            Integer managerIdx = idx.get(node.getParentId());
            if (managerIdx == null) continue;
            totalDist += dist[i][managerIdx];
            count++;
        }

        if (count == 0 || meanDist == 0) return 50.0;
        return clamp(100.0 * (1.0 - (totalDist / count) / meanDist), 0.0, 100.0);
    }

    // For each person check their 3 nearest desk-neighbours; score compatibility of social prefs.
    private double computeSocialSatisfaction(List<BookingRequest> assigned, double[][] dist) {
        int n = assigned.size();
        if (n <= 1) return 100.0;
        int k = Math.min(3, n - 1);

        double total = 0;
        double satisfied = 0;

        for (int i = 0; i < n; i++) {
            SocialPreference myPref = pref(assigned.get(i));
            if (myPref == SocialPreference.NONE) {
                satisfied += 1.0;
                total += 1.0;
                continue;
            }
            int[] neighbors = kNearest(i, k, dist, n);
            double score = 0;
            for (int ni : neighbors) {
                if (ni < 0) continue;
                score += socialCompatibility(myPref, pref(assigned.get(ni)));
            }
            // Map [-1,1] avg to [0,1]
            satisfied += (score / k + 1.0) / 2.0;
            total += 1.0;
        }

        return total == 0 ? 100.0 : clamp(100.0 * satisfied / total, 0.0, 100.0);
    }

    // Returns 1.0 if compatible, -1.0 if incompatible, 0.0 if one is NONE.
    private double socialCompatibility(SocialPreference a, SocialPreference b) {
        if (a == SocialPreference.NONE || b == SocialPreference.NONE) return 0.0;
        return (a == b) ? 1.0 : -1.0;
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double[][] buildDistMatrix(List<Desk> desks) {
        int m = desks.size();
        double[][] d = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                double dx = desks.get(i).getX() - desks.get(j).getX();
                double dy = desks.get(i).getY() - desks.get(j).getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                d[i][j] = dist;
                d[j][i] = dist;
            }
        }
        return d;
    }

    private double meanPairwiseDist(double[][] dist, int n) {
        if (n < 2) return 0.0;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += dist[i][j];
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private int[] kNearest(int idx, int k, double[][] dist, int n) {
        boolean[] used = new boolean[n];
        used[idx] = true;
        int[] result = new int[k];
        for (int t = 0; t < k; t++) {
            double best = Double.MAX_VALUE;
            int bestIdx = -1;
            for (int j = 0; j < n; j++) {
                if (!used[j] && dist[idx][j] < best) {
                    best = dist[idx][j];
                    bestIdx = j;
                }
            }
            result[t] = bestIdx;
            if (bestIdx >= 0) used[bestIdx] = true;
        }
        return result;
    }

    private int treeDistance(OrgNode a, OrgNode b) {
        List<String> pa = a.getOrgPath();
        List<String> pb = b.getOrgPath();
        int minLen = Math.min(pa.size(), pb.size());
        int lcaDepth = -1;
        for (int i = 0; i < minLen; i++) {
            if (pa.get(i).equals(pb.get(i))) lcaDepth = i;
            else break;
        }
        if (lcaDepth < 0) return Integer.MAX_VALUE / 2;
        return (pa.size() - 1 - lcaDepth) + (pb.size() - 1 - lcaDepth);
    }

    private SocialPreference pref(BookingRequest b) {
        SocialPreference p = b.getSocialPreference();
        return p == null ? SocialPreference.NONE : p;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
