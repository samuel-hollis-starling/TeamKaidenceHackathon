package com.starlingbank.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starlingbank.model.Desk;
import com.starlingbank.model.FloorInfo;
import com.starlingbank.model.FloorMap;
import com.starlingbank.model.Pod;
import com.starlingbank.model.Spaces;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class FloorMapParser {


    private static class DeskMeta {
        final String name;
        final String neighborhood;
        final String type;

        DeskMeta(String name, String neighborhood, String type) {
            this.name = name;
            this.neighborhood = neighborhood;
            this.type = type;
        }
    }

    public FloorMap parse(Path harPath) throws Exception {
        HarParser harParser = new HarParser();
        HarParser.HarExtract extract = harParser.parse(harPath);

        SvgParser svgParser = new SvgParser();
        SvgParser.SvgData svgData = svgParser.parse(extract.svgText);

        // Parse API JSON
        Map<String, DeskMeta> metaById = parseApiJson(extract.apiJsonText);

        // Collect sorted unique neighborhoods
        TreeSet<String> neighborhoodSet = new TreeSet<>();
        for (DeskMeta meta : metaById.values()) {
            if (meta.neighborhood != null && !meta.neighborhood.isEmpty()) {
                neighborhoodSet.add(meta.neighborhood);
            }
        }
        List<String> neighborhoods = new ArrayList<>(neighborhoodSet);

        // Merge desks
        List<Desk> desks = new ArrayList<>();
        for (SvgParser.SvgDesk svgDesk : svgData.desks) {
            DeskMeta meta = metaById.get(svgDesk.id);
            if (meta != null) {
                desks.add(new Desk(svgDesk.id, meta.name, meta.neighborhood,
                        svgDesk.x, svgDesk.y, svgDesk.rotation));
            }
        }

        // Merge pods
        List<Pod> pods = new ArrayList<>();
        for (SvgParser.SvgDesk svgPod : svgData.pods) {
            DeskMeta meta = metaById.get(svgPod.id);
            if (meta != null) {
                pods.add(new Pod(svgPod.id, meta.name, meta.neighborhood,
                        svgPod.x, svgPod.y, svgPod.rotation));
            }
        }

        FloorInfo floorInfo = new FloorInfo(extract.floorId, extract.floorName, extract.buildingName, svgData.viewBox);
        Spaces spaces = new Spaces(desks, pods);

        return new FloorMap(floorInfo, neighborhoods, spaces, svgData.walls, svgData.landmarks, svgData.unavailableSpaces);
    }

    private Map<String, DeskMeta> parseApiJson(String apiJsonText) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(apiJsonText);
        JsonNode members = root.path("hydra:member");

        Map<String, DeskMeta> result = new HashMap<>();
        for (JsonNode member : members) {
            String id = member.path("id").asText(null);
            if (id == null || id.isEmpty()) {
                continue;
            }
            String name = member.path("name").asText(null);
            String type = member.path("type").asText(null);
            String neighborhood = null;
            JsonNode nbNode = member.path("neighborhood");
            if (!nbNode.isMissingNode() && !nbNode.isNull()) {
                neighborhood = nbNode.path("name").asText(null);
            }
            result.put(id, new DeskMeta(name, neighborhood, type));
        }
        return result;
    }
}
