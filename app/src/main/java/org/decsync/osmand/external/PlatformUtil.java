package org.decsync.osmand.external;

// Stripped from net.osmand.PlatformUtil
// <https://github.com/osmandapp/OsmAnd/blob/c62bd0ba46126e906baac560c47e9b29c840bc69/OsmAnd-java/src/main/java/net/osmand/PlatformUtil.java>

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PlatformUtil {
    public static XmlPullParser newXMLPullParser() throws XmlPullParserException{
        org.kxml2.io.KXmlParser xmlParser = new org.kxml2.io.KXmlParser();
        xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        return xmlParser;
    }
}