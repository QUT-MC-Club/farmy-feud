package net.gegy1000.farmyfeud.game.map;

import net.gegy1000.farmyfeud.FarmyFeud;
import net.gegy1000.farmyfeud.game.FfConfig;
import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.game.map.template.MapTemplateSerializer;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.gegy1000.plasmid.util.BlockBounds;

import java.util.concurrent.CompletableFuture;

public final class FfMapBuilder {
    private final FfConfig config;

    public FfMapBuilder(FfConfig config) {
        this.config = config;
    }

    public CompletableFuture<FfMap> create() {
        return MapTemplateSerializer.load(this.config.map).thenApply(template -> {
            FfMap map = new FfMap(template);

            this.addGlobalRegions(map, template);
            this.addTeamRegions(map, template);

            return map;
        });
    }

    private void addGlobalRegions(FfMap map, MapTemplate template) {
        BlockBounds sheepSpawn = template.getFirstRegion("sheep_spawn");
        map.setCenterSpawn(sheepSpawn);
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
