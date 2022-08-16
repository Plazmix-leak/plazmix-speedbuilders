package net.plazmix.speedbuilders.scoreboard;

import lombok.NonNull;
import net.plazmix.core.PlazmixCoreApi;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.user.GameUser;
import net.plazmix.scoreboard.BaseScoreboardBuilder;
import net.plazmix.scoreboard.BaseScoreboardScope;
import net.plazmix.utility.DateUtil;

public class EndingScoreboard {

    public EndingScoreboard(@NonNull Player player) {
        GameUser winnerUser = GamePlugin.getInstance().getCache().get("Winner");
        BaseScoreboardBuilder scoreboardBuilder = BaseScoreboardBuilder.newScoreboardBuilder();

        scoreboardBuilder.scoreboardDisplay("§d§lSPEED BUILDERS");
        scoreboardBuilder.scoreboardScope(BaseScoreboardScope.PROTOTYPE);

        scoreboardBuilder.scoreboardLine(9, ChatColor.GRAY + "SpeedBuilders " + DateUtil.formatPattern(DateUtil.DEFAULT_DATE_PATTERN));
        scoreboardBuilder.scoreboardLine(8, "");
        scoreboardBuilder.scoreboardLine(7, "§fПобедитель игры:");
        scoreboardBuilder.scoreboardLine(6, " " + (winnerUser != null ? winnerUser.getPlazmixHandle().getDisplayName() : "§cНеопределено"));
        scoreboardBuilder.scoreboardLine(5, "");
        scoreboardBuilder.scoreboardLine(4, "§fКарта: §a" + GamePlugin.getInstance().getService().getMapName());
        scoreboardBuilder.scoreboardLine(3, "§fСервер: §a" + PlazmixCoreApi.getCurrentServerName());
        scoreboardBuilder.scoreboardLine(2, "");
        scoreboardBuilder.scoreboardLine(1, "§dwww.plazmix.net");

        scoreboardBuilder.build().setScoreboardToPlayer(player);
    }

}
