package net.plazmix.speedbuilders.state;

import lombok.NonNull;
import net.plazmix.core.PlazmixCoreApi;
import net.plazmix.speedbuilders.mysql.GameStatsMysqlDatabase;
import net.plazmix.speedbuilders.scoreboard.EndingScoreboard;
import net.plazmix.speedbuilders.util.GameConstants;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.mysql.GameMysqlDatabase;
import net.plazmix.game.setting.GameSetting;
import net.plazmix.game.state.type.StandardEndingState;
import net.plazmix.game.user.GameUser;
import net.plazmix.game.utility.GameSchedulers;
import net.plazmix.game.utility.hotbar.GameHotbar;
import net.plazmix.game.utility.hotbar.GameHotbarBuilder;
import net.plazmix.game.utility.worldreset.GameWorldReset;
import net.plazmix.utility.ItemUtil;

public class EndingState extends StandardEndingState {

    private final GameHotbar gameHotbar = GameHotbarBuilder.newBuilder()
            .setMoveItems(true)
            .addItem(5, ItemUtil.newBuilder(Material.PAPER)
                            .setName("§aСыграть еще раз")
                            .build(),
                    player -> GamePlugin.getInstance().getService().playAgain(player))

            .addItem(9, ItemUtil.newBuilder(Material.MAGMA_CREAM)
                            .setName("§aПокинуть арену")
                            .build(),
                    PlazmixCoreApi::redirectToLobby)
            .build();


    public EndingState(@NonNull GamePlugin plugin) {
        super(plugin, "Перезагрузка");

        GameSetting.INTERACT_BLOCK.set(plugin.getService(), false);
    }

    @Override
    protected String getWinnerPlayerName() {
        return GamePlugin.getInstance().getCache().get("Winner");
    }

    @Override
    protected void handleStart() {
        GameWorldReset.resetAllWorlds();
        GameUser winnerUser = GamePlugin.getInstance().getCache().get("Winner");

        if (winnerUser == null) {
            plugin.broadcastMessage(ChatColor.RED + "Произошли техничекие неполадки, из-за чего игра была принудительно остановлена!");

            forceShutdown();
            return;
        }

        // Add player win.
        winnerUser.getCache().increment(GameConstants.WINS_PLAYER_CACHE_ID);

        // Run fireworks spam.
        GameSchedulers.runTimer(0, 20, () -> {

            if (winnerUser.getBukkitHandle() == null) {
                return;
            }

            Firework firework = winnerUser.getBukkitHandle().getWorld().spawn(winnerUser.getBukkitHandle().getLocation(), Firework.class);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();

            fireworkMeta.setPower(1);
            fireworkMeta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.STAR)
                    .withColor(Color.RED)
                    .withColor(Color.GREEN)
                    .withColor(Color.WHITE)
                    .build());

            firework.setFireworkMeta(fireworkMeta);
        });

        GameMysqlDatabase statsMysqlDatabase = plugin.getService().getGameDatabase(GameStatsMysqlDatabase.class);

        for (Player player : Bukkit.getOnlinePlayers()) {
            GameUser gameUser = GameUser.from(player);

            // Announcements.
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2, 0);
            player.sendMessage(GameConstants.PREFIX + "§aИгра окончена!");

            if (winnerUser.getName().equalsIgnoreCase(player.getName())) {
                player.sendTitle("§6§lПОБЕДА", "§fВы воссоздали все постройки быстрее и лучше всех!");

                player.sendMessage("§e+250 монет (победа)");
                gameUser.getPlazmixHandle().addCoins(250);

            } else {

                player.sendTitle("§c§lПОРАЖЕНИЕ", "§fВ этот раз лучшим оказался " + winnerUser.getPlazmixHandle().getDisplayName());
            }

            // Player data insert
            int bestTime = gameUser.getCache().getInt(GameConstants.INGAME_TIME_PLAYER_CACHE_ID);
            gameUser.getCache().increment(GameConstants.GAMES_PLAYED_PLAYER_CACHE_ID);

            if (bestTime < gameUser.getCache().getInt(GameConstants.BEST_TIME_PLAYER_CACHE_ID)) {
                gameUser.getCache().set(GameConstants.BEST_TIME_PLAYER_CACHE_ID, bestTime);
            }

            // Give rewards.
            int coins = (bestTime / 1000) * 10;

            player.sendMessage("§e+" + coins + " монет");
            gameUser.getPlazmixHandle().addCoins(coins);

            player.sendMessage("§3+5 опыта");
            gameUser.getPlazmixHandle().addExperience(5);

            // Set hotbar items.
            gameHotbar.setHotbarTo(player);

            // Update player data in database.
            statsMysqlDatabase.insert(false, GameUser.from(player));
        }
    }

    @Override
    protected void handleScoreboardSet(@NonNull Player player) {
        new EndingScoreboard(player);
    }

    @Override
    protected Location getTeleportLocation() {
        return plugin.getService().getMapWorld().getSpawnLocation().clone().add(0.5, 0, 0.5);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

}
