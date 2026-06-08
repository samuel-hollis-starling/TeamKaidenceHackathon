package com.starlingbank.parser;

import com.starlingbank.model.Landmark;
import com.starlingbank.model.UnavailableSpace;
import com.starlingbank.model.ViewBox;
import com.starlingbank.model.Wall;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgParser {

    private static final Set<String> LANDMARK_IDS = new HashSet<>(Arrays.asList(
            "KITCHEN", "ELEVATOR", "RECEPTION", "RESTROOM", "STAIRS", "WORKPLACETECHBAR"
    ));

    private static final Pattern TRANSLATE_PATTERN = Pattern.compile("translate\\(([^)]+)\\)");
    private static final Pattern ROTATE_PATTERN = Pattern.compile("rotate\\(([^)]+)\\)");

    public static class SvgDesk {
        public final String id;
        public final double x;
        public final double y;
        public final double rotation;

        public SvgDesk(String id, double x, double y, double rotation) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }

    public static class SvgData {
        public final ViewBox viewBox;
        public final List<SvgDesk> desks;
        public final List<SvgDesk> pods;
        public final List<Wall> walls;
        public final List<Landmark> landmarks;
        public final List<UnavailableSpace> unavailableSpaces;

        public SvgData(ViewBox viewBox, List<SvgDesk> desks, List<SvgDesk> pods,
                       List<Wall> walls, List<Landmark> landmarks, List<UnavailableSpace> unavailableSpaces) {
            this.viewBox = viewBox;
            this.desks = desks;
            this.pods = pods;
            this.walls = walls;
            this.landmarks = landmarks;
            this.unavailableSpaces = unavailableSpaces;
        }
    }

    public SvgData parse(String svgText) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        // Prevent external DTD loading
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(svgText)));
        doc.getDocumentElement().normalize();

        // Build parent map
        Map<Element, Element> parentMap = new HashMap<>();
        buildParentMap(doc.getDocumentElement(), null, parentMap);

        // Extract viewBox from root SVG element
        Element svgRoot = doc.getDocumentElement();
        ViewBox viewBox = parseViewBox(svgRoot.getAttribute("viewBox"));

        List<SvgDesk> desks = new ArrayList<>();
        List<SvgDesk> pods = new ArrayList<>();
        List<Wall> walls = new ArrayList<>();
        List<Landmark> landmarks = new ArrayList<>();
        List<UnavailableSpace> unavailableSpaces = new ArrayList<>();

        // Walk all elements
        walkElements(doc.getDocumentElement(), parentMap, desks, pods, walls, landmarks, unavailableSpaces);

        return new SvgData(viewBox, desks, pods, walls, landmarks, unavailableSpaces);
    }

    private void buildParentMap(Element element, Element parent, Map<Element, Element> parentMap) {
        if (parent != null) {
            parentMap.put(element, parent);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                buildParentMap((Element) child, element, parentMap);
            }
        }
    }

    private void walkElements(Element element, Map<Element, Element> parentMap,
                              List<SvgDesk> desks, List<SvgDesk> pods,
                              List<Wall> walls, List<Landmark> landmarks,
                              List<UnavailableSpace> unavailableSpaces) {

        String id = element.getAttribute("id");
        String transform = element.getAttribute("transform");

        if (id != null && !id.isEmpty()) {
            if (id.startsWith("space::desk::")) {
                String ulid = id.substring("space::desk::".length());
                double[] xy = firstTranslate(transform);
                double rotation = firstRotate(transform);
                desks.add(new SvgDesk(ulid, xy[0], xy[1], rotation));

            } else if (id.startsWith("space::pod::")) {
                String ulid = id.substring("space::pod::".length());
                double[] xy = firstTranslate(transform);
                double rotation = firstRotate(transform);
                pods.add(new SvgDesk(ulid, xy[0], xy[1], rotation));

            } else if (id.equals("walls")) {
                // Extract walls group translate offset
                double[] offset = firstTranslate(transform);
                walls.addAll(parseWalls(element, offset));

            } else if (LANDMARK_IDS.contains(id)) {
                // The landmark type id is on the path element; position is from parent's first translate
                Element parent = parentMap.get(element);
                if (parent != null) {
                    String parentTransform = parent.getAttribute("transform");
                    double[] xy = firstTranslate(parentTransform);
                    landmarks.add(new Landmark(id, xy[0], xy[1]));
                }

            } else if (id.toLowerCase().startsWith("unavailable")) {
                double[] xy = firstTranslate(transform);
                double rotation = firstRotate(transform);
                unavailableSpaces.add(new UnavailableSpace(xy[0], xy[1], rotation));
            }
        }

        // Recurse into children
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                walkElements((Element) child, parentMap, desks, pods, walls, landmarks, unavailableSpaces);
            }
        }
    }

    private List<Wall> parseWalls(Element wallsGroup, double[] offset) {
        List<Wall> result = new ArrayList<>();
        NodeList children = wallsGroup.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                if ("polygon".equalsIgnoreCase(el.getTagName())) {
                    String polygonId = el.getAttribute("id");
                    String pointsAttr = el.getAttribute("points");
                    List<List<Double>> points = parsePolygonPoints(pointsAttr, offset);
                    result.add(new Wall(polygonId, points));
                }
            }
        }
        return result;
    }

    private List<List<Double>> parsePolygonPoints(String pointsAttr, double[] offset) {
        List<List<Double>> result = new ArrayList<>();
        if (pointsAttr == null || pointsAttr.trim().isEmpty()) {
            return result;
        }
        String[] tokens = pointsAttr.trim().split("[\\s,]+");
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            try {
                double x = Double.parseDouble(tokens[i]) + offset[0];
                double y = Double.parseDouble(tokens[i + 1]) + offset[1];
                List<Double> point = new ArrayList<>();
                point.add(x);
                point.add(y);
                result.add(point);
            } catch (NumberFormatException e) {
                // skip malformed pair
            }
        }
        return result;
    }

    private ViewBox parseViewBox(String viewBoxAttr) {
        if (viewBoxAttr == null || viewBoxAttr.trim().isEmpty()) {
            return new ViewBox(0, 0);
        }
        String[] parts = viewBoxAttr.trim().split("[\\s,]+");
        // Format: "minX minY width height"
        if (parts.length >= 4) {
            try {
                double width = Double.parseDouble(parts[2]);
                double height = Double.parseDouble(parts[3]);
                return new ViewBox(width, height);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return new ViewBox(0, 0);
    }

    /**
     * Extract the first translate(x, y) from a transform string.
     * Returns [x, y], or [0, 0] if not found.
     */
    private double[] firstTranslate(String transform) {
        if (transform == null || transform.isEmpty()) {
            return new double[]{0, 0};
        }
        Matcher m = TRANSLATE_PATTERN.matcher(transform);
        if (m.find()) {
            String inner = m.group(1);
            String[] parts = inner.trim().split("[,\\s]+");
            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0;
                return new double[]{x, y};
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return new double[]{0, 0};
    }

    /**
     * Extract the first rotate(angle, ...) from a transform string.
     * Returns the angle, or 0 if not found.
     */
    private double firstRotate(String transform) {
        if (transform == null || transform.isEmpty()) {
            return 0;
        }
        Matcher m = ROTATE_PATTERN.matcher(transform);
        if (m.find()) {
            String inner = m.group(1);
            String[] parts = inner.trim().split("[,\\s]+");
            try {
                return Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return 0;
    }
}
