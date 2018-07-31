package scripts;

import org.powerbot.Con;
import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;

import java.awt.*;
import java.util.concurrent.Callable;


@Script.Manifest(
        name = "Canifis Rooftop Agility",
        description = "sucky sucky 5 dolla.",
        properties = "client = 4;"
)

public class CanifisAgility extends PollingScript<ClientContext> implements PaintListener {

    //Tiles to click for obstacles
    public static final Tile TREE = new Tile(3508, 3489, 0);
    public static final Tile JUMPONE = new Tile(3506, 3498, 2);
    public static final Tile JUMPTWO = new Tile(3497, 3503, 2);
    public static final Tile JUMPTHREE = new Tile(3486, 3499, 2);
    public static final Tile JUMPFOUR = new Tile(3478, 3491, 3);
    public static final Tile POLEVAULT = new Tile(3480, 3483, 2);
    public static final Tile JUMPFIVE = new Tile(3504, 3476, 3);
    public static final Tile JUMPFINAL = new Tile(3510, 3483, 2);
    public static final Tile STUCK = new Tile(3487,3476,3);
    public static final Tile STUCK_TREE = new Tile(3505,3489,2);

    //Paths
    public static final Tile[] PATH_FAIL_FIVE = {
            new Tile(3506,3485,0),
            new Tile(3511,3485,0)
    };
    public static final Tile[] PATH_FAIL_THREE = {
            new Tile(3491,3491,0),
            new Tile(3502,3486,0),
            new Tile(3511,3485,0)
    };

    //    IDs for Objects
    public static final int MARKOFGRACE = 11849;

    //    BoundBoxes for rooftop
    public static final BoundingBox BB_TREE = new BoundingBox(new Tile(3507, 3487), new Tile(3512, 3484));
    public static final BoundingBox BB_JUMPONE = new BoundingBox(new Tile(3504, 3498), new Tile(3511, 3490));
    public static final BoundingBox BB_JUMPTWO = new BoundingBox(new Tile(3496, 3507), new Tile(3505, 3502));
    public static final BoundingBox BB_JUMPTHREE = new BoundingBox(new Tile(3485, 3504), new Tile(3494, 3497));
    public static final BoundingBox BB_JUMPFOUR = new BoundingBox(new Tile(3474, 3499), new Tile(3481, 3492));
    public static final BoundingBox BB_POLE = new BoundingBox(new Tile(3476, 3487), new Tile(3484, 3480));
    public static final BoundingBox BB_JUMPFIVE = new BoundingBox(new Tile(3488, 3477), new Tile(3504, 3467));
    public static final BoundingBox BB_JUMPFINAL = new BoundingBox(new Tile(3508, 3481), new Tile(3515, 3473));
    public static final BoundingBox BB_JUMPTHREE_FAIL = new BoundingBox(new Tile(3481, 3499), new Tile(3484, 3496));
    public static final BoundingBox BB_JUMPFIVE_FAIL = new BoundingBox(new Tile(3505,3478), new Tile(3508,3475));

    public static int lapCounter = 0;
    public Experience experience;
    public static final Font HEADING1 = new Font("Helvetica", Font.CENTER_BASELINE, 18);
    public static final Font BODY = new Font("Helvetica", Font.BOLD, 14);

    @Override
    public void repaint(Graphics graphics) {
        if (experience == null) {
            return;
        }
        final Graphics2D g = (Graphics2D) graphics;
        Paint.drawCrosshair(g, ctx, 3);
        g.setColor(new Color(35, 35, 35, 190));
        g.fillRect(0, 0, 200, 240);
        g.setColor(new Color(126, 33, 153));
        g.fillRect(0, 0, 200, 25);
        g.fillRect(5, 90, 190, 2);
        g.setColor(new Color(15, 18, 123));
        g.fillOval(12, 95, 25, 25);
        g.setColor(new Color(15, 137, 20));
        g.fillOval(63, 95, 25, 25);
        g.setColor(new Color(121, 25, 12));
        g.fillOval(112, 95, 25, 25);
        g.setColor(new Color(180, 178, 178));
        g.fillOval(163, 95, 25, 25);
        g.setColor(new Color(245, 245, 245));
        g.setFont(BODY);
        g.setColor(new Color(170, 170, 170));
        g.setFont(HEADING1);
        g.drawString(String.format("Canifis Agility"), 10, 20);
        g.setFont(BODY);
        g.drawString(String.format("Time Running: %s", formatTime(getRuntime())), 5, 40);
        g.drawString(String.format("XP+(HR): %,d (%,d/HR)",
                experience.getExperienceGained(Constants.SKILLS_AGILITY),
                experience.getExperienceHour(Constants.SKILLS_AGILITY)),
                5, 60);
        g.setColor(new Color(68, 68, 68));
        g.fillRect(10, 65, 180, 20);
        g.setColor(new Color(146, 211, 110));
        g.fillRect(10, 65, (int) (180 * (experience.getExperiencePercent(Constants.SKILLS_AGILITY) / 100d)), 20);
        g.setColor(new Color(3, 48, 118));
        g.drawString(String.format("%d%s to Lvl %d (%s)",
                experience.getExperiencePercent(Constants.SKILLS_AGILITY),
                "%",
                ctx.skills.realLevel(Constants.SKILLS_AGILITY) + 1,
                formatTime(experience.getTimeToLevel(Constants.SKILLS_AGILITY))),
                20, 80);
    }

    @Override
    public void start() {
        ctx.input.speed(10);
        experience = new Experience();

    }

    @Override
    public void poll() {
        State state = getState();
        System.out.println("State: " + state);
        System.out.println("Laps Completed: " + lapCounter);
        if (state == null) {
            return;
        }
        switch (state) {
            case CLIMB_TREE:
                climbTallTree();
                break;
            case JUMP_GAP_ONE:
                jumpGapOne();
                break;
            case JUMP_GAP_TWO:
                jumpGapTwo();
                break;
            case JUMP_GAP_THREE:
                jumpGapThree();
                break;
            case JUMP_GAP_FOUR:
                jumpGapFour();
                break;
            case POLE_VAULT:
                poleVault();
                break;
            case JUMP_GAP_FIVE:
                jumpGapFive();
                break;
            case JUMP_GAP_FINAL:
                finalJump();
                break;
            case GET_TOKEN:
                pickupMark();
                break;
            case STUCK:
                unstuck();
                break;
            case STUCK_TREE:
                unstuckTree();
                break;
            case FAIL_JUMP_THREE:
            case FAIL_JUMP_FIVE:
                returnToStart();
                break;
        }
        return;
    }

    private State getState() {
        if (!ctx.players.local().inMotion() && ctx.players.local().animation() == -1) {
            if(ctx.players.local().tile().floor() == 0 && !BB_TREE.getCollision(ctx.players.local().tile())){
                return State.FAIL_JUMP_THREE;
            }
            if(ctx.players.local().tile().equals(STUCK)){
                return State.STUCK;
            }
            if(ctx.players.local().tile().equals(STUCK_TREE)){
                return State.STUCK_TREE;
            }
            if (ctx.groundItems.select().id(MARKOFGRACE).poll().valid() && ctx.players.local().tile().distanceTo(ctx.groundItems.select().id(MARKOFGRACE).nearest().poll()) <= 5) {
                return State.GET_TOKEN;
            }
            if (BB_TREE.getCollision(ctx.players.local().tile())) {
                return State.CLIMB_TREE;
            }
            if (BB_JUMPONE.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_ONE;
            }
            if (BB_JUMPTWO.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_TWO;
            }
            if (BB_JUMPTHREE.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_THREE;
            }
            if (BB_JUMPFOUR.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_FOUR;
            }
            if (BB_POLE.getCollision(ctx.players.local().tile())) {
                return State.POLE_VAULT;
            }
            if (BB_JUMPFIVE.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_FIVE;
            }
            if (BB_JUMPFINAL.getCollision(ctx.players.local().tile())) {
                return State.JUMP_GAP_FINAL;
            }
        }
        return null;
    }

    private void climbTallTree() {
        if (TREE.matrix(ctx).inViewport()) {
            TREE.matrix(ctx).interact("Climb", "Tall tree");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(TREE);
        }
    }

    public void jumpGapOne() {
        if (JUMPONE.matrix(ctx).inViewport()) {
            JUMPONE.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(JUMPONE);
        }
    }

    public void jumpGapTwo() {
        if (JUMPTWO.matrix(ctx).inViewport()) {
            JUMPTWO.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(JUMPTWO);
            Condition.sleep(1200);
        }
    }

    public void jumpGapThree() {
        if (JUMPTHREE.matrix(ctx).inViewport()) {
            JUMPTHREE.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(JUMPTHREE);
            Condition.sleep(1200);
        }
    }

    public void jumpGapFour() {
        if (JUMPFOUR.matrix(ctx).inViewport()) {
            JUMPFOUR.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(JUMPFOUR);
        }
    }

    public void poleVault() {
        if (POLEVAULT.matrix(ctx).inViewport()) {
            POLEVAULT.matrix(ctx).interact("Vault", "Pole-vault");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(POLEVAULT);
        }
    }

    public void jumpGapFive() {
        if (JUMPFIVE.matrix(ctx).inViewport()) {
            JUMPFIVE.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
            lapCounter++;
        } else {
            ctx.movement.step(JUMPFIVE);
        }
    }

    public void finalJump() {
        if (JUMPFINAL.matrix(ctx).inViewport()) {
            JUMPFINAL.matrix(ctx).interact("Jump", "Gap");
            Condition.sleep(600);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.players.local().animation() == -1 && !ctx.players.local().inMotion();
                }
            });
        } else {
            ctx.movement.step(JUMPFINAL);
        }
    }

    public void pickupMark() {
        GroundItem markOfGrace = ctx.groundItems.select().id(MARKOFGRACE).nearest().poll();
        final Tile player = ctx.players.local().tile();
        if (markOfGrace.tile().matrix(ctx).inViewport() && player.distanceTo(markOfGrace) <= 5) {
            markOfGrace.interact("Take", "Mark of grace");
        }
    }

    public void unstuck(){
        ctx.movement.step(JUMPFIVE);
        Condition.sleep(600);
    }

    public void unstuckTree(){
        ctx.movement.step(JUMPONE);
        Condition.sleep(600);
    }

    public void returnToStart(){
        ctx.movement.newTilePath(PATH_FAIL_THREE).traverse();
    }

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

    private enum State {CLIMB_TREE, JUMP_GAP_ONE, JUMP_GAP_TWO, JUMP_GAP_THREE, JUMP_GAP_FOUR, POLE_VAULT, JUMP_GAP_FIVE, JUMP_GAP_FINAL, GET_TOKEN, EAT, STUCK, STUCK_TREE, FAIL_JUMP_THREE, FAIL_JUMP_FIVE}
}