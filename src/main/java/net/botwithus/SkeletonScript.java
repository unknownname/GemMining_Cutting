package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.BackpackInventory;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.api.game.hud.traversal.Lodestone;
import net.botwithus.api.game.world.Traverse;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.InventoryUpdateEvent;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.movement.TraverseEvent;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.Headbar;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.util.Regex;
import net.botwithus.rs3.game.scene.entities.characters.PathingEntity.*;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SkeletonScript extends LoopingScript {

    private BotState botState = BotState.IDLE;
    private boolean warTeleport = false;
    private boolean mysticalseed = false;
    public long scriptStartTime = System.currentTimeMillis();

    public int GemMined  = 0;
    public int GemMinedPerHour = 0;

    private Random random = new Random();
    private Pattern gembagpattern = Regex.getPatternForContainingOneOf("Gem bag", "Gem bag (upgraded)");
    private Area Alkharidmine = new Area.Rectangular(new Coordinate(3297, 3287, 0), new Coordinate(3302, 3282, 0));
    private Area AlkharidCity = new Area.Rectangular(new Coordinate(3273,3177,0), new Coordinate(3277,3185,0));
    private Area AlKharidBank = new Area.Rectangular(new Coordinate(3274,3168,0), new Coordinate(3267,3171,0));
    private Area wararea = new Area.Rectangular(new Coordinate(3295,10132,0), new Coordinate(3301,10128,0));
    private Pattern Gems = Regex.getPatternForContainingOneOf("Uncut ruby", "Uncut sapphire", "Uncut emerald");
    enum BotState {
        //define your own states here
        IDLE,
        MINING,
        BANKING,
        PROCESSING,
        RUNNING,
        WAR,
        TELEPORT

        //...
    }

    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);

    }

    @Override
    public boolean initialize()
    {

        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        setActive(false);
        GemMined();


        return super.initialize();

    }


    @Override
    public void onLoop() {
        //Loops every 100ms by default, to change:
        //this.loopDelay = 500;
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState == BotState.IDLE) {
            //wait some time so we dont immediately start on login.
            Execution.delay(random.nextLong(3000,7000));
            return;
        }
        switch (botState) {
            case IDLE -> {
                //do nothing
                println("We're idle!");
                Execution.delay(random.nextLong(1000,3000));
            }
            case MINING -> {
                //do some code that handles your skilling
                Execution.delay(handleSkilling(player));
            }
            case BANKING -> {
                Execution.delay(handleBanking(player));
                //handle your banking logic, etc
            }
            case PROCESSING -> {

                //handle GEM Processing
            }
            case RUNNING -> {
                handlewalking(player);
                // Walking and Teleport Functions
            }
            case WAR -> {
                // Teleport to War
            }
            case TELEPORT -> {
                Execution.delay(teleporting(player));
            }

        }
    }

    public void GemMined()
    {
        subscribe(InventoryUpdateEvent.class, inventoryUpdateEvent -> {
            Item item = inventoryUpdateEvent.getNewItem();
            if (item != null) {
                if (item.getInventoryType().getId() != 93) {
                    return;
                }
                String runeName = item.getName();

                if (runeName != null) {
                    if (runeName.equalsIgnoreCase("Uncut ruby")) {
                        GemMined = GemMined + item.getStackSize();

                    }
                    if (runeName.equalsIgnoreCase("Uncut sapphire")) {
                        GemMined = GemMined + item.getStackSize();

                    }
                    if (runeName.equalsIgnoreCase("Uncut emerald")) {
                        GemMined = GemMined + item.getStackSize();

                    }
                }

            }
            long currenttime = (System.currentTimeMillis() - scriptStartTime) /1000;
            GemMinedPerHour = (int)(Math.round(3600.0 / currenttime * GemMined));
            //println(" Gem's Mined Per Hour: " + GemMinedPerHour);

        });

    }

    private long handleBanking(LocalPlayer player)
    {

        if(player.isMoving())
        {
            return random.nextLong(2100,3350);
        }
        if (Bank.isOpen())
        {
            ResultSet<Item> gembag = InventoryItemQuery.newQuery(93).name(gembagpattern).ids(31455).results();
            if(gembag.stream().anyMatch(item -> item.getId() != -1)) {

                int gemsinbagshappier =     VarManager.getVarbitValue(22581);
                int gemsinbagemerald =      VarManager.getVarbitValue(22582); //VarManager.getVarValue(VarDomainType.PLAYER,22582);
                int gemsinbagruby =         VarManager.getVarbitValue(22583);//VarManager.getVarValue(VarDomainType.PLAYER,22583);
                int gemsinbagdiamond =      VarManager.getVarbitValue(22584);//VarManager.getVarValue(VarDomainType.PLAYER,22584);
                int gemsinbagdragonstone =  VarManager.getVarbitValue(22585);//VarManager.getVarValue(VarDomainType.PLAYER,22585);

                Component box = ComponentQuery.newQuery(517).componentIndex(15).option("Empty").results().first();
                if (box !=null &&(gemsinbagshappier >0 || gemsinbagemerald > 0 || gemsinbagruby > 0))
                {
                    boolean success = box.interact("Empty");
                    println("Deposited box contents: " + success);
                    Bank.depositAllExcept(31455,54004);
                    if (success)
                        return random.nextLong(750, 1000);
                }
                else
                {
                    println("Already Deposited, Skipping");
                }
            }
            Component depositgem = ComponentQuery.newQuery(517).componentIndex(15).option("Deposit-All").results().first();
            if(depositgem != null)
            {
                boolean success = depositgem.interact("Deposit-All");
                println(" Deposit Gems: " + success);
                if (success)
                {
                    return random.nextLong(1500,3000);
                }

            }

            botState = BotState.MINING;
        } else {
            // Bank Areas
            if(player.getCoordinate().getRegionId() == 13214)
            {
                SceneObject bankChest = SceneObjectQuery.newQuery().name("Bank chest").results().nearest();
                if (bankChest != null)
                {

                    println("Interact with War Bank: " + bankChest.interact("Use"));
                    return random.nextLong(1000,3000);
                    //Bank.depositAllExcept(54004);

                }
            }else if(player.getCoordinate().getRegionId() == 13105)
            {
                ResultSet<SceneObject> banks = SceneObjectQuery.newQuery().name("Bank booth").option("Bank").inside(AlKharidBank).results();
                if (banks.isEmpty())
                {
                    println("Bank query was empty.");
                }
                else
                {
                    SceneObject bank = banks.random();
                    if (bank != null) {
                        println("Yay, we found our bank.");
                        println("Interacted bank: " + bank.interact("Bank"));
                        //Bank.depositAllExcept(54004,31455);
                    }
                }
            }else
            {
                // Hall of Memory Area
                println(" None of Bank are near by");
            }


        }




        return random.nextLong(750, 1500);
    }


    private long handleSkilling(LocalPlayer player) {
        //for example, if skilling progress interface is open, return a randomized value to keep waiting.
            //if our inventory is full, lets bank.
        if(player.isMoving())
        {
            return random.nextLong(1500,2250);
        }

        if (Backpack.isFull())
        {
            Item gembag = InventoryItemQuery.newQuery(93).name(gembagpattern).results().first();
            if(gembag ==null || gembag.getId() ==-1)
            {
                println(" We did not find out GemBag, So we should Bank");
                botState = botState.TELEPORT;
            }else
            {
                println(" We found gem bag: " + gembag.getName());
                if(gembag.getName() !=null)
                {
                    Item gems = InventoryItemQuery.newQuery(93).name(Gems).results().first();
                    if(gems ==null && gems.getId() ==-1 && gems.getName() == null)
                    {
                        println(" No Uncut Gems found in inventory");
                    }else
                    {
                        // Checking if items are store in Gembag
                        int gemsinbagshappier =     VarManager.getVarbitValue(22581);
                        int gemsinbagemerald =      VarManager.getVarbitValue(22582); //VarManager.getVarValue(VarDomainType.PLAYER,22582);
                        int gemsinbagruby =         VarManager.getVarbitValue(22583);//VarManager.getVarValue(VarDomainType.PLAYER,22583);
                        int gemsinbagdiamond =      VarManager.getVarbitValue(22584);//VarManager.getVarValue(VarDomainType.PLAYER,22584);
                        int gemsinbagdragonstone =  VarManager.getVarbitValue(22585);//VarManager.getVarValue(VarDomainType.PLAYER,22585);
                        if(gemsinbagshappier == 0 || gemsinbagemerald == 0 || gemsinbagruby ==0)
                        {
                            println("We didnt find Gems in the gem box, but we have one, so fill it.");
                        }else
                        {
                            if(!Backpack.isEmpty() && (gemsinbagshappier == 60 || gemsinbagemerald == 60 || gemsinbagruby == 60))
                            {
                                botState = BotState.TELEPORT;
                                return random.nextLong(1250,1750);
                            }
                        }
                        Component gembagcomp = ComponentQuery.newQuery(1473).componentIndex(5).itemName(gembag.getName()).option("Fill").results().first();
                        if(gembagcomp !=null)
                        {
                            println(" Filled GemBag: " + gembagcomp.interact("Fill"));
                        }

                    }
                }
            }
            return random.nextLong(1500,3000);
        }


        if(player.getAnimationId() ==-1 && player.getCoordinate().getRegionId() != 13107)
        {
            botState = BotState.RUNNING;
        }

        if(!player.isMoving()) {
            GemMining();
        }
        return random.nextLong(1500,3000);
    }

    private void GemMining()
    {
        if (Skills.MINING.getLevel() <20)
        {
            SceneObject CommonGem = SceneObjectQuery.newQuery().name("Common gem rock").option("Mine").results().random();
            if (CommonGem != null) {

                println("Interacted CommonGem: " + CommonGem.interact("Mine"));

            }
        }
        if (Skills.MINING.getLevel() >=20)
        {
            SceneObject UncommonGem = SceneObjectQuery.newQuery().name("Uncommon gem rock").results().random();  //.option("Mine")
            List<Headbar> headbars = Client.getLocalPlayer().getHeadbars();
            if(!headbars.isEmpty()) {
                Headbar firsheadbar = headbars.get(0);

                if (UncommonGem != null && Client.getLocalPlayer().getAnimationId() ==-1) {

                    Execution.delay(random.nextLong(750,1250));
                    println("Interacted UnCommonGem: " + UncommonGem.interact("Mine"));
                    Execution.delay(random.nextLong(750,1250));
                    Execution.delayUntil( random.nextLong(35000,42000), () -> (firsheadbar.getId() == 5 && firsheadbar.getWidth() <= RandomGenerator.nextInt(0,100) || Client.getLocalPlayer().getAnimationId() ==-1));
                }else if(firsheadbar.getId() == 5 && firsheadbar.getWidth() <= RandomGenerator.nextInt(0,100))
                {
                    Execution.delay(random.nextLong(750,1250));
                    println("Interacted UnCommonGem: " + UncommonGem.interact("Mine"));
                    Execution.delay(random.nextLong(750,1250));
                    Execution.delayUntil( random.nextLong(35000,42000), () -> (firsheadbar.getId() == 5 && firsheadbar.getWidth() <= RandomGenerator.nextInt(0,100) || Client.getLocalPlayer().getAnimationId() ==-1));
                }

            }else
            {
                println(" Headbar Check failed");
            }


        }
        //return random.nextLong(1500,3000);
    }

    private long handlewalking(LocalPlayer player)
    {
        // Checking Seed and Teleporting from WAR or any other place
        if(player.getAnimationId() == -1 && player.getCoordinate().getRegionId() == 13107)
        {
            println("In Mining Area");
            botState = BotState.MINING;
        }
        else if(player.getAnimationId() ==-1 && player.getCoordinate().getRegionId() != 13107 && mysticalseed == true)
        {
            println("Currently not standing in Al-Kharid Mining Area");

            boolean hasSandSeed = Backpack.contains("Mystical sand seed");
            if(hasSandSeed)
            {
                println("Found Sand Seed");
                //ActionBar.useItem("Mystical sand seed", "Plant");
                Backpack.interact("Mystical sand seed", "Plant");
                println("Planted seed");
                Execution.delayUntil(50000, () -> Client.getLocalPlayer().getAnimationId() ==-1);
                botState = BotState.MINING;
                return random.nextLong(1500,3000);
            }
        }
        else
        {
            if(Movement.traverse(NavPath.resolve(Alkharidmine.getRandomWalkableCoordinate())) == TraverseEvent.State.FINISHED)
            {
                println(" In Mining Area");
                botState = BotState.MINING;
            }
            // Walking to Mining Area
        }

        return random.nextLong(1250, 1750);
    }


    private long teleporting(LocalPlayer player)
    {
        if(player.getAnimationId() ==-1 && player.getCoordinate().getRegionId() != 13214 && warTeleport == true)
        {
            //if(Movement.traverse(NavPath.resolve(wararea.getRandomWalkableCoordinate())) == TraverseEvent.State.FINISHED)
            //{
                ActionBar.useAbility("War's Retreat Teleport");
                Execution.delayUntil(50000, () -> Client.getLocalPlayer().getAnimationId() ==-1);
                println(" Reached War Area");
                botState = BotState.BANKING;
            //}
        }else
        {
            if(Movement.traverse(NavPath.resolve(AlkharidCity.getRandomWalkableCoordinate())) == TraverseEvent.State.FINISHED)
            {
                println(" Reached AlKahrid City Area");
                botState = BotState.BANKING;
            }
        }

        return random.nextLong(1250,1750);
    }


    private int[] getGemBag(int packedValue) {
        int[] items = new int[4];
        for (int i = 0; i < 4; i++) {
            items[i] = packedValue & 0xFF;
            packedValue >>= 8;
        }
        return items;
    }
    
    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public boolean iswarTeleport() {
        return warTeleport;
    }

    public void setwarTeleport(boolean warTeleport) {
        this.warTeleport = warTeleport;
    }
    public boolean ismysticalseed() {
        return mysticalseed;
    }

    public void setmysticalseed(boolean mysticalseed) {
        this.mysticalseed = mysticalseed;
    }


}
