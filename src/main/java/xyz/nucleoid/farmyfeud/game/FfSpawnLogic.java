package xyz.nucleoid.farmyfeud.game;

import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class FfSpawnLogic {
    private final ServerWorld world;
    private final FfMap map;

    public FfSpawnLogic(ServerWorld world, FfMap map) {
        this.world = world;
        this.map = map;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.inventory.clear();
        player.getEnderChestInventory().clear();
        player.clearStatusEffects();
        player.setHealth(20.0F);
        player.getHungerManager().setFoodLevel(20);
        player.fallDistance = 0.0F;
        player.setGameMode(gameMode);
        player.setFireTicks(0);
        player.inLava = false;
    }

    public void spawnPlayerAtCenter(ServerPlayerEntity player) {
        BlockBounds centerSpawn = this.map.getCenterSpawn();
        if (centerSpawn != null) {
            this.spawnAt(player, new BlockPos(centerSpawn.getCenter()));
        } else {
            this.spawnAt(player, BlockPos.ORIGIN);
        }
    }

    public void spawnPlayerAtTeam(ServerPlayerEntity player, GameTeam team) {
        FfMap.TeamRegions teamRegions = this.map.getTeamRegions(team);
        if (teamRegions == null) {
            return;
        }

        BlockBounds teamSpawn = teamRegions.spawn;
        if (teamSpawn != null) {
            BlockPos spawnPos = new BlockPos(teamSpawn.getCenter());
            this.spawnAt(player, spawnPos);
        }
    }

    private void spawnAt(ServerPlayerEntity player, BlockPos spawnPos) {
        player.teleport(this.world, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
    }
}
