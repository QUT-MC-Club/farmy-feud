package net.gegy1000.farmyfeud.game.map;

import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.game.map.template.TemplateChunkGenerator;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.gegy1000.plasmid.util.BlockBounds;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class FfMap {
    private final MapTemplate template;

    private BlockBounds centerSpawn;
    private final Map<GameTeam, TeamRegions> teamRegions = new HashMap<>();

    public FfMap(MapTemplate template) {
        this.template = template;
    }

    public void setCenterSpawn(BlockBounds centerSpawn) {
        this.centerSpawn = centerSpawn;
    }

    public void addTeamRegions(GameTeam team, TeamRegions regions) {
        this.teamRegions.put(team, regions);
    }

    @Nullable
    public BlockBounds getCenterSpawn() {
        return this.centerSpawn;
    }

    @Nullable
    public TeamRegions getTeamRegions(GameTeam team) {
        return this.teamRegions.get(team);
    }

    public ChunkGenerator createGenerator() {
        return new TemplateChunkGenerator(this.template, BlockPos.ORIGIN);
    }

    public static class TeamRegions {
        public final BlockBounds spawn;
        public final BlockBounds pen;

        public TeamRegions(BlockBounds spawn, BlockBounds pen) {
            this.spawn = spawn;
            this.pen = pen;
        }
    }
}
