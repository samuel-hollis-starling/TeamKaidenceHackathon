package com.starlingbank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starlingbank.model.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedAnnealingAssignmentServiceTest {

    private static final String MARTIN_DOW_ID = "75_Martin_Dow";

    private static List<Desk> desks;
    private static Map<String, Desk> deskById;
    private static Map<String, Employee> employees;
    private static Map<String, OrgNode> orgNodes;

    @BeforeAll
    static void loadData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode floorRoot = mapper.readTree(new File("input-data/floor-map-5th.json"));
        desks = new ArrayList<>();
        for (JsonNode d : floorRoot.get("spaces").get("desks")) {
            JsonNode nbNode = d.get("neighborhood");
            String neighborhood = (nbNode == null || nbNode.isNull()) ? null : nbNode.asText();
            desks.add(new Desk(
                    d.get("id").asText(),
                    d.get("name").asText(),
                    neighborhood,
                    d.get("x").asDouble(),
                    d.get("y").asDouble(),
                    d.get("rotation").asDouble()
            ));
        }
        deskById = desks.stream().collect(Collectors.toMap(Desk::getId, d -> d));

        JsonNode orgRoot = mapper.readTree(new File("input-data/orgchart.json"));
        employees = new LinkedHashMap<>();
        orgNodes = new LinkedHashMap<>();
        orgRoot.fields().forEachRemaining(entry -> {
            String id = entry.getKey();
            JsonNode n = entry.getValue();
            employees.put(id, new Employee(id, n.get("name").asText(), n.get("role").asText(), n.get("location").asText()));
            String parentId = n.get("parent").isNull() ? null : n.get("parent").asText();
            List<String> childrenIds = new ArrayList<>();
            n.get("children").forEach(c -> childrenIds.add(c.asText()));
            List<String> orgPath = new ArrayList<>();
            n.get("orgPath").forEach(p -> orgPath.add(p.asText()));
            orgNodes.put(id, new OrgNode(id, parentId, childrenIds, n.get("depth").asInt(), orgPath));
        });
    }

    @Test
    void martinsOrgAssignment() {
        // Only use real fixed desks — exclude Flexible Working, Working Lounge, and Pods
        List<Desk> realDesks = desks.stream()
                .filter(d -> d.getNeighborhood() != null && !d.getNeighborhood().equals("Desk Pods"))
                .toList();

        // Collect everyone strictly below Martin Dow via BFS on his children
        List<String> subtree = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>(orgNodes.get(MARTIN_DOW_ID).getChildrenIds());
        while (!queue.isEmpty()) {
            String id = queue.poll();
            subtree.add(id);
            queue.addAll(orgNodes.get(id).getChildrenIds());
        }

        // Pick as many as there are real desks (reproducible seed), social preference = NONE throughout
        Collections.shuffle(subtree, new Random(42));
        List<String> selected = subtree.subList(0, Math.min(realDesks.size(), subtree.size()));

        Random rng = new Random(42);
        List<BookingRequest> bookings = selected.stream()
                .map(id -> new BookingRequest(id, SocialPreference.NONE, rng.nextBoolean(), false))
                .collect(Collectors.toList());

        OrgChartService orgChartService = new OrgChartService() {
            @Override public Map<String, Employee> getEmployees() { return employees; }
            @Override public Map<String, OrgNode> getOrgNodes() { return orgNodes; }
        };

        long start = System.currentTimeMillis();
        AssignmentCollection result = new SimulatedAnnealingAssignmentService(orgChartService)
                .assign(bookings, realDesks);
        long elapsedMs = System.currentTimeMillis() - start;

        Map<String, String> deskByEmployee = result.getDeskByEmployeeId();
        Map<String, String> employeeByDesk = result.getEmployeeByDeskId();

        // ── Correctness assertions ──────────────────────────────────────────────
        assertEquals(selected.size(), deskByEmployee.size(), "Every selected employee must have a desk");
        assertEquals(selected.size(), employeeByDesk.size(), "No desk may be double-assigned");
        Set<String> validDeskIds = realDesks.stream().map(Desk::getId).collect(Collectors.toSet());
        deskByEmployee.forEach((ignored, desk) -> assertTrue(validDeskIds.contains(desk), "Unknown desk: " + desk));
        deskByEmployee.forEach((emp, desk) -> assertEquals(emp, employeeByDesk.get(desk), "Reverse map mismatch"));

        // ── Pretty output ───────────────────────────────────────────────────────
        printAssignment(bookings, deskByEmployee, subtree.size(), elapsedMs);
    }

    // ---------------------------------------------------------------------------
    // Output
    // ---------------------------------------------------------------------------

    private void printAssignment(List<BookingRequest> bookings,
                                 Map<String, String> deskByEmployee,
                                 int subtreeSize,
                                 long elapsedMs) {
        Map<String, BookingRequest> bookingById = bookings.stream()
                .collect(Collectors.toMap(BookingRequest::getEmployeeId, b -> b));

        // Resolve depth-4 team name for each employee (Martin's direct reports)
        List<String> teamHeadIds = orgNodes.get(MARTIN_DOW_ID).getChildrenIds();
        Map<String, String> teamHeadFirstName = teamHeadIds.stream()
                .collect(Collectors.toMap(id -> id, id -> employees.get(id).getName().split(" ")[0]));

        // Sort all assignments by desk name alphabetically
        List<String> sorted = new ArrayList<>(deskByEmployee.keySet());
        sorted.sort(Comparator.comparing(empId -> deskById.get(deskByEmployee.get(empId)).getName()));

        System.out.println();
        System.out.println("╔" + "═".repeat(88) + "╗");
        System.out.printf("║  %-86s║%n", "SA Assignment — Martin Dow's Org  (sorted by desk name)");
        System.out.printf("║  %-86s║%n",
                String.format("Sampled %d of %d subordinates  |  Elapsed: %dms", deskByEmployee.size(), subtreeSize, elapsedMs));
        System.out.println("╠" + "═".repeat(88) + "╣");
        System.out.printf("║  %-22s  %-18s  %-28s  %-10s  %-3s║%n",
                "Desk", "Neighborhood", "Employee", "Team", "");
        System.out.println("╠" + "═".repeat(88) + "╣");

        for (String empId : sorted) {
            Desk desk = deskById.get(deskByEmployee.get(empId));
            BookingRequest b = bookingById.get(empId);
            OrgNode node = orgNodes.get(empId);
            String window = b.isWindowSeat() ? "WIN" : "   ";

            // Depth-4 ancestor first name
            String team = node.getOrgPath().size() > 4
                    ? teamHeadFirstName.getOrDefault(node.getOrgPath().get(4), "?")
                    : teamHeadFirstName.getOrDefault(empId, "?");

            System.out.printf("║  %-22s  %-18s  %-28s  %-10s  %s║%n",
                    desk.getName(),
                    desk.getNeighborhood() == null ? "" : desk.getNeighborhood(),
                    employees.get(empId).getName(),
                    team,
                    window);
        }
        System.out.println("╚" + "═".repeat(88) + "╝");

        // ── QAP Metrics ─────────────────────────────────────────────────────────
        System.out.println();
        System.out.println("── QAP Metrics " + "─".repeat(65));

        DoubleSummaryStatistics siblingStats = new DoubleSummaryStatistics();
        DoubleSummaryStatistics sameTeamStats = new DoubleSummaryStatistics();
        DoubleSummaryStatistics crossTeamStats = new DoubleSummaryStatistics();

        List<String> empIds = new ArrayList<>(deskByEmployee.keySet());
        for (int i = 0; i < empIds.size(); i++) {
            for (int j = i + 1; j < empIds.size(); j++) {
                String a = empIds.get(i);
                String b = empIds.get(j);
                int treeDist = treeDistance(orgNodes.get(a), orgNodes.get(b));
                double dist = euclidean(deskById.get(deskByEmployee.get(a)), deskById.get(deskByEmployee.get(b)));
                if (treeDist == 1) siblingStats.accept(dist);
                else if (treeDist <= 5) sameTeamStats.accept(dist);
                else crossTeamStats.accept(dist);
            }
        }

        System.out.printf("  Direct siblings    (tree dist=1):  %6.0f units avg over %5d pairs%n",
                siblingStats.getAverage(), siblingStats.getCount());
        System.out.printf("  Same sub-org       (tree dist 2-5): %6.0f units avg over %5d pairs%n",
                sameTeamStats.getAverage(), sameTeamStats.getCount());
        System.out.printf("  Cross-team         (tree dist 6+):  %6.0f units avg over %5d pairs%n",
                crossTeamStats.getAverage(), crossTeamStats.getCount());

        // The quality signal: siblings should be much closer than cross-team
        double ratio = siblingStats.getCount() > 0 && crossTeamStats.getCount() > 0
                ? crossTeamStats.getAverage() / siblingStats.getAverage() : 0;
        System.out.printf("%n  Clustering ratio (cross/sibling): %.2fx  (>1 = siblings closer than strangers)%n", ratio);
        System.out.println();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private double euclidean(Desk a, Desk b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private int treeDistance(OrgNode a, OrgNode b) {
        if (a == null || b == null) return Integer.MAX_VALUE / 2;
        List<String> pathA = a.getOrgPath();
        List<String> pathB = b.getOrgPath();
        int minLen = Math.min(pathA.size(), pathB.size());
        int lcaDepth = -1;
        for (int i = 0; i < minLen; i++) {
            if (pathA.get(i).equals(pathB.get(i))) lcaDepth = i;
            else break;
        }
        if (lcaDepth < 0) return Integer.MAX_VALUE / 2;
        return (pathA.size() - 1 - lcaDepth) + (pathB.size() - 1 - lcaDepth);
    }
}
