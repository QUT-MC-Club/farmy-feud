package xyz.nucleoid.farmyfeud.game;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.map_templates.BlockBounds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

import java.util.Set;

public record FfSpawnLogic(ServerWorld world, FfMap map) {

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        player.clearStatusEffects();
        player.setHealth(20.0F);
        player.getHungerManager().setFoodLevel(20);
        player.fallDistance = 0.0F;
        player.changeGameMode(gameMode);
        player.setFireTicks(0);
    }

    public void spawnPlayerAtCenter(ServerPlayerEntity player) {
        BlockBounds centerSpawn = this.map.getCenterSpawn();
        if (centerSpawn != null) {
            this.spawnAt(player, BlockPos.ofFloored(centerSpawn.center()), 0);
        } else {
            this.spawnAt(player, BlockPos.ORIGIN, 0);
        }
    }

    public void spawnPlayerAtTeam(ServerPlayerEntity player, GameTeamKey team) {
        FfMap.TeamRegions teamRegions = this.map.getTeamRegions(team);
        if (teamRegions == null) {
            return;
        }

        BlockBounds teamSpawn = teamRegions.spawn();
        if (teamSpawn != null) {
            BlockPos spawnPos = BlockPos.ofFloored(teamSpawn.center());
            BlockPos center;
            var centerSpawn = this.map.getCenterSpawn();
            if (centerSpawn != null) {
                center = BlockPos.ofFloored(centerSpawn.center());
            } else {
                center = BlockPos.ORIGIN;
            }

            this.spawnAt(player, spawnPos, 0);
            player.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, Vec3d.ofCenter(center.withY(spawnPos.getY())));
        }
    }

    private void spawnAt(ServerPlayerEntity player, BlockPos spawnPos, float yaw) {
        player.teleport(this.world, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, Set.of(), yaw, 0.0F, false);
    }
}
