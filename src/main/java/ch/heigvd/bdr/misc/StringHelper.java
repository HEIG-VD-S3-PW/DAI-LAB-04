package ch.heigvd.bdr.misc;

public class StringHelper {
    /**
     * Check if a string contains an integer
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
