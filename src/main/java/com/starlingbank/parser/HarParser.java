package com.starlingbank.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class HarParser {

    private static final java.util.regex.Pattern FLOOR_ID_PATTERN =
            java.util.regex.Pattern.compile("/floors/([A-Z0-9]+)/past-future-spaces");
    private static final java.util.regex.Pattern BUILDING_ID_PATTERN =
            java.util.regex.Pattern.compile("[?&]building\\.id=([A-Z0-9]+)");

    public static class HarExtract {
        public final String svgText;
        public final String apiJsonText;
        public final String floorId;
        public final String floorName;
        public final String buildingName;

        public HarExtract(String svgText, String apiJsonText,
                          String floorId, String floorName, String buildingName) {
            this.svgText = svgText;
            this.apiJsonText = apiJsonText;
            this.floorId = floorId;
            this.floorName = floorName;
            this.buildingName = buildingName;
        }
    }

    public HarExtract parse(Path harPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(harPath.toFile());
        JsonNode entries = root.path("log").path("entries");

        // Pass 1 — collect raw data; floor/building name resolution needs the floor ID first
        String svgText = null;
        String apiJsonText = null;
        String floorId = null;
        String buildingId = null;
        JsonNode floorsListJson = null;
        JsonNode buildingsListJson = null;

        for (JsonNode entry : entries) {
            String url = entry.path("request").path("url").asText("");
            String contentText = entry.path("response").path("content").path("text").asText("");

            if (svgText == null && url.contains(".svg") && !contentText.isEmpty()) {
                svgText = contentText;
            }
            if (url.contains("past-future-spaces")) {
                if (floorId == null) {
                    java.util.regex.Matcher m = FLOOR_ID_PATTERN.matcher(url);
                    if (m.find()) floorId = m.group(1);
                }
                if (apiJsonText == null && !contentText.isEmpty()) {
                    apiJsonText = contentText;
                }
            }
            if (buildingId == null && url.contains("v1/floors?") && !contentText.isEmpty()) {
                java.util.regex.Matcher m = BUILDING_ID_PATTERN.matcher(url);
                if (m.find()) buildingId = m.group(1);
                if (floorsListJson == null) floorsListJson = mapper.readTree(contentText);
            }
            if (buildingsListJson == null && url.contains("v1/buildings?") && !contentText.isEmpty()) {
                buildingsListJson = mapper.readTree(contentText);
            }
        }

        if (svgText == null)     throw new IllegalStateException("No SVG entry found in HAR");
        if (apiJsonText == null)  throw new IllegalStateException("No past-future-spaces response found in HAR");

        // Pass 2 — resolve names now that we have the floor ID
        String floorName = null;
        if (floorsListJson != null && floorId != null) {
            for (JsonNode floor : floorsListJson.path("hydra:member")) {
                if (floor.path("id").asText("").equals(floorId)) {
                    floorName = floor.path("name").asText(null);
                    break;
                }
            }
        }

        String buildingName = null;
        if (buildingsListJson != null && buildingId != null) {
            for (JsonNode building : buildingsListJson.path("hydra:member")) {
                if (building.path("id").asText("").equals(buildingId)) {
                    buildingName = building.path("name").asText(null);
                    break;
                }
            }
        }

        return new HarExtract(svgText, apiJsonText,
                floorId != null ? floorId : "unknown",
                floorName != null ? floorName : "Unknown Floor",
                buildingName != null ? buildingName : "Unknown Building");
    }
}
