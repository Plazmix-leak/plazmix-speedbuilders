package net.plazmix.speedbuilders.scoreboard;

import lombok.NonNull;
import net.plazmix.core.PlazmixCoreApi;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.user.GameUser;
import net.plazmix.scoreboard.BaseScoreboardBuilder;
import net.plazmix.scoreboard.BaseScoreboardScope;
import net.plazmix.speedbuilders.state.IngameState;
import net.plazmix.speedbuilders.util.GameConstants;
import net.plazmix.utility.DateUtil;

import java.util.concurrent.TimeUnit;

public class IngameScoreboard {

    public IngameScoreboard(@NonNull Player player) {
        BaseScoreboardBuilder scoreboardBuilder = BaseScoreboardBuilder.newScoreboardBuilder();

        scoreboardBuilder.scoreboardDisplay("§d§lSPEED BUILDERS");
        scoreboardBuilder.scoreboardScope(BaseScoreboardScope.PROTOTYPE);

        scoreboardBuilder.scoreboardLine(12, ChatColor.GRAY + "SpeedBuilders " + DateUtil.formatPattern(DateUtil.DEFAULT_DATE_PATTERN));
        scoreboardBuilder.scoreboardLine(11, "");
        scoreboardBuilder.scoreboardLine(10, "§fВремени прошло: §e0s");
        scoreboardBuilder.scoreboardLine(9, "§fВремени осталось: §a0s");
        scoreboardBuilder.scoreboardLine(8, "");
        scoreboardBuilder.scoreboardLine(7, "§fПостройка: §b[Неопределено]");
        scoreboardBuilder.scoreboardLine(6, "§fРаунд: §c#0");
        scoreboardBuilder.scoreboardLine(5, "");
        scoreboardBuilder.scoreboardLine(4, "§fКарта: §a" + GamePlugin.getInstance().getService().getMapName());
        scoreboardBuilder.scoreboardLine(3, "§fСервер: §a" + PlazmixCoreApi.getCurrentServerName());
        scoreboardBuilder.scoreboardLine(2, "");
        scoreboardBuilder.scoreboardLine(1, "§dwww.plazmix.net");

        IngameState ingameState = (IngameState) GamePlugin.getInstance()
                .getService()
                .getStateManager()
                .getCurrentState();

        scoreboardBuilder.scoreboardUpdater((baseScoreboard, player1) -> {
            baseScoreboard.updateScoreboardLine(10, player, "§fВремени прошло: §e" + convertTimeMillis(GameUser.from(player).getCache().getInt(GameConstants.INGAME_TIME_PLAYER_CACHE_ID)));
            baseScoreboard.updateScoreboardLine(9, player, "§fВремени осталось: §e" + ingameState.getLeftSeconds() + "s");

            String currentSchematic = GamePlugin.getInstance().getCache().get(GameConstants.CURRENT_SCHEMATIC_CACHE_ID);

            if (currentSchematic != null) {
                baseScoreboard.updateScoreboardLine(7, player, "§fПостройка: §b" + currentSchematic);
            }

            baseScoreboard.updateScoreboardLine(6, player, "§fРаунд: §c#" + ingameState.getRound());

        }, 1);

        scoreboardBuilder.build().setScoreboardToPlayer(player);
    }

    private String convertTimeMillis(long timeMillis) {
        return TimeUnit.MILLISECONDS.toSeconds(timeMillis) + "." + String.valueOf(timeMillis / 100).substring(String.valueOf(timeMillis / 100).length() - 1) + "s";
    }

}
