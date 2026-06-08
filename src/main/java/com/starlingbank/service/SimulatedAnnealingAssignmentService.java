package com.starlingbank.service;

import com.google.inject.Inject;
import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.model.Desk;
import com.starlingbank.model.OrgNode;
import com.starlingbank.model.SocialPreference;

import java.util.*;
import java.util.concurrent.*;

public class SimulatedAnnealingAssignmentService implements AssignmentService {

    private static final int NUM_RUNS = 400;
    private static final int NUM_THREADS = 12;
    private static final int ITERATIONS = 200_000;
    private static final double INITIAL_TEMPERATURE = 10_000.0;
    private static final double COOLING_RATE = 0.9999;

private final OrgChartService orgChartService;
    private final int numRuns;

    @Inject
    public SimulatedAnnealingAssignmentService(OrgChartService orgChartService) {
        this(orgChartService, NUM_RUNS);
    }

    SimulatedAnnealingAssignmentService(OrgChartService orgChartService, int numRuns) {
        this.orgChartService = orgChartService;
        this.numRuns = numRuns;
    }

    @Override
    public AssignmentCollection assign(List<BookingRequest> bookings, List<Desk> desks) {
        int n = bookings.size();
        int m = desks.size();

        // Pad to a full permutation so the SA considers every desk on the floor,
        // not just a random n-subset. Dummy bookings have no org node → weight 0
        // to everyone, so the optimiser freely fills them into leftover desks.
        List<BookingRequest> augmented = new ArrayList<>(bookings);
        for (int i = n; i < m; i++) {
            augmented.add(new BookingRequest("__dummy__" + i, SocialPreference.NONE, false));
        }

        double[][] weightMatrix = buildWeightMatrix(augmented);
        double[][] distMatrix = buildDistMatrix(desks);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<RunResult>> futures = new ArrayList<>(numRuns);

        for (int r = 0; r < numRuns; r++) {
            final int runId = r;
            futures.add(executor.submit(
                    () -> runSA(runId, m, m, weightMatrix, distMatrix)));
        }

        RunResult best = null;
        for (Future<RunResult> f : futures) {
            try {
                RunResult result = f.get();
                if (best == null || result.cost < best.cost) {
                    best = result;
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("SA run failed", e);
            }
        }

        executor.shutdown();

        // Only emit the first n entries — dummies are discarded
        Map<String, String> deskByEmployee = new LinkedHashMap<>();
        Map<String, String> employeeByDesk = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String empId = bookings.get(i).getEmployeeId();
            String deskId = desks.get(best.deskForPerson[i]).getId();
            deskByEmployee.put(empId, deskId);
            employeeByDesk.put(deskId, empId);
        }
        return new AssignmentCollection(deskByEmployee, employeeByDesk);
    }

    // -------------------------------------------------------------------------
    // SA core
    // -------------------------------------------------------------------------

    private RunResult runSA(int runId, int n, int m,
                            double[][] weightMatrix, double[][] distMatrix) {
        Random rng = new Random(System.nanoTime() + runId * 1_000_003L);

        // Random start: pick n distinct desk indices
        int[] available = new int[m];
        for (int i = 0; i < m; i++) available[i] = i;
        shuffleArray(available, rng);
        int[] deskForPerson = Arrays.copyOf(available, n);

        double cost = computeCost(deskForPerson, weightMatrix, distMatrix);
        int[] bestAssignment = deskForPerson.clone();
        double bestCost = cost;

        double temperature = INITIAL_TEMPERATURE;

        for (int iter = 0; iter < ITERATIONS; iter++) {
            int p = rng.nextInt(n);
            int q;
            do { q = rng.nextInt(n); } while (q == p);

            double delta = computeSwapDelta(p, q, deskForPerson, weightMatrix, distMatrix, n);

            if (delta < 0 || rng.nextDouble() < Math.exp(-delta / temperature)) {
                // apply swap
                int tmp = deskForPerson[p];
                deskForPerson[p] = deskForPerson[q];
                deskForPerson[q] = tmp;
                cost += delta;

                if (cost < bestCost) {
                    bestCost = cost;
                    bestAssignment = deskForPerson.clone();
                }
            }

            temperature *= COOLING_RATE;
        }

        return new RunResult(bestAssignment, bestCost);
    }

    private double computeSwapDelta(int p, int q, int[] deskForPerson,
                                    double[][] weight, double[][] dist, int n) {
        int dp = deskForPerson[p];
        int dq = deskForPerson[q];
        double delta = 0.0;
        for (int k = 0; k < n; k++) {
            if (k == p || k == q) continue;
            int dk = deskForPerson[k];
            double wpk = weight[p][k];
            double wqk = weight[q][k];
            // after swap: p gets dq, q gets dp
            delta += wpk * (dist[dq][dk] - dist[dp][dk]);
            delta += wqk * (dist[dp][dk] - dist[dq][dk]);
        }
        // p-q pair: dist[dq][dp] == dist[dp][dq] (symmetric), so contributes 0
        return delta;
    }

    private double computeCost(int[] deskForPerson,
                               double[][] weight, double[][] dist) {
        int n = deskForPerson.length;
        double cost = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                cost += weight[i][j] * dist[deskForPerson[i]][deskForPerson[j]];
            }
        }
        return cost;
    }

    // -------------------------------------------------------------------------
    // Weight matrix
    // -------------------------------------------------------------------------

    private double[][] buildWeightMatrix(List<BookingRequest> bookings) {
        Map<String, OrgNode> orgNodes = orgChartService.getOrgNodes();
        int n = bookings.size();

        // Find highest-ranked person among booked employees (min depth = most senior)
        String luckyTargetId = findHighestRanked(bookings, orgNodes);

        double[][] w = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                BookingRequest bi = bookings.get(i);
                BookingRequest bj = bookings.get(j);

                double weight = teamWeight(bi.getEmployeeId(), bj.getEmployeeId(), orgNodes)
                        + socialTerm(bi.getSocialPreference(), bj.getSocialPreference())
                        + luckyAffinity(bi, bj, luckyTargetId);

                w[i][j] = weight;
                w[j][i] = weight;
            }
        }
        return w;
    }

    private double teamWeight(String empA, String empB, Map<String, OrgNode> orgNodes) {
        OrgNode a = orgNodes.get(empA);
        OrgNode b = orgNodes.get(empB);
        if (a == null || b == null) return 0.0;

        int dist = treeDistance(a, b);
        if (dist <= 0) return 0.0;
        // Scale by leaf-factor: ICs (0 reports) get factor 1.0; managers get 1/N.
        // Two managers each with N reports contribute 1/N² — nearly invisible —
        // so ICs drive placement while managers are anchored by their team aggregate.
        // max() preserves IC→manager signal: if either party is a leaf, full weight.
        // Only two managers paired together get the reduced factor.
        double leafA = 1.0 / Math.max(1, a.getChildrenIds().size());
        double leafB = 1.0 / Math.max(1, b.getChildrenIds().size());
        return (1.0 / dist) * Math.max(leafA, leafB);
    }

    private double socialTerm(SocialPreference si, SocialPreference sj) {
        boolean iQuiet = si == SocialPreference.DONT_TALK_TO_ME;
        boolean jQuiet = sj == SocialPreference.DONT_TALK_TO_ME;
        boolean iTalk  = si == SocialPreference.TALK_TO_ME;
        boolean jTalk  = sj == SocialPreference.TALK_TO_ME;

        if (iTalk && jTalk) return 2.0;   // both social — attract
        if (iQuiet && jQuiet) return 2.0; // both introverts — attract (cluster quietly)
        if (iQuiet != jQuiet) return -3.0; // introvert paired with non-introvert — repel
        return 0.0;
    }

    private double luckyAffinity(BookingRequest bi, BookingRequest bj, String luckyTargetId) {
        if (luckyTargetId == null) return 0.0;
        double bonus = 0.0;
        if (bi.isFeelingLucky() && bj.getEmployeeId().equals(luckyTargetId)) bonus += 10.0;
        if (bj.isFeelingLucky() && bi.getEmployeeId().equals(luckyTargetId)) bonus += 10.0;
        return bonus;
    }

    private String findHighestRanked(List<BookingRequest> bookings, Map<String, OrgNode> orgNodes) {
        String topId = null;
        int minDepth = Integer.MAX_VALUE;
        for (BookingRequest b : bookings) {
            OrgNode node = orgNodes.get(b.getEmployeeId());
            if (node != null && node.getDepth() < minDepth) {
                minDepth = node.getDepth();
                topId = b.getEmployeeId();
            }
        }
        return topId;
    }

    // -------------------------------------------------------------------------
    // Org tree distance via LCA on orgPath
    // -------------------------------------------------------------------------

    private int treeDistance(OrgNode a, OrgNode b) {
        List<String> pathA = a.getOrgPath();
        List<String> pathB = b.getOrgPath();
        int minLen = Math.min(pathA.size(), pathB.size());
        int lcaDepth = -1;
        for (int i = 0; i < minLen; i++) {
            if (pathA.get(i).equals(pathB.get(i))) {
                lcaDepth = i;
            } else {
                break;
            }
        }
        if (lcaDepth < 0) return Integer.MAX_VALUE / 2;
        return (pathA.size() - 1 - lcaDepth) + (pathB.size() - 1 - lcaDepth);
    }

    // -------------------------------------------------------------------------
    // Distance matrix
    // -------------------------------------------------------------------------

    private double[][] buildDistMatrix(List<Desk> desks) {
        int m = desks.size();
        double[][] dist = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                double dx = desks.get(i).getX() - desks.get(j).getX();
                double dy = desks.get(i).getY() - desks.get(j).getY();
                double d = Math.sqrt(dx * dx + dy * dy);
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }
        return dist;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private void shuffleArray(int[] arr, Random rng) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private static class RunResult {
        final int[] deskForPerson;
        final double cost;

        RunResult(int[] deskForPerson, double cost) {
            this.deskForPerson = deskForPerson;
            this.cost = cost;
        }
    }
}