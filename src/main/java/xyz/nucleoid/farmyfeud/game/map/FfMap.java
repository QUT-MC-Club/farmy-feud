package xyz.nucleoid.farmyfeud.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class FfMap {
    private final MapTemplate template;

    private BlockBounds centerSpawn;
    private final Map<GameTeam, TeamRegions> teamRegions = new HashMap<>();

    private final Collection<BlockBounds> illegalSheepRegions = new ArrayList<>();

    public FfMap(MapTemplate template) {
        this.template = template;
    }

    public void setCenterSpawn(BlockBounds centerSpawn) {
        this.centerSpawn = centerSpawn;
    }

    public void addTeamRegions(GameTeam team, TeamRegions regions) {
        this.teamRegions.put(team, regions);
        this.addIllegalSheepRegion(regions.spawn);
    }

    public void addIllegalSheepRegion(BlockBounds bounds) {
        this.illegalSheepRegions.add(bounds);
    }

    @Nullable
    public BlockBounds getCenterSpawn() {
        return this.centerSpawn;
    }

    @Nullable
    public TeamRegions getTeamRegions(GameTeam team) {
        return this.teamRegions.get(team);
    }

    public Collection<BlockBounds> getIllegalSheepRegions() {
        return this.illegalSheepRegions;
    }

    public ChunkGenerator createGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
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
