package scripts;

import org.powerbot.script.rt4.GeItem;

/**
 * Created by noemailgmail on 4/14/2017.
 */
public class Util {

    public static String formatTime(long time) {
        long l;
        String s;
        l = Math.abs((time / 3600000) % 24);
        if (l < 10) {
            s = "0" + (int) l + ":";
        } else {
            s = l + ":";
        }
        l = Math.abs((time / 60000) % 60);
        if (l < 10) {
            s += "0" + (int) l + ":";
        } else {
            s += l + ":";
        }
        l = Math.abs((time / 1000) % 60);
        if (l < 10) {
            s += "0" + (int) l;
        } else {
            s += l;
        }
        return s;
    }

    public static int getGePrice(int item) {
        return new GeItem(item).price;
    }

    public static int[] getGePrice(int items[]) {
        int[] prices = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            prices[i] = getGePrice(items[i]);
        }
        return prices;
    }
}