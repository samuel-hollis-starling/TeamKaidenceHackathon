package com.starlingbank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.starlingbank.model.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Uses real attendance data extracted from starling-bank.onkadence.co.har (2026-06-03).
 * Compares the actual Kadence assignment against SA and a random baseline.
 */
class HarBookingAssignmentTest {

    private static List<Desk> desks;
    private static Map<String, Desk> deskById;
    private static Map<String, Employee> employees;
    private static Map<String, OrgNode> orgNodes;

    // 86 employees present on 5th floor from HAR, mapped to org chart IDs
    private static final List<String> HAR_EMPLOYEE_IDS = List.of(
            "477_David_Pyhyk",
            "1033_James_Sawyer",
            "209_David_Harvey",
            "2083_Yat_Cheung",
            "241_Daniel_Cosentino",
            "234_Ollie_Jamieson",
            "601_Em_Pearce",
            "603_Lauren_Kenward",
            "228_Toby_Hewing",
            "2073_Francesco_Malatto",
            "2081_Leyla_Azad",
            "2075_Ore_Babasina",
            "2079_Jamie_Simm",
            "1268_Jacob_Rosenskold",
            "1288_Rob_Weddell",
            "1192_Fabrice_Conchon",
            "2080_Krish_Harjani",
            "1249_Ian_Riddick",
            "2098_Conor_Grocock",
            "631_Stephan_Blakeslee",
            "2038_Maninder_Soor",
            "1165_James_Barker",
            "1271_Patricia_Hudakova",
            "74_Jimi_Lawal",
            "1266_Dan_Ashworth",
            "1173_Sophia_Georgiou",
            "238_Louise_Tessier",
            "1240_Adam_Modzelewski",
            "3085_Nadya_Aliakseyeva",
            "3083_Dan_Smith",
            "231_Emily_Bate",
            "602_Joe_Jeffries",
            "1269_Kaiman_Mehmet",
            "1216_Sheldon_White",
            "1264_Bilal_Pandor",
            "1199_Becks_Simpson",
            "591_Anna_Blesing",
            "598_Emma_Drew",
            "1208_Sara_Fakouri",
            "2060_Aaron_Tunney",
            "613_Steven_Blyth",
            "2106_Tara_Kaul",
            "2101_Chun_Ngai_Au",
            "570_Christina_Branco",
            "615_Katy_Hemming",
            "1274_Sam_Fraser-Barraud",
            "230_Alex_Gray",
            "1267_George_Cullinane",
            "620_James_Hunter",
            "1150_Oj_Akhigbe",
            "1888_Jenn_Philpott",
            "1203_Shaan_Master",
            "1221_Jesse_Willoughby",
            "2069_Lucie_Spurgeon",
            "216_Erhan_Temurkan",
            "1877_Jonathan_Knight",
            "1285_Oliver_Broughton",
            "1265_Chloe_Fong",
            "252_Nassos_Antoniou",
            "1186_Onur_Gokcinar",
            "251_Matthew_Salt",
            "638_Rich_Sale",
            "1117_Finley_Batchelor",
            "2092_Jonathan_Robinson",
            "600_Mahfuz_Murad",
            "599_Tom_Bennett",
            "1200_Eily_Lo",
            "2082_Marc_Mardare",
            "1207_Maria_Walton_Campagna",
            "112_Mark_Robson",
            "2104_Marcus_Mak",
            "217_Federico_Iaschi",
            "633_Bill_Ahmed",
            "1206_Jacob_Turton",
            "1213_Grace_Breen",
            "1251_Tim_La_Roche",
            "630_Max_Jones",
            "1231_Vivien_Tang",
            "1230_Nikke_San",
            "1229_Gemma_Allwright",
            "1220_Alex_Maimescu",
            "1160_Darnell_Moore",
            "2097_Andrew_James_Hopkins",
            "1901_Hamish_Proudlock",
            "2099_Sophie_Wilson",
            "233_Jordan_Payne"
    );

    // Actual desk assignments recorded in Kadence on that day
    private static final Map<String, String> ACTUAL_DESK_BY_EMPLOYEE = new LinkedHashMap<>();

    @BeforeAll
    static void loadData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode floorRoot = mapper.readTree(new File("input-data/floors/london-fruit-wool-exchange-5th-floor.json"));
        desks = new ArrayList<>();
        for (JsonNode d : floorRoot.get("spaces").get("desks")) {
            JsonNode nbNode = d.get("neighborhood");
            String neighborhood = (nbNode == null || nbNode.isNull()) ? null : nbNode.asText();
            desks.add(new Desk(
                    d.get("id").asText(), d.get("name").asText(), neighborhood,
                    d.get("x").asDouble(), d.get("y").asDouble(), d.get("rotation").asDouble()
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

        ACTUAL_DESK_BY_EMPLOYEE.put("477_David_Pyhyk", "01G9F39Z7B4SVDFQ6DGY4WA7ZG");
        ACTUAL_DESK_BY_EMPLOYEE.put("1033_James_Sawyer", "01G9C6WSG70DH6CF01F4PJY36K");
        ACTUAL_DESK_BY_EMPLOYEE.put("209_David_Harvey", "01G6X1BR49WYB5474RCM37CN01");
        ACTUAL_DESK_BY_EMPLOYEE.put("2083_Yat_Cheung", "01G6X1CJ5W52P55TPYNAMX0NV8");
        ACTUAL_DESK_BY_EMPLOYEE.put("241_Daniel_Cosentino", "01G6X1AXT33V1KY943F8CXP442");
        ACTUAL_DESK_BY_EMPLOYEE.put("234_Ollie_Jamieson", "01G6X1B7F3GY7AQB6TJA0ANZEE");
        ACTUAL_DESK_BY_EMPLOYEE.put("601_Em_Pearce", "01G6X1B6VQE18T4WV7SQFPVD06");
        ACTUAL_DESK_BY_EMPLOYEE.put("603_Lauren_Kenward", "01G6X1B761CTC37N7F6GMA3R6Q");
        ACTUAL_DESK_BY_EMPLOYEE.put("228_Toby_Hewing", "01G6X1B2JV39S29KAPD17201D0");
        ACTUAL_DESK_BY_EMPLOYEE.put("2073_Francesco_Malatto", "01G6X1BG8GD332NVJFWN4AQ904");
        ACTUAL_DESK_BY_EMPLOYEE.put("2081_Leyla_Azad", "01G6X1BGY4APZZ29G9CKJNZM6F");
        ACTUAL_DESK_BY_EMPLOYEE.put("2075_Ore_Babasina", "01G9F39YQABJFRV7SQW80Z37CV");
        ACTUAL_DESK_BY_EMPLOYEE.put("2079_Jamie_Simm", "01G6X1BFZ37Y9P9VNJFFAZMPQC");
        ACTUAL_DESK_BY_EMPLOYEE.put("1268_Jacob_Rosenskold", "01G6X1C6WBHVXP8W89633V0XFJ");
        ACTUAL_DESK_BY_EMPLOYEE.put("1288_Rob_Weddell", "01G6X1CM6QTF2MXGJEK1D5C34E");
        ACTUAL_DESK_BY_EMPLOYEE.put("1192_Fabrice_Conchon", "01G6X1B28A9X9S5MFPFEVS1FC6");
        ACTUAL_DESK_BY_EMPLOYEE.put("2080_Krish_Harjani", "01G6X1CJGSDWE3Z1B42B3GKN1V");
        ACTUAL_DESK_BY_EMPLOYEE.put("1249_Ian_Riddick", "01G6X1B66HZYY69YPT81M420Z8");
        ACTUAL_DESK_BY_EMPLOYEE.put("2098_Conor_Grocock", "01G6X1BX59KV3TCFAP778E2YCB");
        ACTUAL_DESK_BY_EMPLOYEE.put("631_Stephan_Blakeslee", "01G6X1B0M7E1HTXCPNZ0DXJM8B");
        ACTUAL_DESK_BY_EMPLOYEE.put("2038_Maninder_Soor", "01G6X1C6K7N2ENVREEB6BJS624");
        ACTUAL_DESK_BY_EMPLOYEE.put("1165_James_Barker", "01G6X1BW4T04WTXKYB9P6M2Z7E");
        ACTUAL_DESK_BY_EMPLOYEE.put("1271_Patricia_Hudakova", "01HR9KBPJ1HBBQKH6FD3ZZ4E1C");
        ACTUAL_DESK_BY_EMPLOYEE.put("74_Jimi_Lawal", "01G6X1AZNVQQ7HTS0AJ8QVHFMM");
        ACTUAL_DESK_BY_EMPLOYEE.put("1266_Dan_Ashworth", "01G6X1C7X2JRQHWAKDB98S9SXM");
        ACTUAL_DESK_BY_EMPLOYEE.put("1173_Sophia_Georgiou", "01G6X1BVFKK09RE77NGMFGY04K");
        ACTUAL_DESK_BY_EMPLOYEE.put("238_Louise_Tessier", "01G6X1B385P4FF64768DGTZ1EV");
        ACTUAL_DESK_BY_EMPLOYEE.put("1240_Adam_Modzelewski", "01G6X1BNH9N5N014EFCX6M3NXH");
        ACTUAL_DESK_BY_EMPLOYEE.put("3085_Nadya_Aliakseyeva", "01G6X1C04MRB7VPNPFZKAHYNV1");
        ACTUAL_DESK_BY_EMPLOYEE.put("3083_Dan_Smith", "01G6X1C23E6Z67R95W4WMEATZ1");
        ACTUAL_DESK_BY_EMPLOYEE.put("231_Emily_Bate", "01G6X1B6H4VNE2M3YWEZ35BDAJ");
        ACTUAL_DESK_BY_EMPLOYEE.put("602_Joe_Jeffries", "01G6X1B5W48ZGJP01SSHDXXGAN");
        ACTUAL_DESK_BY_EMPLOYEE.put("1269_Kaiman_Mehmet", "01G6X1C7J9NWNQBKM3T5SX8TP9");
        ACTUAL_DESK_BY_EMPLOYEE.put("1216_Sheldon_White", "01G6X1CKW9N865DBQZDKPG5EBF");
        ACTUAL_DESK_BY_EMPLOYEE.put("1264_Bilal_Pandor", "01G6X1C778E9Q7YS9PJMT6SN0K");
        ACTUAL_DESK_BY_EMPLOYEE.put("1199_Becks_Simpson", "01G6X1CHV35SZMPK2F7GMQ9F18");
        ACTUAL_DESK_BY_EMPLOYEE.put("591_Anna_Blesing", "01G6X1CJVYQX8S6XX0ZPJFS5XW");
        ACTUAL_DESK_BY_EMPLOYEE.put("598_Emma_Drew", "01G6X1B2XF5W2TZ691D8713Z4S");
        ACTUAL_DESK_BY_EMPLOYEE.put("1208_Sara_Fakouri", "01G6X1CH615BYTQ2QQK9HYCKQW");
        ACTUAL_DESK_BY_EMPLOYEE.put("2060_Aaron_Tunney", "01G6X1BFAP1HDRCZJX77N73262");
        ACTUAL_DESK_BY_EMPLOYEE.put("613_Steven_Blyth", "01HA6T4NF8712DB3N168YXAMYQ");
        ACTUAL_DESK_BY_EMPLOYEE.put("2106_Tara_Kaul", "01G6X1C8VRG4MA51GJ9Q939SXC");
        ACTUAL_DESK_BY_EMPLOYEE.put("2101_Chun_Ngai_Au", "01G6X1C5XQ0W8Y0PYYHD88W5TK");
        ACTUAL_DESK_BY_EMPLOYEE.put("570_Christina_Branco", "01G6X1BPSYDBYW31YYVXRWA7EN");
        ACTUAL_DESK_BY_EMPLOYEE.put("615_Katy_Hemming", "01G6X1B57ZR7H6V7CDXK7CQXSB");
        ACTUAL_DESK_BY_EMPLOYEE.put("1274_Sam_Fraser-Barraud", "01G6X1C9GDMCGRF71M2HQ6DWG9");
        ACTUAL_DESK_BY_EMPLOYEE.put("230_Alex_Gray", "01G6X1AZBEFZYCSEQ69RHWDXEZ");
        ACTUAL_DESK_BY_EMPLOYEE.put("1267_George_Cullinane", "01G6X1C14T3PDZGM1AKCGX17PC");
        ACTUAL_DESK_BY_EMPLOYEE.put("620_James_Hunter", "01G6X1C50B7T7E4J1HFEHQDSG4");
        ACTUAL_DESK_BY_EMPLOYEE.put("1150_Oj_Akhigbe", "01G6X1BXGBZ8MN6VNJ800RH3S4");
        ACTUAL_DESK_BY_EMPLOYEE.put("1888_Jenn_Philpott", "01G6X1B1JVATKJTS49B818YXRY");
        ACTUAL_DESK_BY_EMPLOYEE.put("1203_Shaan_Master", "01G6X1CKHEMHG0TBE4EB3MYBYX");
        ACTUAL_DESK_BY_EMPLOYEE.put("1221_Jesse_Willoughby", "01G6X1C1RQY3RV0YYXMVPBCK91");
        ACTUAL_DESK_BY_EMPLOYEE.put("2069_Lucie_Spurgeon", "01G6X1BHXVNMD2W71P6Y203YEH");
        ACTUAL_DESK_BY_EMPLOYEE.put("216_Erhan_Temurkan", "01G6X1AYQE0JFGQQPRPBZXP1PH");
        ACTUAL_DESK_BY_EMPLOYEE.put("1877_Jonathan_Knight", "01G6X1BT4RW1Q317BNP4ZG7RKP");
        ACTUAL_DESK_BY_EMPLOYEE.put("1285_Oliver_Broughton", "01G6X1CA60VDGEP4R3GTNX13FK");
        ACTUAL_DESK_BY_EMPLOYEE.put("1265_Chloe_Fong", "01G6X1C96BDHR87FQ9FN2BEYYG");
        ACTUAL_DESK_BY_EMPLOYEE.put("252_Nassos_Antoniou", "01G6X1AYE4YSY1M3TD2CY9N4FQ");
        ACTUAL_DESK_BY_EMPLOYEE.put("1186_Onur_Gokcinar", "01G6X1BP66EEZBBX2JAXHJSCKQ");
        ACTUAL_DESK_BY_EMPLOYEE.put("251_Matthew_Salt", "01G6X1AY3F5B6WWHP50X600DM1");
        ACTUAL_DESK_BY_EMPLOYEE.put("638_Rich_Sale", "01HR9KEMR0TCJAP22EVH7EETMY");
        ACTUAL_DESK_BY_EMPLOYEE.put("1117_Finley_Batchelor", "01G6X1B3JM2APSNTCYX7H5KADN");
        ACTUAL_DESK_BY_EMPLOYEE.put("2092_Jonathan_Robinson", "01G6X1C0FEFMVDCGA511S73PHS");
        ACTUAL_DESK_BY_EMPLOYEE.put("600_Mahfuz_Murad", "01G6X1B1XE6BZ50HPCDJPMEQG0");
        ACTUAL_DESK_BY_EMPLOYEE.put("599_Tom_Bennett", "01G6X1BFME8YVQWWY00YZE45BD");
        ACTUAL_DESK_BY_EMPLOYEE.put("1200_Eily_Lo", "01G6X1CK6N1Y38E1QFC4GHVV4F");
        ACTUAL_DESK_BY_EMPLOYEE.put("2082_Marc_Mardare", "01G6X1BGK9JPZHDRVAPYXM4CGB");
        ACTUAL_DESK_BY_EMPLOYEE.put("1207_Maria_Walton_Campagna", "01G6X1CHGF80CSJZ3QNJFYQ575");
        ACTUAL_DESK_BY_EMPLOYEE.put("112_Mark_Robson", "01HA6T6NH97KSEDMRF71ETVXYN");
        ACTUAL_DESK_BY_EMPLOYEE.put("2104_Marcus_Mak", "01G6X1C5MGC52QRQWT980ABGBD");
        ACTUAL_DESK_BY_EMPLOYEE.put("217_Federico_Iaschi", "01G6X1B09PGF9JSXFBZ3Q5SKB0");
        ACTUAL_DESK_BY_EMPLOYEE.put("633_Bill_Ahmed", "01G6X1BKK00BR6A02K35WSA3Q3");
        ACTUAL_DESK_BY_EMPLOYEE.put("1206_Jacob_Turton", "01HA6T49MC7Y74Q6GNMG1C8QKJ");
        ACTUAL_DESK_BY_EMPLOYEE.put("1213_Grace_Breen", "01HA6T3QZVDX0TEZ863NFFSBHC");
        ACTUAL_DESK_BY_EMPLOYEE.put("1251_Tim_La_Roche", "01HA6T4FPVHEZ4VMNMS4GRG2VF");
        ACTUAL_DESK_BY_EMPLOYEE.put("630_Max_Jones", "01G6X1BH8KW99J1CSZM6NFE8F5");
        ACTUAL_DESK_BY_EMPLOYEE.put("1231_Vivien_Tang", "01G6X1C2DRMD02PR1SF58PHZQN");
        ACTUAL_DESK_BY_EMPLOYEE.put("1230_Nikke_San", "01G6X1C331RQZ5RHJ3VPHHZCAR");
        ACTUAL_DESK_BY_EMPLOYEE.put("1229_Gemma_Allwright", "01G6X1C2RSCA62EFT7NA4VXSN6");
        ACTUAL_DESK_BY_EMPLOYEE.put("1220_Alex_Maimescu", "01HA6T44CAEPEA2M2DRWS743SK");
        ACTUAL_DESK_BY_EMPLOYEE.put("1160_Darnell_Moore", "01G6X1BVT6MNB56FQ4XXKC55M5");
        ACTUAL_DESK_BY_EMPLOYEE.put("2097_Andrew_James_Hopkins", "01G6X1BM7EJPVGB9ZVDQPXE0XY");
        ACTUAL_DESK_BY_EMPLOYEE.put("1901_Hamish_Proudlock", "01G6X1BYF3XERKW9M7JWTT08J1");
        ACTUAL_DESK_BY_EMPLOYEE.put("2099_Sophie_Wilson", "01G6X1BY4F657SDRYDDN7W52QJ");
        ACTUAL_DESK_BY_EMPLOYEE.put("233_Jordan_Payne", "01G6X1B47Z4ZM5RW4K1VT6FZ2S");
    }

    @Test
    void harAssignment() throws Exception {
        List<Desk> realDesks = desks.stream()
                .filter(d -> d.getNeighborhood() != null && !d.getNeighborhood().equals("Desk Pods"))
                .toList();

        List<BookingRequest> bookings = HAR_EMPLOYEE_IDS.stream()
                .map(id -> new BookingRequest(id, SocialPreference.NONE, false, false))
                .collect(Collectors.toList());

        OrgChartService orgChartService = new OrgChartService() {
            @Override public Map<String, Employee> getEmployees() { return employees; }
            @Override public Map<String, OrgNode> getOrgNodes() { return orgNodes; }
        };

        // ── Actual Kadence assignment (filter to real fixed desks only) ──────────
        Set<String> realDeskIds = realDesks.stream().map(Desk::getId).collect(Collectors.toSet());
        Map<String, String> actualFiltered = ACTUAL_DESK_BY_EMPLOYEE.entrySet().stream()
                .filter(e -> realDeskIds.contains(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
        System.out.printf("%nActual Kadence: %d of %d employees on real fixed desks%n",
                actualFiltered.size(), ACTUAL_DESK_BY_EMPLOYEE.size());
        printMetrics("Actual Kadence", actualFiltered);

        // ── Random baseline ─────────────────────────────────────────────────────
        printMetrics("Random baseline", new RandomAssignmentService(42).assign(bookings, realDesks).getDeskByEmployeeId());

        // ── Simulated Annealing ─────────────────────────────────────────────────
        long start = System.currentTimeMillis();
        AssignmentCollection saResult = new SimulatedAnnealingAssignmentService(orgChartService)
                .assign(bookings, realDesks);
        System.out.printf("SA elapsed: %dms%n", System.currentTimeMillis() - start);
        printMetrics("Simulated Annealing", saResult.getDeskByEmployeeId());

        writeJsonOutput(bookings, saResult);
    }

    // ---------------------------------------------------------------------------
    // JSON output — written to har-sa-output.json (separate from sa-output.json)
    // ---------------------------------------------------------------------------

    private void writeJsonOutput(List<BookingRequest> bookings, AssignmentCollection result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        ArrayNode empArr = root.putArray("employees");
        ArrayNode nodeArr = root.putArray("orgNodes");
        for (BookingRequest b : bookings) {
            String id = b.getEmployeeId();
            Employee emp = employees.get(id);
            ObjectNode e = empArr.addObject();
            e.put("id", emp.getId());
            e.put("name", emp.getName());
            e.put("role", emp.getRole());
            e.put("location", emp.getLocation());

            OrgNode on = orgNodes.get(id);
            ObjectNode n = nodeArr.addObject();
            n.put("employeeId", on.getEmployeeId());
            if (on.getParentId() == null) n.putNull("parentId"); else n.put("parentId", on.getParentId());
            ArrayNode ch = n.putArray("childrenIds");
            on.getChildrenIds().forEach(ch::add);
            n.put("depth", on.getDepth());
            ArrayNode op = n.putArray("orgPath");
            on.getOrgPath().forEach(op::add);
        }

        ObjectNode deskByEmp = root.putObject("deskByEmployeeId");
        result.getDeskByEmployeeId().forEach(deskByEmp::put);
        ObjectNode empByDesk = root.putObject("employeeByDeskId");
        result.getEmployeeByDeskId().forEach(empByDesk::put);

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("har-sa-output.json"), root);
        System.out.println("Wrote har-sa-output.json");
    }

    // ---------------------------------------------------------------------------
    // Metrics
    // ---------------------------------------------------------------------------

    private void printMetrics(String label, Map<String, String> deskByEmployee) {
        DoubleSummaryStatistics siblingStats = new DoubleSummaryStatistics();
        DoubleSummaryStatistics sameTeamStats = new DoubleSummaryStatistics();
        DoubleSummaryStatistics crossTeamStats = new DoubleSummaryStatistics();

        List<String> empIds = new ArrayList<>(deskByEmployee.keySet());
        for (int i = 0; i < empIds.size(); i++) {
            for (int j = i + 1; j < empIds.size(); j++) {
                String a = empIds.get(i), b = empIds.get(j);
                OrgNode na = orgNodes.get(a), nb = orgNodes.get(b);
                if (na == null || nb == null) continue;
                int treeDist = treeDistance(na, nb);
                Desk da = deskById.get(deskByEmployee.get(a));
                Desk db = deskById.get(deskByEmployee.get(b));
                if (da == null || db == null) continue;
                double dist = euclidean(da, db);
                if (treeDist == 1) siblingStats.accept(dist);
                else if (treeDist <= 5) sameTeamStats.accept(dist);
                else crossTeamStats.accept(dist);
            }
        }

        double ratio = siblingStats.getCount() > 0 && crossTeamStats.getCount() > 0
                ? crossTeamStats.getAverage() / siblingStats.getAverage() : 0;

        System.out.println("\n── QAP Metrics (" + label + ") " + "─".repeat(Math.max(0, 60 - label.length())));
        System.out.printf("  Direct siblings    (tree dist=1):  %6.0f units avg over %5d pairs%n",
                siblingStats.getAverage(), siblingStats.getCount());
        System.out.printf("  Same sub-org       (tree dist 2-5): %6.0f units avg over %5d pairs%n",
                sameTeamStats.getAverage(), sameTeamStats.getCount());
        System.out.printf("  Cross-team         (tree dist 6+):  %6.0f units avg over %5d pairs%n",
                crossTeamStats.getAverage(), crossTeamStats.getCount());
        System.out.printf("  Clustering ratio (cross/sibling):   %.2fx%n", ratio);
    }

    private double euclidean(Desk a, Desk b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private int treeDistance(OrgNode a, OrgNode b) {
        List<String> pa = a.getOrgPath(), pb = b.getOrgPath();
        int minLen = Math.min(pa.size(), pb.size()), lcaDepth = -1;
        for (int i = 0; i < minLen; i++) {
            if (pa.get(i).equals(pb.get(i))) lcaDepth = i; else break;
        }
        if (lcaDepth < 0) return Integer.MAX_VALUE / 2;
        return (pa.size() - 1 - lcaDepth) + (pb.size() - 1 - lcaDepth);
    }
}
