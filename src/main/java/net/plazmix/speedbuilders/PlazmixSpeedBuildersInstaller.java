package net.plazmix.speedbuilders;

import lombok.NonNull;
import net.plazmix.game.GameCache;
import net.plazmix.speedbuilders.util.GameConstants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import net.plazmix.game.GamePlugin;
import net.plazmix.game.installer.GameInstallerTask;

import java.util.ArrayList;
import java.util.List;

public final class PlazmixSpeedBuildersInstaller extends GameInstallerTask {

    public PlazmixSpeedBuildersInstaller(@NonNull GamePlugin plugin) {
        super(plugin);
    }

    @Override
    protected void handleExecute(@NonNull Actions actions, @NonNull Settings settings) {

        // Init map settings.
        settings.setCenter(plugin.getService().getMapWorld().getSpawnLocation());
        settings.setUseOnlyTileBlocks(false);
        settings.setRadius(150);

        // Init required variables.
        GameCache gameCache = GamePlugin.getInstance().getCache();
        List<Location> playerSpawnsList = new ArrayList<>();

        // Init players spawns.
        actions.addEntity(EntityType.ARMOR_STAND, (entity) -> {
            entity.remove();

            // Add locations
            playerSpawnsList.add(entity.getLocation());
            gameCache.set(GameConstants.PLAYERS_SPAWNS_CACHE_ID, playerSpawnsList);

            // Update max players value.
            GamePlugin.getInstance().getService().setMaxPlayers(playerSpawnsList.size());
        });

        // Init guardian point.
        actions.addBlock(Material.BEACON, (block) -> {
            Bukkit.getScheduler().runTask(GamePlugin.getInstance(), () -> block.setType(Material.AIR));

            // Cache location value.
            gameCache.set(GameConstants.GUARDIAN_POINT_CACHE_ID, block.getLocation());
        });
    }

}
