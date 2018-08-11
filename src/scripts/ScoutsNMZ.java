package scripts;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;

import java.awt.*;
import java.awt.Component;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;


@Script.Manifest(
        name = "Auto Nightmare Zone",
        description = "Will AFK the nightmare zone for dank xp drops.",
        properties = "client = 4;"
)

public class ScoutsNMZ extends PollingScript<ClientContext> implements MessageListener, PaintListener{

    public static final BoundingBox BB_LOBBY = new BoundingBox(new Tile(2601,3118),
            new Tile(2609,3112));
    public static final int DOMINIC_ONION = 1120;
    public static final int POTION = 26269;
    public static final int OVERLOAD_BARREL = 26279;
    public static final int ABSORPTION_BARREL = 26280;
    public static final int ROCK_CAKE = 7510;
    public static final int POWER_SURGE = 26264;
    public static final int[] ABSORPTION_POTION = {11734, 11735, 11736, 11737};
    public static final int[] OVERLOAD_POTION = {11730, 11731, 11732, 11733};
    public static final int MAGIC_SHORTBOW = 12788;
    public static final int RUNE_ARROWS = 892;
    public static final int DRAGON_SCIMITAR = 4587;
    public static final int DRAGON_DAGGER = 5698;
    public static final int GRANITE_MAUL = 4153;
    public static final int[] SPECIAL_WEAPONS = {DRAGON_DAGGER, GRANITE_MAUL};
    public static final int BLOW_PIPE = 12926;
    public static final int HOLY_BLESSING = 20220;
    public static final Font FONT_BODY = new Font("Default", Font.PLAIN, 12);
    public static final Font FONT_HEADING = new Font("Default", Font.PLAIN, 18);
    public static final Color COLOR_BACKGROUND = new Color(90,73,44);
    public static final Color COLOR_BORDER = new Color(127,70,15);
    public static final Color COLOR_SHADOW = new Color(0,0,0);
    public static final Color COLOR_TEXT = new Color(255,152,31);
    public static final Color COLOR_TEXT_XP = new Color(255,255,255);
    public static final Color COLOR_PROGRESS = new Color(73,150,0);
    private Tile finalTile;
    private long prayerTimer = 0;
    private int xpStartHitPoints = 0;
    private int xpStartAttack = 0;
    private int xpStartStrength = 0;
    private int xpStartDefense = 0;
    private int xpStartRange = 0;
    private int xpStartMagic = 0;
    private long powerSurgeTimer = -46000;
    private boolean started = false;
    private int mainWeapon = 0;

    @Override
    public void start() {
        try {
            FileHandler fh;
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            Date date = new Date();
            SimpleFormatter sf = new SimpleFormatter();
            fh = new FileHandler(getStorageDirectory() + "/" + df.format(date) + ".log");
            fh.setFormatter(sf);
            log.addHandler(fh);
            log.info("Script starting up...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ctx.input.speed(25);
        if(ctx.inventory.select().id(ROCK_CAKE).isEmpty()){
            log.severe("No rock cake found, stopping script!");
            ctx.controller.stop();
            return;
        }
        resetDisplayMode();
        turnOnAutoRetaliate();
        resetCameraYaw();
        resetCameraPitch();
        resetCameraZoom();
        mainWeapon = getEquipWeapon();
    }

    @Override
    public void poll() {
        final State state = getState();
//        final State state = null;
        if (state == null) {return;}

        log.info("Current State: " + state);

        switch (state) {
            case START_DREAM:
                withdrawOverload();
                withdrawAbsorption();
                startDream();
                drinkPotion();
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return inZone();
                    }
                });
                eatToHealth(51);
                flickPrayer();
                Tile startTile = ctx.players.local().tile();
                finalTile = new Tile(startTile.x() + 2, startTile.y() + 31, 3);
                xpStartHitPoints = getXpCombat(Experience.HIT_POINTS);
                xpStartAttack = getXpCombat(Experience.ATTACK);
                xpStartStrength = getXpCombat(Experience.STRENGTH);
                xpStartDefense = getXpCombat(Experience.DEFENSE);
                xpStartRange = getXpCombat(Experience.RANGE);
                xpStartMagic = getXpCombat(Experience.MAGIC);
                started = true;
                break;
            case DRINK_ABSORPTION:
                ctx.game.tab(Game.Tab.INVENTORY);
                for (int i = 0; i < 16; i++) {
                    Item absorptionPotion = ctx.inventory.select().id(ABSORPTION_POTION).first().poll();
                    if(ctx.inventory.select().id(ABSORPTION_POTION).isEmpty()){
                        break;
                    }
                    absorptionPotion.interact("Drink", "Absorption");
                    Condition.sleep(Random.nextInt(600, 700));
                }
                break;
            case DRINK_OVERLOAD:
                if(ctx.prayer.prayerPoints() > 3){
                    ctx.game.tab(Game.Tab.PRAYER);
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.widgets.widget(541).component(18).visible();
                        }
                    });
                    ctx.widgets.widget(541).component(18).click();
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.widgets.widget(541).component(18).component(0).visible();
                        }
                    });
                }
                ctx.game.tab(Game.Tab.INVENTORY);
                if(!ctx.inventory.select().id(OVERLOAD_POTION).isEmpty()) {
                    Item overloadPotion = ctx.inventory.select().id(OVERLOAD_POTION).first().poll();
                    overloadPotion.interact("Drink", "Overload");
                    Condition.sleep(Random.nextInt(600, 1200));
                }
                if(ctx.prayer.prayerPoints() > 3){
                    ctx.game.tab(Game.Tab.PRAYER);
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.combat.health() == 1;
                        }
                    });
                    ctx.widgets.widget(541).component(18).click();
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.widgets.widget(541).component(18).component(1).visible();
                        }
                    });
                }
                break;
            case EAT_CAKE:
                ctx.game.tab(Game.Tab.INVENTORY);
                Item rockCake = ctx.inventory.select().id(ROCK_CAKE).first().poll();
                rockCake.interact("Guzzle", "Dwarven");
                Condition.sleep(Random.nextInt(600, 625));
                break;
            case FLICK_PRAYER:
                flickPrayer();
                break;
            case RUN_TO_TILE:
                ctx.camera.turnTo(finalTile);
                ctx.movement.step(finalTile);
                Condition.sleep(Random.nextInt(600, 1200));
                break;
            case POWER_SURGE:
                usePowerSurge();
                powerSurgeTimer = getRuntime();
                break;
            case SPECIAL_ATTACK:
                specialAttack();
                break;
        }
    }

    private State getState() {
        long timer = getRuntime();
        if(inLobby()){
            return State.START_DREAM;
        }
        if (timer - prayerTimer > Random.nextInt(45000, 52500)) {
            return State.FLICK_PRAYER;
        }
        if (!(ctx.skills.level(Constants.SKILLS_ATTACK) > ctx.skills.realLevel(Constants.SKILLS_ATTACK)) &&
                ctx.combat.health() >= 51){
            return State.DRINK_OVERLOAD;
        }
        if (getAbsorptionPoints() < 200) {
            return State.DRINK_ABSORPTION;
        }
        if(timer - powerSurgeTimer < 45000){
            return State.SPECIAL_ATTACK;
        }
        if (!ctx.players.local().tile().equals(finalTile)) {
            return State.RUN_TO_TILE;
        }
        if(ctx.combat.health() < 10 &&
                ctx.combat.health() >= 2){
            return State.EAT_CAKE;
        }
        if(ctx.objects.select().id(POWER_SURGE).poll().valid()){
            return State.POWER_SURGE;
        }
        return null;
    }

    private String formatTime(long time) {
        long l;
        String s;
        l = Math.abs((time / 3600000) % 24);
        if (l < 10) {
            s = "0" + (int) l + ":";
        } else {
            s = l + ":";
        }
        l = Math.abs((time / 60000) % 60);
        if(l < 10){
            s += "0" + (int) l + ":";
        } else {
            s += l + ":";
        }
        l = Math.abs((time / 1000) % 60);
        if(l < 10){
            s += "0" + (int) l;
        } else {
            s += l;
        }
        return s;
    }

    private int getAbsorptionPoints(){
        if(ctx.widgets.widget(202).valid()){
            String s =  ctx.widgets.widget(202).component(3).component(5).text();
            if(s.equals("")){
                return -1;
            }
            return Integer.parseInt(s);
        }
        return -1;
    }

    private String getRewardPoints() {
        if (ctx.widgets.widget(202).valid()) {
            String s = ctx.widgets.widget(202).component(4).component(3).text();
            return s.substring(s.indexOf('>') + 1, s.length());
        }
        return null;
    }

    private int getXpCombat(Experience exp){
        switch(exp){
            case HIT_POINTS:
                return ctx.skills.experience(Constants.SKILLS_HITPOINTS);
            case ATTACK:
                return ctx.skills.experience(Constants.SKILLS_ATTACK);
            case STRENGTH:
                return ctx.skills.experience(Constants.SKILLS_STRENGTH);
            case DEFENSE:
                return ctx.skills.experience(Constants.SKILLS_DEFENSE);
            case RANGE:
                return ctx.skills.experience(Constants.SKILLS_RANGE);
            case MAGIC:
                return ctx.skills.experience(Constants.SKILLS_MAGIC);
        }
        return -1;
    }

    private int getXpGained(int startXp, Experience exp){
        switch(exp){
            case HIT_POINTS:
                return getXpCombat(Experience.HIT_POINTS) - startXp;
            case ATTACK:
                return getXpCombat(Experience.ATTACK) - startXp;
            case STRENGTH:
                return getXpCombat(Experience.STRENGTH) - startXp;
            case DEFENSE:
                return getXpCombat(Experience.DEFENSE) - startXp;
            case RANGE:
                return getXpCombat(Experience.RANGE) - startXp;
            case MAGIC:
                return getXpCombat(Experience.MAGIC) - startXp;
        }
        return -1;
    }

    private int getXpHour(int startXp, Experience exp){
        switch(exp){
            case HIT_POINTS:
                return (int)((getXpGained(startXp, Experience.HIT_POINTS) * 3600000D) / getRuntime());
            case ATTACK:
                return (int)((getXpGained(startXp, Experience.ATTACK) * 3600000D) / getRuntime());
            case STRENGTH:
                return (int)((getXpGained(startXp, Experience.STRENGTH) * 3600000D) / getRuntime());
            case DEFENSE:
                return (int)((getXpGained(startXp, Experience.DEFENSE) * 3600000D) / getRuntime());
            case RANGE:
                return (int)((getXpGained(startXp, Experience.RANGE) * 3600000D) / getRuntime());
            case MAGIC:
                return (int)((getXpGained(startXp, Experience.MAGIC) * 3600000D) / getRuntime());
        }
        return -1;
    }

    private int getXpPercentage(Experience exp){
        double numerator = 0.0;
        double denominator = 0.0;
        switch(exp){
            case HIT_POINTS:
                numerator = ctx.skills.experience(Constants.SKILLS_HITPOINTS) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_HITPOINTS));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_HITPOINTS) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_HITPOINTS));
                return (int)((numerator / denominator) * 100);
            case ATTACK:
                numerator = ctx.skills.experience(Constants.SKILLS_ATTACK) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_ATTACK));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_ATTACK) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_ATTACK));
                return (int)((numerator / denominator) * 100);
            case STRENGTH:
                numerator = ctx.skills.experience(Constants.SKILLS_STRENGTH) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_STRENGTH));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_STRENGTH) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_STRENGTH));
                return (int)((numerator / denominator) * 100);
            case DEFENSE:
                numerator = ctx.skills.experience(Constants.SKILLS_DEFENSE) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_DEFENSE));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_DEFENSE) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_DEFENSE));
                return (int)((numerator / denominator) * 100);
            case RANGE:
                numerator = ctx.skills.experience(Constants.SKILLS_RANGE) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_RANGE));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_RANGE) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_RANGE));
                return (int)((numerator / denominator) * 100);
            case MAGIC:
                numerator = ctx.skills.experience(Constants.SKILLS_MAGIC) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_MAGIC));
                denominator = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_MAGIC) + 1) - ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_MAGIC));
                return (int)((numerator / denominator) * 100);
        }
        return -1;
    }

    private int getTimeToLevel(int xpStart, Experience exp){
        double xpToLevel;
        double xpHour;
        switch(exp){
            case HIT_POINTS:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_HITPOINTS) + 1) - ctx.skills.experience(Constants.SKILLS_HITPOINTS);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
            case ATTACK:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_ATTACK) + 1) - ctx.skills.experience(Constants.SKILLS_ATTACK);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
            case STRENGTH:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_STRENGTH) + 1) - ctx.skills.experience(Constants.SKILLS_STRENGTH);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
            case DEFENSE:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_DEFENSE) + 1) - ctx.skills.experience(Constants.SKILLS_DEFENSE);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
            case RANGE:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_RANGE) + 1) - ctx.skills.experience(Constants.SKILLS_RANGE);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
            case MAGIC:
                xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_MAGIC) + 1) - ctx.skills.experience(Constants.SKILLS_MAGIC);
                xpHour = getXpHour(xpStart, exp);
                if(xpHour == 0){
                    return 0;
                }
                return (int)((xpToLevel / xpHour) * 3600000D);
        }
        return -1;
    }

    private String getCombatStyle(){
        Varpbits vb = ctx.varpbits;
        switch(vb.varpbit(43)){
            case 0:
                return ctx.widgets.widget(593).component(6).text();
            case 1:
                return ctx.widgets.widget(593).component(10).text();
            case 2:
                return ctx.widgets.widget(593).component(14).text();
            case 3:
                return ctx.widgets.widget(593).component(18).text();
        }
        return null;
    }

    private String getCombatLevel(){
        String s = ctx.widgets.widget(593).component(2).text();
        return s.substring(s.indexOf(':') + 2, s.length());
    }

    private boolean inLobby(){
        return BB_LOBBY.getCollision(ctx.players.local().tile());
    }

    private boolean inZone() {return ctx.players.local().tile().floor() == 3;}

    private boolean isOverloaded(){
        Varpbits vb = ctx.varpbits;
        return vb.varpbit(1067) != -1162870784;
    }

    private void startDream() {
        log.info("Starting the dream...");
        Npc dominicOnion = ctx.npcs.select().id(DOMINIC_ONION).poll();
        dominicOnion.interact("Dream", "Dominic Onion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(219).component(0).component(4).visible();
            }
        });
        if(!ctx.widgets.widget(219).component(0).component(4).visible()){
            log.severe("Dialog 1/4 never appeared!");
            return;
        }
        ctx.widgets.widget(219).component(0).component(4).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(231).component(3).visible();
            }
        });
        if(!ctx.widgets.widget(231).component(3).visible()){
            log.severe("Dialog 2/4 never appeared!");
            return;
        }
        ctx.widgets.widget(231).component(3).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(219).component(0).component(1).visible();
            }
        });
        if(!ctx.widgets.widget(219).component(0).component(1).visible()){
            log.severe("Dialog 3/4 never appeared!");
            return;
        }
        ctx.widgets.widget(219).component(0).component(1).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(231).component(3).visible();
            }
        });
        if(!ctx.widgets.widget(231).component(3).visible()){
            log.severe("Dialog 4/4 never appeared!");
            return;
        }
        ctx.widgets.widget(231).component(3).click();
        log.info("Finished starting the dream!");
    }

    private void drinkPotion(){
        log.info("Drinking the start-potion...");
        GameObject potion = ctx.objects.select().id(POTION).poll();
        if(!potion.valid()){
            log.severe("Can't find the start-potion!");
            return;
        }
        potion.interact("Drink", "Potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(129).component(6).component(9).visible();
            }
        });
        if(!ctx.widgets.widget(129).component(6).component(9).visible()){
            log.severe("Dialog screen never appeared!");
            return;
        }
        ctx.widgets.widget(129).component(6).component(9).click();
        log.info("Finished drinking the start-potion!");
    }

    private void withdrawOverload(){
        log.info("Withdrawing overload potions...");
        GameObject overloadBarrel = ctx.objects.select().id(OVERLOAD_BARREL).poll();
        if(!overloadBarrel.valid()){
            log.severe("Can't find overload barrel");
            return;
        }
        overloadBarrel.interact("Take", "Overload potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(162).component(36).visible();
            }
        });
        if(!ctx.widgets.widget(162).component(36).visible()){
            log.severe("Input screen never appeared!");
        }
        ctx.input.sendln("24");
        log.info("Finished withdrawing overload potions!");
    }

    private void withdrawAbsorption(){
        log.info("Withdrawing absorption potions...");
        GameObject absorptionBarrel = ctx.objects.select().id(ABSORPTION_BARREL).poll();
        if(!absorptionBarrel.valid()){
            log.severe("Can't find absorption barrel!");
            return;
        }
        absorptionBarrel.interact("Take", "Absorption potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(162).component(36).visible();
            }
        });
        if(!ctx.widgets.widget(162).component(36).visible()){
            log.severe("Input screen never appeared!");
            return;
        }
        ctx.input.sendln(String.valueOf(Random.nextInt(84, 999)));
        log.info("Finished withdrawing absorption potions!");
    }

    private void eatToHealth(int health){
        log.info("Eating to " + health + " health...");
        ctx.game.tab(Game.Tab.INVENTORY);
        Item rockCake = ctx.inventory.select().id(ROCK_CAKE).poll();
        if (!rockCake.valid()){
            log.severe("No rockcake found in inventory!");
            return;
        }
        int tempHealth = ctx.skills.realLevel(Constants.SKILLS_HITPOINTS);
        int damage = tempHealth / 10;
        tempHealth = ctx.combat.health();
        int limitHealth =  (tempHealth - health) / damage;
        for(int i = 1; i < limitHealth; i ++){
            log.info("Taking big-bite (" + i + "/" + (limitHealth - 1) + ")");
            rockCake.interact("Guzzle", "Dwarven rock cake");
            Condition.sleep(Random.nextInt(1200, 1300));
        }
        tempHealth = ctx.combat.health();
        limitHealth = tempHealth - health;
        for(int j = 0; j < limitHealth; j++){
            log.info("Taking small-bite (" + (j + 1) + "/" + limitHealth + ")");
            rockCake.interact("Eat", "Dwarven rock cake");
            Condition.sleep(Random.nextInt(600, 700));
        }
        log.info("Finished eating to " + health + " health!");
    }

    private void flickPrayer(){
        log.info("Flicking prayer...");
        if(ctx.prayer.prayerPoints() == 0){
            log.severe("Out of prayer points!");
            return;
        }
        ctx.game.tab(Game.Tab.PRAYER);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(541).component(11).visible();
            }
        });
        ctx.widgets.widget(541).component(11).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(541).component(11).component(0).visible();
            }
        });
        log.info("Rapid Renewal: On");
        ctx.widgets.widget(541).component(11).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(541).component(11).component(1).visible();
            }
        });
        log.info("Rapid Renewal: Off");
        prayerTimer = getRuntime();
        log.info("Finished flicking prayer!");
    }

    private void usePowerSurge(){
        final GameObject powerSurge = ctx.objects.select().id(POWER_SURGE).nearest().poll();
        final Tile tile = powerSurge.tile();
        ctx.camera.turnTo(tile);
        ctx.movement.step(tile);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.objects.select().id(POWER_SURGE).poll().tile().matrix(ctx).inViewport();
            }
        });
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !ctx.players.local().inMotion() && tile.distanceTo(ctx.players.local().tile()) < 5;
            }
        });
        powerSurge.interact("Activate", "Power surge");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(162).component(50).component(0).text().contains("You feel a surge");
            }
        });
        equipSpecialWeapon();
    }

    private void specialAttack(){
        if(ctx.combat.specialPercentage() == 0){
            return;
        }
        ctx.game.tab(Game.Tab.ATTACK);
        ctx.widgets.widget(593).component(34).click();
        Condition.sleep(600);
    }

    private void resetCameraYaw(){
        log.info("Resetting camera yaw...");
        if(ctx.camera.yaw() <= 10 || ctx.camera.y() >= 350){
            log.warning("Camera yaw is offset by less than 20 degrees, no need to reset!");
            return;
        }
        ctx.widgets.widget(548).component(7).click();
        log.info("Camera yaw reset!");
    }

    private void resetCameraPitch(){
        log.info("Resetting camera pitch...");
        if(ctx.camera.pitch() >= 95){
            log.warning("Camera pitch is already greater than 95 degrees!");
            return;
        }
        ctx.camera.pitch(true);
        log.info("Camera pitch reset!");
    }

    private void resetCameraZoom(){
        log.info("Resetting camera Zoom...");
        if (ctx.widgets.widget(261).component(14).screenPoint().x == 601){
            log.warning("Camera zoom already set!");
            return;
        }
        ctx.game.tab(Game.Tab.OPTIONS);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(261).component(14).visible();
            }
        });
        ctx.input.move(new Point(601,265));
        ctx.input.click(1);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(261).component(14).screenPoint().x == 601;
            }
        });
        log.info("Camera zoom reset!");
    }

    private void turnOnAutoRetaliate(){
        log.info("Turning on auto-retaliate...");
        if(ctx.varpbits.varpbit(172) == 0){
            log.warning("Auto-retaliate is already enabled!");
            return;
        }
        ctx.game.tab(Game.Tab.ATTACK);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(593).component(28).visible();
            }
        });
        ctx.widgets.widget(593).component(28).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.varpbits.varpbit(172) == 0;
            }
        });
        log.info("Auto-retaliate is now enabled!");
    }

    private void resetDisplayMode(){
        log.info("Setting display mode to fixed...");
        if(ctx.widgets.widget(261).component(22).component(9).textureId() == 1572){
            log.warning("Display mode is already in fixed!");
            return;
        }
        ctx.game.tab(Game.Tab.OPTIONS);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(261).component(22).component(9).visible();
            }
        });
        ctx.widgets.widget(261).component(22).component(9).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(261).component(22).component(9).textureId() == 1572;
            }
        });
        log.info("Display mode set to fixed!");
    }

    private boolean detectSpecialWeapon(){
        return !ctx.inventory.select().id(SPECIAL_WEAPONS).isEmpty();
    }

    private int getEquipWeapon(){
        return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id();
    }

    private void equipSpecialWeapon(){
        if(!detectSpecialWeapon()){
            return;
        }
        if(ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == -1){
            return;
        }
        ctx.game.tab(Game.Tab.INVENTORY);
        ctx.inventory.select().id(SPECIAL_WEAPONS).poll().click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                for(int i = 0; i < SPECIAL_WEAPONS.length; i++){
                    return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == SPECIAL_WEAPONS[i];
                }
                return true;
            }
        });
    }

    private void equipMainHand(final int weapon){
        if(ctx.inventory.select().id(mainWeapon).isEmpty()){
            return;
        }
        ctx.game.tab(Game.Tab.INVENTORY);
        ctx.inventory.select().id(weapon).poll().click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == weapon;
            }
        });
    }

    @Override
    public void repaint(Graphics graphics) {
        if(!started){
            return;
        }
        final Graphics2D g = (Graphics2D) graphics;
        if(ctx.players.local().tile().compareTo(finalTile) == 0){
            g.setColor(new Color(0,255,0,64));
        } else {
            g.setColor(new Color(255,0,0,64));
        }
        g.fillPolygon(finalTile.matrix(ctx).bounds());
        Paint.drawCrosshair(g, ctx, 3);
        Paint.drawRectangleBordered(g,3,3,250, 100,2,COLOR_BACKGROUND,COLOR_BORDER);
        Paint.drawRectangleBordered(g,3,138,250, 200,2 ,COLOR_BACKGROUND, COLOR_BORDER);
        Paint.drawTextShadow(g,5,20, COLOR_TEXT, COLOR_SHADOW, FONT_HEADING, "Auto Nightmare Zone - Brotein");
        g.setColor(new Color(0,0,0));
        g.fillRect(5, 153, 245, 15);
        g.fillRect(5, 183, 245, 15);
        g.fillRect(5, 213, 245, 15);
        g.fillRect(5, 243, 245, 15);
        g.fillRect(5, 273, 245, 15);
        g.fillRect(5, 303, 245, 15);
        Paint.drawTextShadow(g,5,40,COLOR_TEXT, COLOR_SHADOW, FONT_BODY, String.format("Time Running: %s", formatTime(getTotalRuntime())));
        Paint.drawTextShadow(g,5,60,COLOR_TEXT, COLOR_SHADOW, FONT_BODY, String.format("Points Gained: %s", getRewardPoints()));
        Paint.drawTextShadow(g,5,80,COLOR_TEXT, COLOR_SHADOW, FONT_BODY, String.format("Absorption: %d", getAbsorptionPoints()));
        Paint.drawTextShadow(g,5,100,COLOR_TEXT, COLOR_SHADOW, FONT_BODY, String.format("Combat Style: %s (Level %s)", getCombatStyle(), getCombatLevel()));
        Paint.drawTextShadow(g,5,150,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-14s% ,d%n XP (% ,d%n/HR)",
                "Hit Points:",
                getXpGained(xpStartHitPoints, Experience.HIT_POINTS),
                getXpHour(xpStartHitPoints, Experience.HIT_POINTS)));
        Paint.drawTextShadow(g,5,180,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-16s% ,d%n XP (% ,d%n/HR)",
                        "Attack:",
                        getXpGained(xpStartAttack, Experience.ATTACK),
                        getXpHour(xpStartAttack, Experience.ATTACK)));
        Paint.drawTextShadow(g,5,210,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-14s% ,d%n XP (% ,d%n/HR)",
                        "Strength:",
                        getXpGained(xpStartStrength, Experience.STRENGTH),
                        getXpHour(xpStartStrength, Experience.STRENGTH)));
        Paint.drawTextShadow(g,5,240,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-14s% ,d%n XP (% ,d%n/HR)",
                        "Defense:",
                        getXpGained(xpStartDefense, Experience.DEFENSE),
                        getXpHour(xpStartDefense, Experience.DEFENSE)));
        Paint.drawTextShadow(g,5,270,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-15s% ,d%n XP (% ,d%n/HR)",
                        "Range:",
                        getXpGained(xpStartRange, Experience.RANGE),
                        getXpHour(xpStartRange, Experience.RANGE)));
        Paint.drawTextShadow(g,5,300,COLOR_TEXT, COLOR_SHADOW, FONT_BODY,
                String.format("%-16s% ,d%n XP (% ,d%n/HR)",
                        "Magic:",
                        getXpGained(xpStartMagic, Experience.MAGIC),
                        getXpHour(xpStartMagic, Experience.MAGIC)));
        g.setColor(COLOR_PROGRESS);
        g.fillRect(6, 154, (int)(250 *  (getXpPercentage(Experience.HIT_POINTS) / 100.0)) - 2, 13);
        g.fillRect(6, 184, (int)(250 *  (getXpPercentage(Experience.ATTACK) / 100.0)) - 2, 13);
        g.fillRect(6, 214, (int)(250 *  (getXpPercentage(Experience.STRENGTH) / 100.0)) - 2, 13);
        g.fillRect(6, 244, (int)(250 *  (getXpPercentage(Experience.DEFENSE) / 100.0)) - 2, 13);
        g.fillRect(6, 274, (int)(250 *  (getXpPercentage(Experience.RANGE) / 100.0)) - 2, 13);
        g.fillRect(6, 304, (int)(250 *  (getXpPercentage(Experience.MAGIC) / 100.0)) - 2, 13);
        Paint.drawTextShadow(g,65,165,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.HIT_POINTS),
                        "%",
                        formatTime(getTimeToLevel(xpStartHitPoints, Experience.HIT_POINTS)),
                        ctx.skills.realLevel(Constants.SKILLS_HITPOINTS) + 1));
        Paint.drawTextShadow(g,65,195,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.ATTACK),
                        "%",
                        formatTime(getTimeToLevel(xpStartAttack, Experience.ATTACK)),
                        ctx.skills.realLevel(Constants.SKILLS_ATTACK) + 1));
        Paint.drawTextShadow(g,65,225,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.STRENGTH),
                        "%",
                        formatTime(getTimeToLevel(xpStartStrength, Experience.STRENGTH)),
                        ctx.skills.realLevel(Constants.SKILLS_STRENGTH) + 1));
        Paint.drawTextShadow(g,65,255,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.DEFENSE),
                        "%",
                        formatTime(getTimeToLevel(xpStartDefense, Experience.DEFENSE)),
                        ctx.skills.realLevel(Constants.SKILLS_DEFENSE) + 1));
        Paint.drawTextShadow(g,65,285,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.RANGE),
                        "%",
                        formatTime(getTimeToLevel(xpStartRange, Experience.RANGE)),
                        ctx.skills.realLevel(Constants.SKILLS_RANGE) + 1));
        Paint.drawTextShadow(g,65,315,COLOR_TEXT_XP, COLOR_SHADOW, FONT_BODY,
                String.format("%d%s (%s to %d)", getXpPercentage(Experience.MAGIC),
                        "%",
                        formatTime(getTimeToLevel(xpStartMagic, Experience.MAGIC)),
                        ctx.skills.realLevel(Constants.SKILLS_MAGIC) + 1));
    }

    @Override
    public void messaged(MessageEvent e){
        final String msg = e.text().toLowerCase();
        if(msg.equals("you wake up feeling refreshed.")){
            log.severe("You have died, script stopping!!!");
            ctx.controller.stop();
        }
        else if(msg.equals("your feel a surge of special attack power!")){
            powerSurgeTimer = getRuntime();
        }
        else if(msg.equals("your surge of special attack power has ended.")){
            equipMainHand(mainWeapon);
            ctx.camera.turnTo(finalTile);
            ctx.movement.step(finalTile);
            Condition.sleep(Random.nextInt(600, 1200));
        }
    }

    private enum State {
        START_DREAM, DRINK_ABSORPTION, DRINK_OVERLOAD, EAT_CAKE, FLICK_PRAYER, RUN_TO_TILE, POWER_SURGE, SPECIAL_ATTACK
    }

    private enum Experience {
        HIT_POINTS, ATTACK, STRENGTH, DEFENSE, RANGE, MAGIC
    }
}