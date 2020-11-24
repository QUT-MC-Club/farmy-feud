package xyz.nucleoid.farmyfeud.game.active;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.farmyfeud.entity.FarmSheepEntity;
import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

final class FfCaptureLogic {
    private final FfActive game;

    FfCaptureLogic(FfActive game) {
        this.game = game;
    }

    @Nullable
    public GameTeam getTeamAt(Vec3d pos) {
        for (GameTeam team : this.game.config.teams) {
            if (this.isCapturedBy(pos, team)) {
                return team;
            }
        }

        return null;
    }

    public void captureSheep(FarmSheepEntity sheep, GameTeam team) {
        if (team == sheep.getOwnerTeam()) {
            return;
        }

        if (sheep.getOwnerTeam() != null) {
            FfTeamState lastTeam = this.game.getTeam(sheep.getOwnerTeam());
            lastTeam.decrementCapturedSheep();
        }

        FfTeamState newTeam = this.game.getTeam(team);
        newTeam.incrementCapturedSheep();

        FfMap.TeamRegions regions = this.game.map.getTeamRegions(team);

        BlockBounds teamPen = regions != null ? regions.pen : null;
        if (teamPen == null) {
            teamPen = BlockBounds.of(sheep.getBlockPos());
        }

        sheep.setOwnerTeam(team, teamPen);

        this.spawnFireworkAt(sheep, team);
    }

    private void spawnFireworkAt(Entity entity, GameTeam team) {
        World world = entity.world;

        int flight = world.random.nextInt(3);

        FireworkItem.Type type = world.random.nextInt(4) == 0 ? FireworkItem.Type.STAR : FireworkItem.Type.BURST;
        FireworkRocketEntity firework = new FireworkRocketEntity(
                world,
                entity.getX(),
                entity.getEyeY(),
                entity.getZ(),
                team.createFirework(flight, type)
        );

        entity.world.spawnEntity(firework);
    }

    private boolean isCapturedBy(Vec3d pos, GameTeam team) {
        FfMap.TeamRegions regions = this.game.map.getTeamRegions(team);
        if (regions == null || regions.pen == null) {
            return false;
        }

        BlockPos min = regions.pen.getMin();
        BlockPos max = regions.pen.getMax();

        return pos.x >= min.getX() && pos.z >= min.getZ()
                && pos.x <= max.getX() && pos.z <= max.getZ();
    }
}
