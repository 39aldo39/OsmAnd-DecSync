package org.decsync.osmand.external;

// Stripped from net.osmand.GPXUtilities
// <https://github.com/osmandapp/OsmAnd/blob/1fd01594a08279c2bf698f83c2c353d719345414/OsmAnd-java/src/main/java/net/osmand/GPXUtilities.java>

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TimeZone;

public class GPXUtilities {

    public final static String TAG = "GPXUtilities";

    public final static String GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"; //$NON-NLS-1$
    private final static String GPX_TIME_FORMAT_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; //$NON-NLS-1$

    public enum GPXColor {
        BLACK(0xFF000000),
        DARKGRAY(0xFF444444),
        GRAY(0xFF888888),
        LIGHTGRAY(0xFFCCCCCC),
        WHITE(0xFFFFFFFF),
        RED(0xFFFF0000),
        GREEN(0xFF00FF00),
        DARKGREEN(0xFF006400),
        BLUE(0xFF0000FF),
        YELLOW(0xFFFFFF00),
        CYAN(0xFF00FFFF),
        MAGENTA(0xFFFF00FF),
        AQUA(0xFF00FFFF),
        FUCHSIA(0xFFFF00FF),
        DARKGREY(0xFF444444),
        GREY(0xFF888888),
        LIGHTGREY(0xFFCCCCCC),
        LIME(0xFF00FF00),
        MAROON(0xFF800000),
        NAVY(0xFF000080),
        OLIVE(0xFF808000),
        PURPLE(0xFF800080),
        SILVER(0xFFC0C0C0),
        TEAL(0xFF008080);

        int color;

        GPXColor(int color) {
            this.color = color;
        }

        public static GPXColor getColorFromName(String s) {
            for (GPXColor c : values()) {
                if (c.name().equalsIgnoreCase(s)) {
                    return c;
                }
            }
            return null;
        }
    }

    public static class GPXExtensions {
        Map<String, String> extensions = null;

        public Map<String, String> getExtensionsToRead() {
            if (extensions == null) {
                return Collections.emptyMap();
            }
            return extensions;
        }

        public Map<String, String> getExtensionsToWrite() {
            if (extensions == null) {
                extensions = new LinkedHashMap<>();
            }
            return extensions;
        }

        public int getColor(int defColor) {
            String clrValue = null;
            if (extensions != null) {
                clrValue = extensions.get("color");
                if (clrValue == null) {
                    clrValue = extensions.get("colour");
                }
                if (clrValue == null) {
                    clrValue = extensions.get("displaycolor");
                }
                if (clrValue == null) {
                    clrValue = extensions.get("displaycolour");
                }
            }
            return parseColor(clrValue, defColor);
        }

        public void setColor(int color) {
            getExtensionsToWrite().put("color", Algorithms.colorToString(color));
        }

        protected int parseColor(String colorString, int defColor) {
            if (!Algorithms.isEmpty(colorString)) {
                if (colorString.charAt(0) == '#') {
                    long color = Long.parseLong(colorString.substring(1), 16);
                    if (colorString.length() <= 7) {
                        color |= 0x00000000ff000000;
                    } else if (colorString.length() != 9) {
                        return defColor;
                    }
                    return (int) color;
                } else {
                    GPXColor c = GPXColor.getColorFromName(colorString);
                    if (c != null) {
                        return c.color;
                    }
                }
            }
            return defColor;
        }
    }

    public static class WptPt extends GPXExtensions {
        public double lat;
        public double lon;
        public String name = null;
        public String link = null;
        // previous undocumented feature 'category' ,now 'type'
        public String category = null;
        public String desc = null;
        public String comment = null;
        // by default
        public long time = 0;
        public double ele = Double.NaN;
        public double speed = 0;
        public double hdop = Double.NaN;

        public WptPt() {
        }

        public int getColor() {
            return getColor(0);
        }

        public boolean isVisible() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((category == null) ? 0 : category.hashCode());
            result = prime * result + ((desc == null) ? 0 : desc.hashCode());
            result = prime * result + ((comment == null) ? 0 : comment.hashCode());
            result = prime * result + ((lat == 0) ? 0 : Double.valueOf(lat).hashCode());
            result = prime * result + ((lon == 0) ? 0 : Double.valueOf(lon).hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            WptPt other = (WptPt) obj;
            return Algorithms.objectEquals(other.name, name)
                    && Algorithms.objectEquals(other.category, category)
                    && Algorithms.objectEquals(other.lat, lat)
                    && Algorithms.objectEquals(other.lon, lon)
                    && Algorithms.objectEquals(other.desc, desc);
        }
    }

    public static class TrkSegment extends GPXExtensions {
        public List<WptPt> points = new ArrayList<>();

        public List<RouteSegment> routeSegments = new ArrayList<>();
        public List<RouteType> routeTypes = new ArrayList<>();
    }

    public static class Track extends GPXExtensions {
        public String name = null;
        public String desc = null;
        public List<TrkSegment> segments = new ArrayList<>();

    }

    public static class Route extends GPXExtensions {
        public String name = null;
        public String desc = null;
        public List<WptPt> points = new ArrayList<>();

    }

    public static class Metadata extends GPXExtensions {

        public String name;
        public String desc;
        public String link;
        public String keywords;
        public long time = 0;
        public Author author = null;
        public Copyright copyright = null;
        public Bounds bounds = null;
    }

    public static class Author extends GPXExtensions {
        public String name;
        public String email;
        public String link;
    }

    public static class Copyright extends GPXExtensions {
        public String author;
        public String year;
        public String license;
    }

    public static class Bounds extends GPXExtensions {
        public double minlat;
        public double minlon;
        public double maxlat;
        public double maxlon;
    }

    public static class RouteSegment {
        public String id;
        public String length;
        public String segmentTime;
        public String speed;
        public String turnType;
        public String turnAngle;
        public String types;
        public String pointTypes;
        public String names;
    }

    public static class RouteType {
        public String tag;
        public String value;
    }

    public static class GPXFile extends GPXExtensions {
        public String author;
        public Metadata metadata;
        public List<Track> tracks = new ArrayList<>();
        private List<WptPt> points = new ArrayList<>();
        public List<Route> routes = new ArrayList<>();

        public Exception error = null;
        public String path = "";
        public long modifiedTime = 0;

        public GPXFile(String author) {
            this.author = author;
        }

        public List<WptPt> getPoints() {
            return Collections.unmodifiableList(points);
        }
    }

    private static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
        int tok;
        StringBuilder text = null;
        while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (tok == XmlPullParser.END_TAG && parser.getName().equals(key)) {
                break;
            } else if (tok == XmlPullParser.TEXT) {
                if (text == null) {
                    text = new StringBuilder(parser.getText());
                } else {
                    text.append(parser.getText());
                }
            }
        }
        return text == null ? null : text.toString();
    }

    private static Map<String, String> readTextMap(XmlPullParser parser, String key)
            throws XmlPullParserException, IOException {
        int tok;
        StringBuilder text = null;
        Map<String, String> result = new HashMap<>();
        while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (tok == XmlPullParser.END_TAG) {
                String tag = parser.getName();
                if (text != null && !Algorithms.isEmpty(text.toString().trim())) {
                    result.put(tag, text.toString());
                }
                if (tag.equals(key)) {
                    break;
                }
                text = null;
            } else if (tok == XmlPullParser.START_TAG) {
                text = null;
            } else if (tok == XmlPullParser.TEXT) {
                if (text == null) {
                    text = new StringBuilder(parser.getText());
                } else {
                    text.append(parser.getText());
                }
            }
        }
        return result;
    }

    private static long parseTime(String text,SimpleDateFormat format,SimpleDateFormat formatMillis) {
        long time = 0;
        if (text != null) {
            try {
                time = format.parse(text).getTime();
            } catch (ParseException e1) {
                try {
                    time = formatMillis.parse(text).getTime();
                } catch (ParseException e2) {

                }
            }
        }
        return time;
    }

    public static GPXFile loadGPXFile(File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            GPXFile file = loadGPXFile(fis);
            file.path = f.getAbsolutePath();
            file.modifiedTime = f.lastModified();

            try {
                fis.close();
            } catch (IOException e) {
            }
            return file;
        } catch (IOException e) {
            GPXFile res = new GPXFile(null);
            res.path = f.getAbsolutePath();
            Log.e(TAG, "Error reading gpx " + res.path, e); //$NON-NLS-1$
            res.error = e;
            return res;
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    public static GPXFile loadGPXFile(InputStream f) {
        GPXFile gpxFile = new GPXFile(null);
        SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat formatMillis = new SimpleDateFormat(GPX_TIME_FORMAT_MILLIS, Locale.US);
        formatMillis.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            XmlPullParser parser = new org.kxml2.io.KXmlParser();
            parser.setInput(getUTF8Reader(f));
            Track routeTrack = new Track();
            TrkSegment routeTrackSegment = new TrkSegment();
            routeTrack.segments.add(routeTrackSegment);
            Stack<GPXExtensions> parserState = new Stack<>();
            TrkSegment firstSegment = null;
            boolean extensionReadMode = false;
            boolean routePointExtension = false;
            List<RouteSegment> routeSegments = new ArrayList<>();
            List<RouteType> routeTypes = new ArrayList<>();
            boolean routeExtension = false;
            boolean typesExtension = false;
            parserState.push(gpxFile);
            int tok;
            while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (tok == XmlPullParser.START_TAG) {
                    GPXExtensions parse = parserState.peek();
                    String tag = parser.getName();
                    if (extensionReadMode && parse != null && !routePointExtension) {
                        String tagName = tag.toLowerCase();
                        if (routeExtension) {
                            if (tagName.equals("segment")) {
                                RouteSegment segment = parseRouteSegmentAttributes(parser);
                                routeSegments.add(segment);
                            }
                        } else if (typesExtension) {
                            if (tagName.equals("type")) {
                                RouteType type = parseRouteTypeAttributes(parser);
                                routeTypes.add(type);
                            }
                        }
                        switch (tagName) {
                            case "routepointextension":
                                routePointExtension = true;
                                if (parse instanceof WptPt) {
                                    parse.getExtensionsToWrite().put("offset", routeTrackSegment.points.size() + "");
                                }
                                break;

                            case "route":
                                routeExtension = true;
                                break;

                            case "types":
                                typesExtension = true;
                                break;

                            default:
                                Map<String, String> values = readTextMap(parser, tag);
                                if (values.size() > 0) {
                                    for (Entry<String, String> entry : values.entrySet()) {
                                        String t = entry.getKey().toLowerCase();
                                        String value = entry.getValue();
                                        parse.getExtensionsToWrite().put(t, value);
                                        if (tag.equals("speed") && parse instanceof WptPt) {
                                            try {
                                                ((WptPt) parse).speed = Float.parseFloat(value);
                                            } catch (NumberFormatException e) {
                                                Log.d(TAG, e.getMessage(), e);
                                            }
                                        }
                                    }
                                }
                                break;
                        }
                    } else if (parse != null && tag.equals("extensions")) {
                        extensionReadMode = true;
                    } else if (routePointExtension) {
                        if (tag.equals("rpt")) {
                            WptPt wptPt = parseWptAttributes(parser);
                            routeTrackSegment.points.add(wptPt);
                            parserState.push(wptPt);
                        }
                    } else {
                        if (parse instanceof GPXFile) {
                            if (tag.equals("gpx")) {
                                ((GPXFile) parse).author = parser.getAttributeValue("", "creator");
                            }
                            if (tag.equals("metadata")) {
                                Metadata metadata = new Metadata();
                                ((GPXFile) parse).metadata = metadata;
                                parserState.push(metadata);
                            }
                            if (tag.equals("trk")) {
                                Track track = new Track();
                                ((GPXFile) parse).tracks.add(track);
                                parserState.push(track);
                            }
                            if (tag.equals("rte")) {
                                Route route = new Route();
                                ((GPXFile) parse).routes.add(route);
                                parserState.push(route);
                            }
                            if (tag.equals("wpt")) {
                                WptPt wptPt = parseWptAttributes(parser);
                                ((GPXFile) parse).points.add(wptPt);
                                parserState.push(wptPt);
                            }
                        } else if (parse instanceof Metadata) {
                            if (tag.equals("name")) {
                                ((Metadata) parse).name = readText(parser, "name");
                            }
                            if (tag.equals("desc")) {
                                ((Metadata) parse).desc = readText(parser, "desc");
                            }
                            if (tag.equals("author")) {
                                Author author = new Author();
                                author.name = parser.getText();
                                ((Metadata) parse).author = author;
                                parserState.push(author);
                            }
                            if (tag.equals("copyright")) {
                                Copyright copyright = new Copyright();
                                copyright.license = parser.getText();
                                copyright.author = parser.getAttributeValue("", "author");
                                ((Metadata) parse).copyright = copyright;
                                parserState.push(copyright);
                            }
                            if (tag.equals("link")) {
                                ((Metadata) parse).link = parser.getAttributeValue("", "href");
                            }
                            if (tag.equals("time")) {
                                String text = readText(parser, "time");
                                ((Metadata) parse).time = parseTime(text, format, formatMillis);
                            }
                            if (tag.equals("keywords")) {
                                ((Metadata) parse).keywords = readText(parser, "keywords");
                            }
                            if (tag.equals("bounds")) {
                                Bounds bounds = parseBoundsAttributes(parser);
                                ((Metadata) parse).bounds = bounds;
                                parserState.push(bounds);
                            }
                        } else if (parse instanceof Author) {
                            if (tag.equals("name")) {
                                ((Author) parse).name = readText(parser, "name");
                            }
                            if (tag.equals("email")) {
                                String id = parser.getAttributeValue("", "id");
                                String domain = parser.getAttributeValue("", "domain");
                                if (!Algorithms.isEmpty(id) && !Algorithms.isEmpty(domain)) {
                                    ((Author) parse).email = id + "@" + domain;
                                }
                            }
                            if (tag.equals("link")) {
                                ((Author) parse).link = parser.getAttributeValue("", "href");
                            }
                        } else if (parse instanceof Copyright) {
                            if (tag.equals("year")) {
                                ((Copyright) parse).year = readText(parser, "year");
                            }
                            if (tag.equals("license")) {
                                ((Copyright) parse).license = readText(parser, "license");
                            }
                        } else if (parse instanceof Route) {
                            if (tag.equals("name")) {
                                ((Route) parse).name = readText(parser, "name");
                            }
                            if (tag.equals("desc")) {
                                ((Route) parse).desc = readText(parser, "desc");
                            }
                            if (tag.equals("rtept")) {
                                WptPt wptPt = parseWptAttributes(parser);
                                ((Route) parse).points.add(wptPt);
                                parserState.push(wptPt);
                            }
                        } else if (parse instanceof Track) {
                            if (tag.equals("name")) {
                                ((Track) parse).name = readText(parser, "name");
                            } else if (tag.equals("desc")) {
                                ((Track) parse).desc = readText(parser, "desc");
                            } else if (tag.equals("trkseg")) {
                                TrkSegment trkSeg = new TrkSegment();
                                ((Track) parse).segments.add(trkSeg);
                                parserState.push(trkSeg);
                            } else if (tag.equals("trkpt") || tag.equals("rpt")) {
                                WptPt wptPt = parseWptAttributes(parser);
                                int size = ((Track) parse).segments.size();
                                if (size == 0) {
                                    ((Track) parse).segments.add(new TrkSegment());
                                    size++;
                                }
                                ((Track) parse).segments.get(size - 1).points.add(wptPt);
                                parserState.push(wptPt);
                            }
                        } else if (parse instanceof TrkSegment) {
                            if (tag.equals("trkpt") || tag.equals("rpt")) {
                                WptPt wptPt = parseWptAttributes(parser);
                                ((TrkSegment) parse).points.add(wptPt);
                                parserState.push(wptPt);
                            }
                            if (tag.equals("csvattributes")) {
                                String segmentPoints = readText(parser, "csvattributes");
                                String[] pointsArr = segmentPoints.split("\n");
                                for (int i = 0; i < pointsArr.length; i++) {
                                    String[] pointAttrs = pointsArr[i].split(",");
                                    try {
                                        int arrLength = pointsArr.length;
                                        if (arrLength > 1) {
                                            WptPt wptPt = new WptPt();
                                            wptPt.lon = Double.parseDouble(pointAttrs[0]);
                                            wptPt.lat = Double.parseDouble(pointAttrs[1]);
                                            ((TrkSegment) parse).points.add(wptPt);
                                            if (arrLength > 2) {
                                                wptPt.ele = Double.parseDouble(pointAttrs[2]);
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                    }
                                }
                            }
                            // main object to parse
                        } else if (parse instanceof WptPt) {
                            if (tag.equals("name")) {
                                ((WptPt) parse).name = readText(parser, "name");
                            } else if (tag.equals("desc")) {
                                ((WptPt) parse).desc = readText(parser, "desc");
                            } else if (tag.equals("cmt")) {
                                ((WptPt) parse).comment = readText(parser, "cmt");
                            } else if (tag.equals("speed")) {
                                try {
                                    String value = readText(parser, "speed");
                                    if (!Algorithms.isEmpty(value)) {
                                        ((WptPt) parse).speed = Float.parseFloat(value);
                                        parse.getExtensionsToWrite().put("speed", value);
                                    }
                                } catch (NumberFormatException e) {
                                }
                            } else if (tag.equals("link")) {
                                ((WptPt) parse).link = parser.getAttributeValue("", "href");
                            } else if (tag.equals("category")) {
                                ((WptPt) parse).category = readText(parser, "category");
                            } else if (tag.equals("type")) {
                                if (((WptPt) parse).category == null) {
                                    ((WptPt) parse).category = readText(parser, "type");
                                }
                            } else if (tag.equals("ele")) {
                                String text = readText(parser, "ele");
                                if (text != null) {
                                    try {
                                        ((WptPt) parse).ele = Float.parseFloat(text);
                                    } catch (NumberFormatException e) {
                                    }
                                }
                            } else if (tag.equals("hdop")) {
                                String text = readText(parser, "hdop");
                                if (text != null) {
                                    try {
                                        ((WptPt) parse).hdop = Float.parseFloat(text);
                                    } catch (NumberFormatException e) {
                                    }
                                }
                            } else if (tag.equals("time")) {
                                String text = readText(parser, "time");
                                ((WptPt) parse).time = parseTime(text, format, formatMillis);
                            }
                        }
                    }

                } else if (tok == XmlPullParser.END_TAG) {
                    Object parse = parserState.peek();
                    String tag = parser.getName();

                    if (tag.toLowerCase().equals("routepointextension")) {
                        routePointExtension = false;
                    }
                    if (parse != null && tag.equals("extensions")) {
                        extensionReadMode = false;
                    }
                    if (extensionReadMode && tag.equals("route")) {
                        routeExtension = false;
                        continue;
                    }
                    if (extensionReadMode && tag.equals("types")) {
                        typesExtension = false;
                        continue;
                    }

                    if (tag.equals("metadata")) {
                        Object pop = parserState.pop();
                        assert pop instanceof Metadata;
                    } else if (tag.equals("author")) {
                        if (parse instanceof Author) {
                            parserState.pop();
                        }
                    } else if (tag.equals("copyright")) {
                        if (parse instanceof Copyright) {
                            parserState.pop();
                        }
                    } else if (tag.equals("bounds")) {
                        if (parse instanceof Bounds) {
                            parserState.pop();
                        }
                    } else if (tag.equals("trkpt")) {
                        Object pop = parserState.pop();
                        assert pop instanceof WptPt;
                    } else if (tag.equals("wpt")) {
                        Object pop = parserState.pop();
                        assert pop instanceof WptPt;
                    } else if (tag.equals("rtept")) {
                        Object pop = parserState.pop();
                        assert pop instanceof WptPt;
                    } else if (tag.equals("trk")) {
                        Object pop = parserState.pop();
                        assert pop instanceof Track;
                    } else if (tag.equals("rte")) {
                        Object pop = parserState.pop();
                        assert pop instanceof Route;
                    } else if (tag.equals("trkseg")) {
                        Object pop = parserState.pop();
                        if (pop instanceof TrkSegment) {
                            TrkSegment segment = (TrkSegment) pop;
                            segment.routeSegments = routeSegments;
                            segment.routeTypes = routeTypes;
                            routeSegments = new ArrayList<>();
                            routeTypes = new ArrayList<>();
                            if (firstSegment == null) {
                                firstSegment = segment;
                            }
                        }
                        assert pop instanceof TrkSegment;
                    } else if (tag.equals("rpt")) {
                        Object pop = parserState.pop();
                        assert pop instanceof WptPt;
                    }
                }
            }
            if (!routeTrackSegment.points.isEmpty()) {
                gpxFile.tracks.add(routeTrack);
            }
            if (!routeSegments.isEmpty() && !routeTypes.isEmpty() && firstSegment != null) {
                firstSegment.routeSegments = routeSegments;
                firstSegment.routeTypes = routeTypes;
            }
        } catch (Exception e) {
            gpxFile.error = e;
            Log.e(TAG, "Error reading gpx", e); //$NON-NLS-1$
        }
        return gpxFile;
    }

    private static Reader getUTF8Reader(InputStream f) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(f);
        assert bis.markSupported();
        bis.mark(3);
        boolean reset = true;
        byte[] t = new byte[3];
        bis.read(t);
        if (t[0] == ((byte) 0xef) && t[1] == ((byte) 0xbb) && t[2] == ((byte) 0xbf)) {
            reset = false;
        }
        if (reset) {
            bis.reset();
        }
        return new InputStreamReader(bis, "UTF-8");
    }

    private static WptPt parseWptAttributes(XmlPullParser parser) {
        WptPt wpt = new WptPt();
        try {
            wpt.lat = Double.parseDouble(parser.getAttributeValue("", "lat")); //$NON-NLS-1$ //$NON-NLS-2$
            wpt.lon = Double.parseDouble(parser.getAttributeValue("", "lon")); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (NumberFormatException e) {
            // ignore
        }
        return wpt;
    }

    private static RouteSegment parseRouteSegmentAttributes(XmlPullParser parser) {
        RouteSegment segment = new RouteSegment();
        segment.id = parser.getAttributeValue("", "id");
        segment.length = parser.getAttributeValue("", "length");
        segment.segmentTime = parser.getAttributeValue("", "segmentTime");
        segment.speed = parser.getAttributeValue("", "speed");
        segment.turnType = parser.getAttributeValue("", "turnType");
        segment.turnAngle = parser.getAttributeValue("", "turnAngle");
        segment.types = parser.getAttributeValue("", "types");
        segment.pointTypes = parser.getAttributeValue("", "pointTypes");
        segment.names = parser.getAttributeValue("", "names");
        return segment;
    }

    private static RouteType parseRouteTypeAttributes(XmlPullParser parser) {
        RouteType type = new RouteType();
        type.tag = parser.getAttributeValue("", "t");
        type.value = parser.getAttributeValue("", "v");
        return type;
    }

    private static Bounds parseBoundsAttributes(XmlPullParser parser) {
        Bounds bounds = new Bounds();
        try {
            String minlat = parser.getAttributeValue("", "minlat");
            String minlon = parser.getAttributeValue("", "minlon");
            String maxlat = parser.getAttributeValue("", "maxlat");
            String maxlon = parser.getAttributeValue("", "maxlon");

            if (minlat == null) {
                minlat = parser.getAttributeValue("", "minLat");
            }
            if (minlon == null) {
                minlon = parser.getAttributeValue("", "minLon");
            }
            if (maxlat == null) {
                maxlat = parser.getAttributeValue("", "maxLat");
            }
            if (maxlat == null) {
                maxlon = parser.getAttributeValue("", "maxLon");
            }

            if (minlat != null) {
                bounds.minlat = Double.parseDouble(minlat);
            }
            if (minlon != null) {
                bounds.minlon = Double.parseDouble(minlon);
            }
            if (maxlat != null) {
                bounds.maxlat = Double.parseDouble(maxlat);
            }
            if (maxlon != null) {
                bounds.maxlon = Double.parseDouble(maxlon);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return bounds;
    }
}