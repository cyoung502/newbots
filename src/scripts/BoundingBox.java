
package scripts;

import org.powerbot.script.*;

/**
 * Created by noemailgmail on 3/7/2017.
 */

public class BoundingBox {

    private Tile topLeft;
    private Tile bottomRight;

    public BoundingBox(){
        topLeft = new Tile (0,0);
        bottomRight = new Tile(0,0);
    }

    public BoundingBox(Tile topLeft, Tile bottomRight){
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public Tile getTopLeft() {
        return topLeft;
    }

    public void setTopLeft(Tile topLeft) {
        this.topLeft = topLeft;
    }

    public Tile getBottomRight() {
        return bottomRight;
    }

    public void setBottomRight(Tile bottomRight) {
        this.bottomRight = bottomRight;
    }

    public boolean getCollision(Tile position) {
        return (position.x() >= topLeft.x() &&
                position.x() <= bottomRight.x() &&
                position.y() <= topLeft.y() &&
                position.y() >= bottomRight.y()
        );
    }
}