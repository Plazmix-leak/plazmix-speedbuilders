package net.plazmix.speedbuilders.util;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.plazmix.game.utility.GameSchedulers;
import net.plazmix.utility.location.region.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PlayerBuildZone {

    Player player;
    Location centerLocation;
    CuboidRegion cuboidRegion;

    @SneakyThrows
    public void plantSchematic(@NonNull CuboidClipboard cuboidClipboard) {
        int maxX = cuboidClipboard.getWidth();
        int maxY = cuboidClipboard.getHeight();
        int maxZ = cuboidClipboard.getLength();

        Location origin = centerLocation.clone().add(cuboidClipboard.getOffset().getBlockX(),
                cuboidClipboard.getOffset().getBlockY(),
                cuboidClipboard.getOffset().getBlockZ());

        Map<Location, BaseBlock> blocksMap = new HashMap<>();

        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                for (int z = 0; z < maxZ; z++) {
                    BaseBlock block = cuboidClipboard.getBlock(new com.sk89q.worldedit.Vector(x, y, z));

                    if (!block.isAir()) {
                        Location blockLocation = origin.clone().add(x, y, z);

                        blocksMap.put(blockLocation, block);
                    }
                }
            }
        }

        blocksMap.forEach((location, baseBlock) -> {

            Block block = location.getBlock();

            block.setTypeId(baseBlock.getType());
            block.setData((byte) baseBlock.getData());
        });
    }

    public void clearCuboidZone(Consumer<Block> blockConsumer) {
        cuboidRegion.forEachBlock(block -> {

            if (block.isEmpty()) {
                return;
            }

            if (blockConsumer != null) {
                blockConsumer.accept(block);
            }

            block.setType(Material.AIR);
        });
    }

    public void breakCuboidZone() {
        Collection<FallingBlock> fallingBlockCollection = new ArrayList<>();
        CuboidRegion cuboidRegion = new CuboidRegion(centerLocation.clone().add(8, 10, 8), centerLocation.clone().subtract(8, 8, 8));

        centerLocation.getWorld().playSound(centerLocation, Sound.ENTITY_GENERIC_EXPLODE, 10, 2);

        cuboidRegion.forEachBlock(block -> {

            if (block.isEmpty()) {
                return;
            }

            FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation(), block.getState().getData());
            fallingBlock.setTicksLived(100);

            fallingBlock.setHurtEntities(false);
            fallingBlock.setDropItem(false);

            fallingBlock.setVelocity(new Vector(ThreadLocalRandom.current().nextDouble(-0.12, 0.12), 2, ThreadLocalRandom.current().nextDouble(-0.12, 0.12)));
            fallingBlockCollection.add(fallingBlock);

            block.setType(Material.AIR);
        });

        GameSchedulers.runLater(20 * 3, () -> {

            for (FallingBlock fallingBlock : fallingBlockCollection) {
                fallingBlock.remove();
            }
        });
    }
}
