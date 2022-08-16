package net.plazmix.speedbuilders.scoreboard;

import lombok.NonNull;
import net.plazmix.core.PlazmixCoreApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.state.type.StandardWaitingState;
import net.plazmix.scoreboard.BaseScoreboardBuilder;
import net.plazmix.scoreboard.BaseScoreboardScope;
import net.plazmix.utility.DateUtil;
import net.plazmix.utility.NumberUtil;

public class WaitingScoreboard {

    public WaitingScoreboard(@NonNull StandardWaitingState.TimerStatus timerStatus, @NonNull Player player) {
        GamePlugin gamePlugin = GamePlugin.getInstance();
        BaseScoreboardBuilder scoreboardBuilder = BaseScoreboardBuilder.newScoreboardBuilder();

        scoreboardBuilder.scoreboardDisplay("§d§lSPEED BUILDERS");
        scoreboardBuilder.scoreboardScope(BaseScoreboardScope.PROTOTYPE);

        scoreboardBuilder.scoreboardLine(10, "SpeedBuilders " + ChatColor.GRAY + DateUtil.formatPattern(DateUtil.DEFAULT_DATE_PATTERN));
        scoreboardBuilder.scoreboardLine(9, "");
        scoreboardBuilder.scoreboardLine(8, "§fКарта: §a" + gamePlugin.getService().getMapName());
        scoreboardBuilder.scoreboardLine(7, "§fИгроки: §a" + Bukkit.getOnlinePlayers().size() + "§f/§c" + gamePlugin.getService().getMaxPlayers());
        scoreboardBuilder.scoreboardLine(6, "");
        scoreboardBuilder.scoreboardLine(5, "[...]");
        scoreboardBuilder.scoreboardLine(4, "");
        scoreboardBuilder.scoreboardLine(3, "§fСервер: §a" + PlazmixCoreApi.getCurrentServerName());
        scoreboardBuilder.scoreboardLine(2, "");
        scoreboardBuilder.scoreboardLine(1, "§dwww.plazmix.net");

        scoreboardBuilder.scoreboardUpdater((baseScoreboard, player1) -> {
            baseScoreboard.updateScoreboardLine(7, player, "§fИгроки: §a" + Bukkit.getOnlinePlayers().size() + "§f/§c" + gamePlugin.getService().getMaxPlayers());
            baseScoreboard.updateScoreboardLine(5, player, (!timerStatus.isLived() ? "§7Ожидание игроков..." : "§fИгра начнется через §a" + NumberUtil.formattingSpaced(timerStatus.getLeftSeconds(), "§fсекунду", "§fсекунды", "§fсекунд")));

        }, 20);

        scoreboardBuilder.build().setScoreboardToPlayer(player);
    }

}
