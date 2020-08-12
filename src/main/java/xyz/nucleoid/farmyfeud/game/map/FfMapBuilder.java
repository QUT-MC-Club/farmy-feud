package xyz.nucleoid.farmyfeud.game.map;

import net.minecraft.world.biome.BuiltinBiomes;
import xyz.nucleoid.farmyfeud.FarmyFeud;
import xyz.nucleoid.farmyfeud.game.FfConfig;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.game.map.template.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.concurrent.CompletableFuture;

public final class FfMapBuilder {
    private final FfConfig config;

    public FfMapBuilder(FfConfig config) {
        this.config = config;
    }

    public CompletableFuture<FfMap> create() {
        return MapTemplateSerializer.INSTANCE.load(this.config.map).thenApply(template -> {
            FfMap map = new FfMap(template);

            this.addGlobalRegions(map, template);
            this.addTeamRegions(map, template);

            template.setBiome(BuiltinBiomes.PLAINS);

            return map;
        });
    }

    private void addGlobalRegions(FfMap map, MapTemplate template) {
        BlockBounds sheepSpawn = template.getFirstRegion("sheep_spawn");
        map.setCenterSpawn(sheepSpawn);

        template.getRegions("sheep_die").forEach(map::addIllegalSheepRegion);
    }

    private void addTeamRegions(FfMap map, MapTemplate template) {
        for (GameTeam team : this.config.teams) {
            String key = team.getKey();

            BlockBounds spawn = template.getFirstRegion(key + "_spawn");
            if (spawn == null) {
                FarmyFeud.LOGGER.warn("Missing '{}_spawn'", key);
            }

            BlockBounds pen = template.getFirstRegion(key + "_pen");
            if (pen == null) {
                FarmyFeud.LOGGER.warn("Missing '{}_pen'", key);
            }

            map.addTeamRegions(team, new FfMap.TeamRegions(spawn, pen));
        }
    }
}
