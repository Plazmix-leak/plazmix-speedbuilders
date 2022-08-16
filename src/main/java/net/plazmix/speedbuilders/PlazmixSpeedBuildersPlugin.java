package net.plazmix.speedbuilders;

import net.plazmix.core.PlazmixCoreApi;
import org.bukkit.ChatColor;
import org.bukkit.World;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.installer.GameInstaller;
import net.plazmix.game.installer.GameInstallerTask;
import net.plazmix.game.utility.GameSchedulers;
import net.plazmix.speedbuilders.mysql.GameStatsMysqlDatabase;
import net.plazmix.speedbuilders.state.EndingState;
import net.plazmix.speedbuilders.state.IngameState;
import net.plazmix.speedbuilders.state.WaitingState;

/*  Leaked by https://t.me/leak_mine
    - Все слитые материалы вы используете на свой страх и риск.

    - Мы настоятельно рекомендуем проверять код плагинов на хаки!
    - Список софта для декопиляции плагинов:
    1. Luyten (последнюю версию можно скачать можно тут https://github.com/deathmarine/Luyten/releases);
    2. Bytecode-Viewer (последнюю версию можно скачать можно тут https://github.com/Konloch/bytecode-viewer/releases);
    3. Онлайн декомпиляторы https://jdec.app или http://www.javadecompilers.com/

    - Предложить свой слив вы можете по ссылке @leakmine_send_bot или https://t.me/leakmine_send_bot
*/

public final class PlazmixSpeedBuildersPlugin
        extends GamePlugin {

    @Override
    public GameInstallerTask getInstallerTask() {
        return new PlazmixSpeedBuildersInstaller(this);
    }

    @Override
    protected void handleEnable() {
        saveResource("schema", false);

        // Setup game info.
        service.setGameName("SpeedBuilders");
        service.setServerMode("SpeedBuilders");

        service.setMaxPlayers(8);

        // Add game databases.
        service.addGameDatabase(new GameStatsMysqlDatabase());

        // Register game states.
        service.registerState(new WaitingState(this));
        service.registerState(new IngameState(this));
        service.registerState(new EndingState(this));

        // Execute installer task
        GameInstaller.create().executeInstall(getInstallerTask());

        // Run world ticker
        GameSchedulers.runTimer(0, 10, () -> {
            for (World world : getServer().getWorlds()) {

                world.setStorm(false);
                world.setThundering(false);

                world.setWeatherDuration(0);
                world.setTime(1200);
            }
        });
    }

    @Override
    protected void handleDisable() {
        broadcastMessage(ChatColor.RED + "Арена " + PlazmixCoreApi.getCurrentServerName() + " перезапускается!");
    }

}
