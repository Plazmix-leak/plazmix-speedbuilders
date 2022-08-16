package net.plazmix.speedbuilders.mysql;

import lombok.NonNull;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.mysql.GameMysqlDatabase;
import net.plazmix.game.mysql.RemoteDatabaseRowType;
import net.plazmix.game.user.GameUser;
import net.plazmix.speedbuilders.util.GameConstants;

public final class GameStatsMysqlDatabase
        extends GameMysqlDatabase {

    public GameStatsMysqlDatabase() {
        super("SpeedBuilders", true);
    }

    @Override
    public void initialize() {
        super.addColumn(GameConstants.WINS_PLAYER_CACHE_ID, RemoteDatabaseRowType.INT, gameUser -> gameUser.getCache().getInt(GameConstants.WINS_PLAYER_CACHE_ID));
        super.addColumn(GameConstants.BEST_TIME_PLAYER_CACHE_ID, RemoteDatabaseRowType.INT, gameUser -> gameUser.getCache().getInt(GameConstants.BEST_TIME_PLAYER_CACHE_ID));
        super.addColumn(GameConstants.GAMES_PLAYED_PLAYER_CACHE_ID, RemoteDatabaseRowType.INT, gameUser -> gameUser.getCache().getInt(GameConstants.GAMES_PLAYED_PLAYER_CACHE_ID));
    }

    @Override
    public void onJoinLoad(@NonNull GamePlugin gamePlugin, @NonNull GameUser gameUser) {
        loadPrimary(false, gameUser, gameUser.getCache()::set);
    }

}
