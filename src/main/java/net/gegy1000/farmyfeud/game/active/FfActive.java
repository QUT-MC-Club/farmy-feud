package net.gegy1000.farmyfeud.game.active;

import net.gegy1000.farmyfeud.FarmyFeud;
import net.gegy1000.farmyfeud.entity.FarmSheepEntity;
import net.gegy1000.farmyfeud.game.FfConfig;
import net.gegy1000.farmyfeud.game.FfSpawnLogic;
import net.gegy1000.farmyfeud.game.map.FfMap;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.event.GameCloseListener;
import net.gegy1000.plasmid.game.event.GameOpenListener;
import net.gegy1000.plasmid.game.event.GameTickListener;
import net.gegy1000.plasmid.game.event.HandSwingListener;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDamageListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.UseItemListener;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.gegy1000.plasmid.game.player.JoinResult;
import net.gegy1000.plasmid.game.rule.GameRule;
import net.gegy1000.plasmid.game.rule.RuleResult;
import net.gegy1000.plasmid.util.BlockBounds;
import net.gegy1000.plasmid.util.ColoredBlocks;
import net.gegy1000.plasmid.util.ItemStackBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class FfActive {
    public static final long RESPAWN_SECONDS = 5;
    public static final long RESPAWN_TICKS = RESPAWN_SECONDS * 20;

    private static final ItemStackBuilder SWORD = ItemStackBuilder.of(Items.WOODEN_SWORD)
            .setUnbreakable();

    private static final ItemStackBuilder AXE = ItemStackBuilder.of(Items.WOODEN_AXE)
            .setUnbreakable();

    private static final ItemStackBuilder BOW = ItemStackBuilder.of(Items.BOW)
            .setUnbreakable();

    private static final ItemStackBuilder SADDLE = ItemStackBuilder.of(Items.SADDLE)
            .addLore(new LiteralText("Right-click on a sheep to pick it up!"));

    public final ServerWorld world;
    public final GameWorld gameWorld;
    public final FfConfig config;

    public final FfMap map;
    public final FfSpawnLogic spawnLogic;
    public final FfCaptureLogic captureLogic;
    public final FfScoreboard scoreboard;

    private final FfTimerBar timerBar = new FfTimerBar();

    private final Map<GameTeam, FfTeamState> teams = new HashMap<>();
    private final Map<UUID, FfParticipant> participants = new HashMap<>();

    private long nextArrowTime;
    public long nextSpawnTime;

    public long endTime;

    private long closeTime = -1;

    private FfActive(GameWorld gameWorld, FfMap map, FfConfig config) {
        this.world = gameWorld.getWorld();
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;

        this.spawnLogic = new FfSpawnLogic(this.world, this.map);
        this.captureLogic = new FfCaptureLogic(this);

        this.scoreboard = FfScoreboard.create(this);

        for (GameTeam team : config.teams) {
            this.teams.put(team, new FfTeamState(team));
        }
    }

    public static void open(GameWorld gameWorld, FfMap map, FfConfig config) {
        FfActive active = new FfActive(gameWorld, map, config);

        gameWorld.newGame(game -> {
            game.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
            game.setRule(GameRule.ALLOW_PVP, RuleResult.ALLOW);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::open);
            game.on(GameCloseListener.EVENT, active::close);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);

            game.on(UseItemListener.EVENT, active::onUseItem);
            game.on(HandSwingListener.EVENT, active::onSwingHand);
        });
    }

    private void open() {
        long time = this.world.getTime();

        this.endTime = time + this.config.gameDuration;
        this.initNextSpawnTime(time);
        this.initNextArrowTime(time);

        int sheepCount = (this.config.teams.size() * 3) / 2;
        for (int i = 0; i < sheepCount; i++) {
            this.spawnSheep();
        }
    }

    private void close() {
        this.scoreboard.close();
        this.timerBar.close();

        for (FfParticipant participant : this.participants.values()) {
            ServerPlayerEntity player = participant.player();
            if (player != null) {
                participant.carryStack.dropAll(player);
            }
        }
    }

    private void addPlayer(ServerPlayerEntity player) {
        FfParticipant participant = this.getOrCreateParticipant(player);
        this.spawnParticipant(player, participant);

        this.timerBar.addPlayer(player);
    }

    private void tick() {
        long time = this.world.getTime();

        if (this.closeTime != -1) {
            if (time >= this.closeTime) {
                this.gameWorld.closeWorld();
            }
            return;
        }

        this.scoreboard.tick();

        WinResult winResult = this.tickActive(time);
        if (winResult.isWin()) {
            this.broadcastWinResult(winResult);
            this.closeTime = time + 20 * 5;
        }
    }

    private WinResult tickActive(long time) {
        this.timerBar.update(this.endTime - time, this.config.gameDuration);

        if (time >= this.endTime) {
            return this.computeWinResult();
        }

        this.tickSheepSpawning(time);

        for (FfParticipant participant : this.participants.values()) {
            ServerPlayerEntity player = participant.player();
            if (player == null) {
                continue;
            }

            this.tickParticipant(time, player, participant);
        }

        this.tickArrows(time);

        return WinResult.no();
    }

    private void tickParticipant(long time, ServerPlayerEntity player, FfParticipant participant) {
        if (participant.tryRespawn(time)) {
            this.spawnParticipant(player, participant);
        }

        this.tickSheepStack(player, participant);
    }

    private WinResult computeWinResult() {
        FfTeamState winningTeam = null;

        for (FfTeamState team : this.teams.values()) {
            if (winningTeam == null) {
                winningTeam = team;
                continue;
            }

            // two teams have the same sheep count!
            if (team.getCapturedSheep() == winningTeam.getCapturedSheep()) {
                return WinResult.draw();
            }

            if (team.getCapturedSheep() > winningTeam.getCapturedSheep()) {
                winningTeam = team;
            }
        }

        if (winningTeam == null) {
            return WinResult.draw();
        }

        return WinResult.win(winningTeam.team);
    }

    private void tickArrows(long time) {
        if (time >= this.nextArrowTime) {
            this.players().forEach(player -> {
                if (player.inventory.count(Items.ARROW) < this.config.maxArrows) {
                    player.inventory.offerOrDrop(this.world, new ItemStack(Items.ARROW));
                }
            });

            this.initNextArrowTime(time);
        }
    }

    private void tickSheepSpawning(long time) {
        if (time >= this.nextSpawnTime) {
            this.spawnSheep();
            this.initNextSpawnTime(time);
        }
    }

    private void initNextSpawnTime(long currentTime) {
        this.nextSpawnTime = currentTime + this.config.spawnInterval;
    }

    private void initNextArrowTime(long currentTime) {
        this.nextArrowTime = currentTime + this.config.arrowInterval;
    }

    private void spawnSheep() {
        BlockBounds centerSpawn = this.map.getCenterSpawn();
        if (centerSpawn == null) {
            return;
        }

        FarmSheepEntity entity = new FarmSheepEntity(this.world, this);

        Vec3d spawnPos = centerSpawn.getCenter();
        entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y + 0.5, spawnPos.z, 0.0F, 0.0F);

        entity.setOwnerTeam(null, centerSpawn);
        entity.setInvulnerable(true);

        if (!this.world.spawnEntity(entity)) {
            FarmyFeud.LOGGER.warn("Chunk not loaded to spawn sheep entity!");
        }
    }

    private void onSwingHand(ServerPlayerEntity player, Hand hand) {
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem() == Items.SADDLE) {
            FfParticipant participant = this.getParticipant(player);
            if (participant != null) {
                List<FarmSheepEntity> entities = participant.carryStack.dropAll(player);
                this.throwEntities(player, entities, 1.0);
            }
        }
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem() == Items.SADDLE) {
            FfParticipant participant = this.getParticipant(player);
            if (participant != null) {
                EntityHitResult traceResult = EntityRayTrace.rayTrace(player, 5.0, 0.5, entity -> {
                    return entity instanceof FarmSheepEntity && !entity.hasVehicle() && entity.collides();
                });

                if (traceResult != null) {
                    this.tryPickUpSheep(player, participant, (FarmSheepEntity) traceResult.getEntity());
                    return TypedActionResult.consume(heldStack);
                }
            }
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    public boolean tryPickUpSheep(ServerPlayerEntity player, FfParticipant participant, FarmSheepEntity sheep) {
        GameTeam currentTeam = this.captureLogic.getTeamAt(sheep.getPos());
        if (currentTeam == participant.team) {
            return false;
        }

        return participant.carryStack.tryAdd(player, sheep);
    }

    public void tickSheep(FarmSheepEntity sheep) {
        if (sheep.hasVehicle()) {
            return;
        }

        if (this.world.getTime() % 20 == 0) {
            BlockPos sheepPos = sheep.getBlockPos();

            for (BlockBounds region : this.map.getIllegalSheepRegions()) {
                if (region.contains(sheepPos)) {
                    BlockBounds home = sheep.getHome();
                    if (home == null) {
                        home = this.map.getCenterSpawn();
                    }

                    Vec3d spawn = home.getCenter();

                    sheep.detach();
                    sheep.teleport(spawn.x, spawn.y, spawn.z);
                }
            }
        }
    }

    private void tickSheepStack(ServerPlayerEntity player, FfParticipant participant) {
        if (participant.carryStack.isEmpty()) {
            return;
        }

        GameTeam captureTeam = this.captureLogic.getTeamAt(player.getPos());
        if (captureTeam == participant.team) {
            List<FarmSheepEntity> entities = participant.carryStack.dropAll(player);
            this.throwEntities(player, entities, 0.5);

            for (FarmSheepEntity sheep : entities) {
                this.captureLogic.captureSheep(sheep, captureTeam);
            }
        }
    }

    private void throwEntities(ServerPlayerEntity player, Collection<? extends Entity> entities, double strength) {
        Vec3d rotation = player.getRotationVec(1.0F);
        for (Entity entity : entities) {
            entity.setVelocity(rotation.multiply(strength));
        }
    }

    private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        MutableText message;

        Entity attackerEntity = source.getAttacker();
        if (attackerEntity instanceof ServerPlayerEntity) {
            message = player.getDisplayName().shallowCopy().append(" was killed by ").append(attackerEntity.getDisplayName());
        } else {
            message = player.getDisplayName().shallowCopy().append(" died");
        }

        this.broadcastMessage(message.formatted(Formatting.GRAY));

        this.respawnPlayer(player);
        return true;
    }

    private boolean onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (amount < 1.0F) {
            return false;
        }

        FfParticipant participant = this.getParticipant(player);
        if (participant != null) {
            FfParticipant attackerParticipant = this.getParticipant(source.getAttacker());
            if (attackerParticipant != null && attackerParticipant.team == participant.team) {
                return true;
            }

            participant.carryStack.dropAll(player);
        }

        if (!player.isSpectator() && source == DamageSource.LAVA) {
            this.respawnPlayer(player);
            return true;
        }

        return false;
    }

    private void respawnPlayer(ServerPlayerEntity player) {
        FfParticipant participant = this.getParticipant(player);
        if (participant != null) {
            participant.startRespawn(this.world.getTime());
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);

            player.sendMessage(new LiteralText("You will respawn in " + RESPAWN_SECONDS + " seconds..").formatted(Formatting.BOLD), false);
        }
    }

    private void spawnParticipant(ServerPlayerEntity player, FfParticipant participant) {
        GameTeam team = participant.team;

        this.spawnLogic.spawnPlayerAtTeam(player, team);
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        player.inventory.insertStack(0, SWORD.build());
        player.inventory.insertStack(1, AXE.build());
        player.inventory.insertStack(2, BOW.build());
        player.inventory.insertStack(4, SADDLE.build());

        player.inventory.insertStack(8,
                ItemStackBuilder.of(ColoredBlocks.wool(team.getDye()))
                        .setName(new LiteralText(team.getDisplay() + " Team")
                                .formatted(team.getFormatting())
                        )
                        .build()
        );

        int color = team.getColor();
        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setUnbreakable().setColor(color).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setUnbreakable().setColor(color).build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(color).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setUnbreakable().setColor(color).build());
    }

    private void broadcastWinResult(WinResult result) {
        GameTeam winningTeam = result.getWinningTeam();

        if (winningTeam != null) {
            Text message = new LiteralText(winningTeam.getDisplay() + " won the game!")
                    .formatted(Formatting.BOLD, winningTeam.getFormatting());
            this.broadcastMessage(message);
        } else {
            Text message = new LiteralText("The game ended in a draw!")
                    .formatted(Formatting.BOLD, Formatting.GRAY);
            this.broadcastMessage(message);
        }
    }
    // TODO: extract common broadcast utils into plasmid

    private void broadcastMessage(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            player.sendMessage(message, false);
        }
    }

    private void broadcastActionBar(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            player.sendMessage(message, true);
        }
    }

    private void broadcastSound(SoundEvent sound) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    public Stream<FfParticipant> participants() {
        return this.participants.values().stream();
    }

    public Stream<UUID> participantsFor(GameTeam team) {
        return this.teams.get(team).participants();
    }

    public Stream<ServerPlayerEntity> players() {
        return this.playersBy(this.participants().map(participant -> participant.playerId));
    }

    public Stream<ServerPlayerEntity> playersFor(GameTeam team) {
        return this.playersBy(this.participantsFor(team));
    }

    private Stream<ServerPlayerEntity> playersBy(Stream<UUID> participants) {
        ServerWorld world = this.gameWorld.getWorld();
        return participants
                .map(id -> (ServerPlayerEntity) world.getPlayerByUuid(id))
                .filter(Objects::nonNull);
    }

    public Stream<FfTeamState> teams() {
        return this.teams.values().stream();
    }

    public FfTeamState getTeam(GameTeam team) {
        return this.teams.get(team);
    }

    private FfParticipant getOrCreateParticipant(ServerPlayerEntity player) {
        return this.participants.computeIfAbsent(player.getUuid(), uuid -> {
            FfTeamState teamState = this.getSmallestTeam();

            teamState.addParticipant(uuid);
            this.scoreboard.addPlayer(player, teamState.team);

            return new FfParticipant(player, teamState.team);
        });
    }

    @Nullable
    public FfParticipant getParticipant(Entity entity) {
        if (entity == null) {
            return null;
        }
        return this.participants.get(entity.getUuid());
    }

    private FfTeamState getSmallestTeam() {
        List<FfTeamState> teams = new ArrayList<>(this.teams.values());
        Collections.shuffle(teams);

        return teams.stream()
                .min(Comparator.comparingInt(FfTeamState::getParticipantCount))
                .orElseThrow(() -> new IllegalStateException("no teams present!"));
    }

    private static class WinResult {
        private final GameTeam winningTeam;
        private final boolean win;

        private WinResult(GameTeam winningTeam, boolean win) {
            this.winningTeam = winningTeam;
            this.win = win;
        }

        public static WinResult win(GameTeam winningTeam) {
            return new WinResult(winningTeam, true);
        }

        public static WinResult draw() {
            return new WinResult(null, true);
        }

        public static WinResult no() {
            return new WinResult(null, false);
        }

        @Nullable
        public GameTeam getWinningTeam() {
            return this.winningTeam;
        }

        public boolean isWin() {
            return this.win;
        }
    }
}
