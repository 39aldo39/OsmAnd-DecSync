package org.decsync.osmand.external;

// Stripped from net.osmand.plus.util.ColorDialogs
// <https://github.com/osmandapp/OsmAnd/blob/1fd01594a08279c2bf698f83c2c353d719345414/OsmAnd/src/net/osmand/plus/helpers/ColorDialogs.java>

import android.graphics.Color;

public class ColorDialogs {
    public static int[] pallette = new int[] {
            0xffeecc22,
            0xffd00d0d,
            0xffff5020,
            0xffeeee10,
            0xff88e030,
            0xff00842b,
            0xff10c0f0,
            0xff1010a0,
            0xffa71de1,
            0xffe044bb,
            0xff8e2512,
            0xff000001
    };

    public static String[] paletteColorTags = new String[] {
            "darkyellow",
            "red",
            "orange",
            "yellow",
            "lightgreen",
            "green",
            "lightblue",
            "blue",
            "purple",
            "pink",
            "brown",
            "black"
    };

    private static double getDistanceBetweenColors(int color1, int color2) {
        double distance;

        double r1 = Color.red(color1);
        double g1 = Color.green(color1);
        double b1 = Color.blue(color1);
        double a1 = Color.alpha(color1);

        double r2 = Color.red(color2);
        double g2 = Color.green(color2);
        double b2 = Color.blue(color2);
        double a2 = Color.alpha(color2);

        distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        if (distance == 0) {
            distance = Math.sqrt(Math.pow(a1 - a2, 2));
        }

        return distance;
    }

    public static int getNearestColor(int source, int[] colors) {
        double distance = Double.MAX_VALUE;

        int index = 0;
        for (int i = 0; i < colors.length; i++) {
            double newDistance = getDistanceBetweenColors(source, colors[i]);
            if (newDistance < distance) {
                index = i;
                distance = newDistance;
            }
        }

        return colors[index];
    }

    public static int getColorByTag(String tag) {
        String t = tag.toLowerCase();
        for (int i = 0; i < paletteColorTags.length; i++) {
            String colorTag = paletteColorTags[i];
            if (colorTag.equals(t)) {
                return pallette[i];
            }
        }
        return 0;
    }
}