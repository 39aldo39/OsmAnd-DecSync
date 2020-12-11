package org.decsync.osmand.external;

// Stripped from net.osmand.util.Algorithms
// <https://github.com/osmandapp/OsmAnd/blob/1fd01594a08279c2bf698f83c2c353d719345414/OsmAnd-java/src/main/java/net/osmand/util/Algorithms.java>

/**
 * Basic algorithms that are not in jdk
 */
public class Algorithms {
    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    public static boolean objectEquals(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    /**
     * Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     */
    public static int parseColor(String colorString) throws IllegalArgumentException {
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            if (colorString.length() == 4) {
                colorString = "#" +
                        colorString.charAt(1) + colorString.charAt(1) +
                        colorString.charAt(2) + colorString.charAt(2) +
                        colorString.charAt(3) + colorString.charAt(3);
            }
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0x00000000ff000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
            }
            return (int) color;
        }
        throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
    }

    public static String colorToString(int color) {
        if ((0xFF000000 & color) == 0xFF000000) {
            return "#" + format(6, Integer.toHexString(color & 0x00FFFFFF)); //$NON-NLS-1$
        } else {
            return "#" + format(8, Integer.toHexString(color)); //$NON-NLS-1$
        }
    }

    private static String format(int i, String hexString) {
        while (hexString.length() < i) {
            hexString = "0" + hexString;
        }
        return hexString;
    }
}