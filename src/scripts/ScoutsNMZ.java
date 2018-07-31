package scripts;

import org.powerbot.Con;
import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;

import java.awt.*;
import java.util.concurrent.Callable;


@Script.Manifest(
        name = "Nightmare Zone",
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

    @Override
    public void start() {
        System.out.println("Script starting up...");
        ctx.input.speed(25);
    }

    @Override
    public void poll() {

        System.out.println(ctx.camera.pitch());
//        final State state = getState();
        final State state = null;
//        System.out.println("Current State: " + state);
        if (state == null) {
            return;
        }

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
//        if(timer - powerSurgeTimer < 45000){
//            return State.SPECIAL_ATTACK;
//        }
        if (!ctx.players.local().tile().equals(finalTile)) {
            return State.RUN_TO_TILE;
        }
        if(ctx.combat.health() < 10 &&
                ctx.combat.health() >= 2){
            return State.EAT_CAKE;
        }
//        if(ctx.objects.select().id(POWER_SURGE).poll().valid()){
//            return State.POWER_SURGE;
//        }
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
            String s =  ctx.widgets.widget(202).component(1).component(9).text();
            if(s.equals("")){
                return -1;
            }
            return Integer.parseInt(s);
        }
        return -1;
    }

    private String getRewardPoints() {
        if (ctx.widgets.widget(202).valid()) {
            String s = ctx.widgets.widget(202).component(1).component(3).text();
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
        Npc dominicOnion = ctx.npcs.select().id(DOMINIC_ONION).poll();
        dominicOnion.interact("Dream", "Dominic Onion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(219).component(0).component(4).visible();
            }
        });
        ctx.widgets.widget(219).component(0).component(4).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(231).component(3).visible();
            }
        });
        ctx.widgets.widget(231).component(3).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(219).component(0).component(1).visible();
            }
        });
        ctx.widgets.widget(219).component(0).component(1).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(231).component(3).visible();
            }
        });
        ctx.widgets.widget(231).component(3).click();
    }

    private void drinkPotion(){
        GameObject potion = ctx.objects.select().id(POTION).poll();
        potion.interact("Drink", "Potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(129).component(6).component(9).visible();
            }
        });
        ctx.widgets.widget(129).component(6).component(9).click();
    }

    private void withdrawOverload(){
        GameObject overloadBarrel = ctx.objects.select().id(OVERLOAD_BARREL).poll();
        overloadBarrel.interact("Take", "Overload potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(162).component(36).visible();
            }
        });
        ctx.input.sendln("24");
    }

    private void withdrawAbsorption(){
        GameObject absorptionBarrel = ctx.objects.select().id(ABSORPTION_BARREL).poll();
        absorptionBarrel.interact("Take", "Absorption potion");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(162).component(36).visible();
            }
        });
        ctx.input.sendln(String.valueOf(Random.nextInt(84, 999)));
    }

    private void eatToHealth(int health){
        ctx.game.tab(Game.Tab.INVENTORY);
        int tempHealth = ctx.skills.realLevel(Constants.SKILLS_HITPOINTS);
        int damage = tempHealth / 10;
        tempHealth = ctx.combat.health();
        Item rockCake = ctx.inventory.select().id(ROCK_CAKE).poll();
        if (rockCake.valid()){
            for(int i = 1; i < (tempHealth - health) / damage; i ++){
                rockCake.interact("Guzzle", "Dwarven rock cake");
                Condition.sleep(Random.nextInt(1200, 1300));
            }
            tempHealth = ctx.combat.health();
            for(int j = 0; j < (tempHealth - health); j++){
                rockCake.interact("Eat", "Dwarven rock cake");
                Condition.sleep(Random.nextInt(600, 700));
            }
        }
    }

    private void flickPrayer(){
        if(ctx.prayer.prayerPoints() == 0){
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
        ctx.widgets.widget(541).component(11).click();
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(541).component(11).component(1).visible();
            }
        });
        prayerTimer = getRuntime();
    }

    private void usePowerSurge(){
        final GameObject powerSurge = ctx.objects.select().id(POWER_SURGE).nearest().poll();
        Tile tile = powerSurge.tile();
        ctx.camera.turnTo(tile);
        ctx.movement.step(tile);
        Condition.sleep(Random.nextInt(600, 700));
        equipSpecialWeapons();
        powerSurge.interact("Activate", "Power surge");
    }

    private void equipSpecialWeapons() {
        int weaponID = ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id();
        switch (weaponID){
            case DRAGON_SCIMITAR:
                ctx.game.tab(Game.Tab.INVENTORY);
                ctx.inventory.select().id(DRAGON_DAGGER).first().poll().interact("Wield", "Dragon dagger(p++)");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == DRAGON_DAGGER;
                    }
                });
                break;
            case BLOW_PIPE:
                ctx.game.tab(Game.Tab.INVENTORY);
                ctx.inventory.select().id(MAGIC_SHORTBOW).first().poll().interact("Wield", "Magic Shortbow (i)");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == MAGIC_SHORTBOW;
                    }
                });
                ctx.inventory.select().id(RUNE_ARROWS).first().poll().interact("Wield", "Rune arrow");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.QUIVER).id() == RUNE_ARROWS;
                    }
                });
                break;
        }
    }

    private void reEquipWeapons() {
        int weaponID = ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id();
        switch (weaponID){
            case DRAGON_DAGGER:
                ctx.game.tab(Game.Tab.INVENTORY);
                ctx.inventory.select().id(DRAGON_SCIMITAR).first().poll().interact("Wield", "Dragon scimitar");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == DRAGON_SCIMITAR;
                    }
                });
                break;
            case MAGIC_SHORTBOW:
                ctx.game.tab(Game.Tab.INVENTORY);
                ctx.inventory.select().id(BLOW_PIPE).first().poll().interact("Wield", "Toxic blowpipe");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() == BLOW_PIPE;
                    }
                });
                ctx.inventory.select().id(HOLY_BLESSING).first().poll().interact("Equip", "Holy blessing");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.equipment.itemAt(Equipment.Slot.QUIVER).id() == HOLY_BLESSING;
                    }
                });
                break;
        }
    }

    private void specialAttack(){
        ctx.game.tab(Game.Tab.ATTACK);
        if (ctx.combat.specialPercentage() >= 50){
            ctx.widgets.widget(593).component(34).click();
            Condition.sleep(600);
        }
    }

    @Override
    public void repaint(Graphics graphics) {
        if(!started){
            return;
        }
        final Graphics2D g = (Graphics2D) graphics;
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
            ctx.controller.stop();
        }
        else if(msg.equals("your feel a surge of special attack power!")){
            powerSurgeTimer = getRuntime();
        }
        else if(msg.equals("your surge of special attack power has ended.")){
            reEquipWeapons();
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