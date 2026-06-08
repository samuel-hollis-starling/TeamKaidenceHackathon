package com.starlingbank.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class HarParser {

    public static class HarExtract {
        public final String svgText;
        public final String apiJsonText;

        public HarExtract(String svgText, String apiJsonText) {
            this.svgText = svgText;
            this.apiJsonText = apiJsonText;
        }
    }

    public HarExtract parse(Path harPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(harPath.toFile());
        JsonNode entries = root.path("log").path("entries");

        String svgText = null;
        String apiJsonText = null;

        for (JsonNode entry : entries) {
            String url = entry.path("request").path("url").asText("");
            String contentText = entry.path("response").path("content").path("text").asText("");

            if (svgText == null && url.contains(".svg") && !contentText.isEmpty()) {
                svgText = contentText;
            }
            if (apiJsonText == null && url.contains("past-future-spaces") && !contentText.isEmpty()) {
                apiJsonText = contentText;
            }

            if (svgText != null && apiJsonText != null) {
                break;
            }
        }

        if (svgText == null) {
            throw new IllegalStateException("No SVG entry found in HAR file (url containing '.svg' with non-empty content)");
        }
        if (apiJsonText == null) {
            throw new IllegalStateException("No desk API entry found in HAR file (url containing 'past-future-spaces' with non-empty content)");
        }

        return new HarExtract(svgText, apiJsonText);
    }
}
