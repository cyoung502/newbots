package scripts;

import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;

import java.awt.*;

public class Paint {

    public static void drawCrosshair(Graphics2D g, ClientContext ctx, int size){
        Point p = ctx.input.getLocation();
        g.setColor(new Color(255,55,53));
        g.drawLine(p.x - size, p.y - size, p.x + size, p.y + size);
    }

    public static void drawRectangleBordered(Graphics2D g, int x, int y, int width, int height, int thickness, Color background, Color border){
        g.setColor(border);
        g.fillRect(x - thickness, y - thickness, width + (thickness * 2), height + (thickness * 2));
        g.setColor(background);
        g.fillRect(x, y, width, height);
    }

    public static void drawTextShadow(Graphics2D g, int x, int y, Color text, Color shadow, Font font, String s){
        g.setFont(font);
        g.setColor(shadow);
        g.drawString(s,x+1,y+1);
        g.setColor(text);
        g.drawString(s,x,y);
    }

    public static void drawBorderPolygon(Graphics2D g, Polygon p){
        g.fillPolygon(p);
        g.setColor(new Color(0,0,0));
        for(int i = 0; i < p.npoints; i++){
            if(i == p.npoints - 1){
                g.drawLine(p.xpoints[i], p.ypoints[i], p.xpoints[0], p.ypoints[0]);
            } else {
                g.drawLine(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
            }
        }
    }
}
