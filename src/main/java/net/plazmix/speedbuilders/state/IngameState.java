package net.plazmix.speedbuilders.state;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.setting.GameSetting;
import net.plazmix.game.state.GameState;
import net.plazmix.game.user.GameUser;
import net.plazmix.game.utility.GameSchedulers;
import net.plazmix.protocollib.entity.impl.FakeGuardian;
import net.plazmix.speedbuilders.scoreboard.IngameScoreboard;
import net.plazmix.speedbuilders.util.GameConstants;
import net.plazmix.speedbuilders.util.PlayerBuildZone;
import net.plazmix.utility.BukkitPotionUtil;
import net.plazmix.utility.NumberUtil;
import net.plazmix.utility.PlayerUtil;
import net.plazmix.utility.ProgressBar;
import net.plazmix.utility.location.region.CuboidRegion;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IngameState extends GameState {

    @Getter private final int maxSeconds = 60;
    @Getter private int leftSeconds = maxSeconds;
    @Getter private int round = 0;

    @Getter private long roundStartedMillis = System.currentTimeMillis();

    private FakeGuardian fakeGuardian;
    private BukkitTask guardianLookingTask;

    private void startGuardianLooking() {
        if (guardianLookingTask != null && !guardianLookingTask.isCancelled()) {
            return;
        }

        guardianLookingTask = GameSchedulers.runTimer(1, 1, () -> {

            if (fakeGuardian.getLocation().getYaw() >= 360) {
                fakeGuardian.look(0, 0);

            } else {

                fakeGuardian.look(fakeGuardian.getLocation().getYaw() + 10, 0);
            }
        });
    }

    private void cancelGuardianLooking() {
        guardianLookingTask.cancel();
        guardianLookingTask = null;
    }


    private CuboidClipboard currentSchematic;
    private final Map<Player, PlayerBuildZone> playerBuildZoneMap = new HashMap<>();

    public IngameState(GamePlugin plugin) {
        super(plugin, "Идет игра", false);
    }


    public PlayerBuildZone getBuildZone(@NonNull Player player) {
        return playerBuildZoneMap.get(player);
    }

    @SneakyThrows
    private void newBuildGeneration() {
        startGuardianLooking();
        round++;

        File[] schematicsFilesArray = plugin.getDataFolder().toPath().resolve("schema").toFile().listFiles();
        File schematicFile = Arrays.stream(Objects.requireNonNull(schematicsFilesArray))
                .filter(Objects::nonNull)
                .skip((long) (Math.random() * schematicsFilesArray.length))
                .findFirst()
                .orElse(null);

        CuboidClipboard baseSchematic = currentSchematic = CuboidClipboard.loadSchematic(Objects.requireNonNull(schematicFile));

        if (baseSchematic == null) {
            nextStage();
            return;
        }

        for (GameUser gameUser : getPlugin().getService().getAlivePlayers()) {
            gameUser.getBukkitHandle().setGameMode(GameMode.ADVENTURE);
            gameUser.getBukkitHandle().setAllowFlight(true);

            PlayerBuildZone buildZone = getBuildZone(gameUser.getBukkitHandle());

            gameUser.getBukkitHandle().sendTitle("§cИдет загрузка постройки...", "", 0, 100, 0);
            gameUser.getBukkitHandle().addPotionEffect(BukkitPotionUtil.getInfinityPotion(PotionEffectType.BLINDNESS, 3));

            gameUser.getBukkitHandle().teleport(buildZone.getCenterLocation().clone().add(0, 0, 4));
        }

        GameSchedulers.runLater(20 * 3, () -> {

            String schematicName = schematicFile.getName().split("_")[1];
            schematicName = schematicName.split("\\.")[0];

            plugin.getCache().set(GameConstants.CURRENT_SCHEMATIC_CACHE_ID, schematicName);


            for (GameUser gameUser : getPlugin().getService().getAlivePlayers()) {
                gameUser.getBukkitHandle().removePotionEffect(PotionEffectType.BLINDNESS);

                PlayerBuildZone buildZone = getBuildZone(gameUser.getBukkitHandle());
                buildZone.plantSchematic(baseSchematic);

                gameUser.getBukkitHandle().playSound(gameUser.getBukkitHandle().getLocation(), Sound.BLOCK_NOTE_BELL, 1, 2);
                gameUser.getBukkitHandle().sendTitle("Название постройки:", "§6" + schematicName);
            }

            GameSchedulers.runLater(20 * 10, () -> {
                roundStartedMillis = System.currentTimeMillis();

                for (GameUser gameUser : getPlugin().getService().getAlivePlayers()) {

                    gameUser.getBukkitHandle().playSound(gameUser.getBukkitHandle().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    gameUser.getBukkitHandle().sendTitle("", "§aПоехали в казахстан!");
                    gameUser.getBukkitHandle().setGameMode(GameMode.SURVIVAL);

                    gameUser.getBukkitHandle().setAllowFlight(true);

                    getBuildZone(gameUser.getBukkitHandle()).clearCuboidZone(block -> gameUser.getBukkitHandle().getInventory().addItem(block.getState().getData().toItemStack(1)));
                }

                AtomicBoolean hasAccess = new AtomicBoolean(false);

                BukkitTask bukkitTask1 = GameSchedulers.runTimer(0, 20, () -> {
                    if (leftSeconds > 0 && leftSeconds <= 5) {

                        for (Player player : Bukkit.getOnlinePlayers()) {

                            player.sendTitle(ChatColor.GREEN.toString() + leftSeconds, "");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1);
                        }
                    }

                    if (leftSeconds != 0) {
                        leftSeconds--;
                        return;
                    }

                    hasAccess.set(true);
                    Map<Player, Integer> playerCompletablePercentsMap = new HashMap<>();

                    for (GameUser gameUser : getPlugin().getService().getAlivePlayers()) {
                        gameUser.getBukkitHandle().setGameMode(GameMode.SPECTATOR);

                        int completablePercent = getCompletablePercent(baseSchematic, gameUser.getBukkitHandle());
                        playerCompletablePercentsMap.put(gameUser.getBukkitHandle(), completablePercent);

                        gameUser.getBukkitHandle().sendTitle("§6§lРАУНД #" + round + " ЗАВЕРШЕН", "§fВаша постройка закончена на §b" + completablePercent + "%");
                    }

                    Optional<Player> looserOptional = playerCompletablePercentsMap.keySet()
                            .stream()

                            .sorted(Comparator.comparingInt(playerCompletablePercentsMap::get))
                            .max(Comparator.comparingInt(player -> GameUser.from(player).getCache().getInt(GameConstants.INGAME_TIME_PLAYER_CACHE_ID)));

                    cancelGuardianLooking();
                    looserOptional.ifPresent(player -> fakeGuardian.look(getBuildZone(player).getCenterLocation()));

                    GameSchedulers.runLater(20 * 3, () -> {
                        looserOptional.ifPresent(player -> {

                            PlayerBuildZone buildZone = getBuildZone(player);

                            if (buildZone != null) {
                                buildZone.breakCuboidZone();
                            }

                            GameUser gameUser = GameUser.from(player);
                            GameSchedulers.runLater(20 * 4 + 10, () -> gameUser.setGhost(true));

                            getPlugin().broadcastMessage(GameConstants.PREFIX + gameUser.getPlazmixHandle().getDisplayName() + " §cвылетает из игры");
                        });

                        GameSchedulers.runLater(20 * 5, () -> {
                            leftSeconds = maxSeconds;

                            if (getPlugin().getService().getAlivePlayers().size() <= 1) {
                                GamePlugin.getInstance().getCache().set("Winner", getPlugin().getService().getAlivePlayers()
                                        .stream()
                                        .min(Comparator.comparingInt(gameUser -> gameUser.getCache().getInt(GameConstants.INGAME_TIME_PLAYER_CACHE_ID)))
                                        .orElse(null));

                                nextStage();

                            } else {

                                newBuildGeneration();
                            }
                        });
                    });
                });

                BukkitTask bukkitTask2 = GameSchedulers.runTimer(0, 1, () -> {

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (getCompletablePercent(baseSchematic, player) < 100) {
                            GameUser.from(player).getCache().set(GameConstants.INGAME_TIME_PLAYER_CACHE_ID, (int)(System.currentTimeMillis() - roundStartedMillis));
                        }

                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§e" + convertTimeMillis(GameUser.from(player).getCache().getInt(GameConstants.INGAME_TIME_PLAYER_CACHE_ID)) + " §8| §fОсталось: " + convertTimeMillis(maxSeconds * 1000L - Math.abs(System.currentTimeMillis() - roundStartedMillis)) + " "
                                + new ProgressBar(leftSeconds, maxSeconds, 40, "§a", "§7", "|").getProgressBar()));
                    }
                });

                GameSchedulers.runTimer(0, 2, () -> {

                    if (hasAccess.get() && leftSeconds == 0) {

                        bukkitTask1.cancel();
                        bukkitTask2.cancel();
                    }
                });
            });
        });
    }

    @SneakyThrows
    private int getCompletablePercent(@NonNull CuboidClipboard baseSchematic, @NonNull Player player) {
        Map<Location, MaterialData> correctBlocksMap = new HashMap<>();
        Location originLocation = getBuildZone(player).getCenterLocation().clone().add(0, 0, 3);

        int maxX = baseSchematic.getWidth();
        int maxY = baseSchematic.getHeight();
        int maxZ = baseSchematic.getLength();

        Location origin = originLocation.clone().add(baseSchematic.getOffset().getBlockX(),
                baseSchematic.getOffset().getBlockY(),
                baseSchematic.getOffset().getBlockZ());

        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                for (int z = 0; z < maxZ; z++) {
                    BaseBlock block = baseSchematic.getBlock(new com.sk89q.worldedit.Vector(x, y, z));

                    if (!block.isAir()) {
                        Location blockLocation = origin.clone().add(x, y, z);

                        correctBlocksMap.put(blockLocation, new MaterialData(block.getId(), (byte) block.getData()));
                    }
                }
            }
        }

        AtomicInteger playerCorrectBlocksCount = new AtomicInteger();
        getBuildZone(player).getCuboidRegion().forEachBlock(block -> {

            if (!correctBlocksMap.containsKey(block.getLocation())) {
                return;
            }

            if (!correctBlocksMap.get(block.getLocation()).equals(block.getState().getData())) {
                return;
            }

            playerCorrectBlocksCount.set(playerCorrectBlocksCount.get() + 1);
        });

        return NumberUtil.getIntPercent(playerCorrectBlocksCount.get(), correctBlocksMap.size());
    }

    @Override
    protected void onStart() {
        List<Location> playerSpawnsList = getPlugin().getCache().getOrDefault(GameConstants.PLAYERS_SPAWNS_CACHE_ID, ArrayList::new);

        if (playerSpawnsList.isEmpty()) {
            forceShutdown();
            return;
        }

        // change game settings.
        GameSetting.INTERACT_BLOCK.set(plugin.getService(), true);
        GameSetting.BLOCK_PLACE.set(plugin.getService(), true);

        // Spawn Guardian entity.
        fakeGuardian = new FakeGuardian(getPlugin().getCache().get(GameConstants.GUARDIAN_POINT_CACHE_ID));
        fakeGuardian.setRetractingSpikes(true);
        fakeGuardian.spawn();

        startGuardianLooking();

        // handle players.
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = playerSpawnsList.stream().skip((long) (Math.random() * playerSpawnsList.size())).findFirst().orElse(null);

            if (playerLocation == null) {
                GameUser.from(player).setGhost(true);
                continue;
            }

            // Initialize player location & build zone.
            playerSpawnsList.remove(playerLocation);
            player.teleport(playerLocation);

            playerBuildZoneMap.put(player, new PlayerBuildZone(player, playerLocation, new CuboidRegion(playerLocation.clone().add(3, 20, 3), playerLocation.clone().subtract(3, 0, 3))));

            // Announce of game start.
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.sendTitle("§a§lИГРА НАЧАЛАСЬ", "§fВоссоздайте все постройки корректно и быстро!");

            player.setGameMode(GameMode.ADVENTURE);

            new IngameScoreboard(player);
        }

        newBuildGeneration();
    }

    @Override
    protected void onShutdown() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleEvent(BlockPlaceEvent event) {
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
            event.setCancelled(true);
            return;
        }

        if (!getBuildZone(event.getPlayer()).getCuboidRegion().contains(event.getBlockPlaced())) {
            event.setCancelled(true);
            return;
        }

        if (currentSchematic != null && getCompletablePercent(currentSchematic, event.getPlayer()) >= 100) {
            event.getPlayer().setGameMode(GameMode.ADVENTURE);

            event.getPlayer().sendTitle("", "§eВы завершили постройку, ожидайте других");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 1);

            event.getPlayer().setAllowFlight(true);
            event.getPlayer().setFlying(true);

            getPlugin().broadcastMessage(GameConstants.PREFIX + PlayerUtil.getDisplayName(event.getPlayer())
                    + " §fзавершил постройку за §e" + convertTimeMillis((int) (System.currentTimeMillis() - roundStartedMillis)));

            if (getPlugin().getService().getAlivePlayers().size() == getPlugin().getService().getAlivePlayers()
                    .stream()
                    .filter(gameUser1 -> getCompletablePercent(currentSchematic, gameUser1.getBukkitHandle()) >= 100)
                    .count()) {

                leftSeconds = 0;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleEvent(PlayerInteractEvent event) {
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
            return;
        }

        if (event.getClickedBlock() != null && event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (!getBuildZone(event.getPlayer()).getCuboidRegion().contains(event.getClickedBlock())) {
                return;
            }

            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            event.getPlayer().getInventory().addItem(event.getClickedBlock().getState().getData().toItemStack(1));

            event.getClickedBlock().setType(Material.AIR);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleEvent(EntityDamageEvent event) {

        if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleEvent(PlayerDeathEvent event) {
        event.getEntity().spigot().respawn();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getLocation().getY() < 25) {

            if (plugin.getService().isGhost(player)) {
                player.setHealth(0);

            } else {

                player.teleport(getBuildZone(player).getCenterLocation().clone().add(0, 0, 4));
            }
        }
    }

    @EventHandler
    public void handleEvent(PlayerToggleFlightEvent event) {
        event.getPlayer().setVelocity(new Vector(0, 1, 0));
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        event.setCancelled(true);
    }

    @EventHandler
    public void handleEvent(PlayerQuitEvent event) {

        if (GameUser.from(event.getPlayer()).isAlive()) {
            getBuildZone(event.getPlayer()).breakCuboidZone();

            getPlugin().broadcastMessage(GameConstants.PREFIX + PlayerUtil.getDisplayName(event.getPlayer()) + " §fвышел из игры!");

            playerBuildZoneMap.remove(event.getPlayer());

            if (getPlugin().getService().getAlivePlayers().size() <= 1) {
                GamePlugin.getInstance().getCache().set("Winner", getPlugin().getService().getAlivePlayers().stream().findFirst().orElse(null));

                leftSeconds = 0;
                nextStage();
            }
        }
    }

    private String convertTimeMillis(long timeMillis) {
        return TimeUnit.MILLISECONDS.toSeconds(timeMillis) + "." + String.valueOf(timeMillis / 100).substring(String.valueOf(timeMillis / 100).length() - 1) + "s";
    }

}
