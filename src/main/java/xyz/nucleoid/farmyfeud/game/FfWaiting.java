package xyz.nucleoid.farmyfeud.game;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.farmyfeud.game.active.FfActive;
import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.farmyfeud.game.map.FfMapBuilder;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public final class FfWaiting {
    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final FfMap map;
    private final FfConfig config;

    private final FfSpawnLogic spawnLogic;
    private final TeamSelectionLobby teamSelection;

    private FfWaiting(GameSpace gameSpace, ServerWorld world, FfMap map, FfConfig config, TeamSelectionLobby teamSelection) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.teamSelection = teamSelection;

        this.spawnLogic = new FfSpawnLogic(this.world, map);
    }

    public static GameOpenProcedure open(GameOpenContext<FfConfig> context) {
        FfConfig config = context.config();

        FfMap map = new FfMapBuilder(config).create(context.server());

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.createGenerator(context.server()));

        return context.openWithWorld(worldConfig, (game, world) -> {
            var teamSelection = TeamSelectionLobby.addTo(game, config.teams());
            GameWaitingLobby.addTo(game, config.players());
            FfWaiting waiting = new FfWaiting(game.getGameSpace(), world, map, config, teamSelection);

            game.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            game.listen(GamePlayerEvents.ACCEPT, player -> player.teleport(world, map.getCenterSpawn() != null ? map.getCenterSpawn().center() : new Vec3d(0, 256, 0)));
            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.ADD, waiting::addPlayer);
            game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
        });
    }

    private GameResult requestStart() {
        FfActive.open(this.gameSpace, this.world, this.map, this.config, this.teamSelection);
        return GameResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.spawnPlayerAtCenter(player);
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
    }
}
