package net.gegy1000.farmyfeud.game.active;

import net.gegy1000.farmyfeud.FarmyFeud;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FfScoreboard implements AutoCloseable {
    private final FfActive game;

    private final Map<GameTeam, Team> scoreboardTeams = new HashMap<>();
    private final ScoreboardObjective objective;

    private FfScoreboard(FfActive game, ScoreboardObjective objective) {
        this.game = game;
        this.objective = objective;
    }

    public static FfScoreboard create(FfActive game) {
        ServerWorld world = game.gameWorld.getWorld();
        MinecraftServer server = world.getServer();

        ServerScoreboard scoreboard = server.getScoreboard();

        ScoreboardObjective objective = new ScoreboardObjective(
                scoreboard, FarmyFeud.ID,
                ScoreboardCriterion.DUMMY,
                new LiteralText("Farmy Feud").formatted(Formatting.GOLD, Formatting.BOLD),
                ScoreboardCriterion.RenderType.INTEGER
        );
        scoreboard.addScoreboardObjective(objective);

        scoreboard.setObjectiveSlot(1, objective);

        return new FfScoreboard(game, objective);
    }

    public void tick() {
        ServerWorld world = this.game.gameWorld.getWorld();
        long time = world.getTime();

        if (time % 20 == 0) {
            this.rerender(time);
        }
    }

    public void addPlayer(ServerPlayerEntity player, GameTeam team) {
        ServerWorld world = this.game.gameWorld.getWorld();
        MinecraftServer server = world.getServer();

        ServerScoreboard scoreboard = server.getScoreboard();
        scoreboard.addPlayerToTeam(player.getEntityName(), this.scoreboardTeam(team));
    }

    public Team scoreboardTeam(GameTeam team) {
        return this.scoreboardTeams.computeIfAbsent(team, t -> {
            ServerWorld world = this.game.gameWorld.getWorld();
            MinecraftServer server = world.getServer();
            ServerScoreboard scoreboard = server.getScoreboard();
            String teamKey = t.getDisplay();
            Team scoreboardTeam = scoreboard.getTeam(teamKey);
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.addTeam(teamKey);
                scoreboardTeam.setColor(team.getFormatting());
                scoreboardTeam.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
                scoreboardTeam.setFriendlyFireAllowed(false);
            }
            return scoreboardTeam;
        });
    }

    private void rerender(long time) {
        List<String> lines = new ArrayList<>(10);

        long ticksRemaining = Math.max(this.game.endTime - time, 0);
        long sheepTicksRemaining = Math.max(this.game.nextSpawnTime - time, 0);

        lines.add(Formatting.RED.toString() + Formatting.BOLD + "Time left: " + Formatting.RESET + this.renderTime(ticksRemaining));
        lines.add("");

        lines.add(Formatting.BLUE + "Sheep in: " + Formatting.RESET + this.renderTime(sheepTicksRemaining));
        lines.add("");

        lines.add(Formatting.BOLD + "Teams:");
        this.game.teams().forEach(teamState -> {
            String nameFormat = teamState.team.getFormatting().toString() + Formatting.BOLD.toString();
            String descriptionFormat = Formatting.RESET.toString() + Formatting.GRAY.toString();

            String name = teamState.team.getDisplay();
            String description = teamState.getCapturedSheep() + " sheep";

            lines.add("  " + nameFormat + name + ": " + descriptionFormat + description);
        });

        this.render(lines.toArray(new String[0]));
    }

    private String renderTime(long ticks) {
        long seconds = (ticks / 20) % 60;
        long minutes = ticks / (20 * 60);

        return String.format("%02d:%02d", minutes, seconds);
    }

    private void render(String[] lines) {
        ServerWorld world = this.game.gameWorld.getWorld();
        MinecraftServer server = world.getServer();
        ServerScoreboard scoreboard = server.getScoreboard();

        render(scoreboard, this.objective, lines);
    }

    private static void render(ServerScoreboard scoreboard, ScoreboardObjective objective, String[] lines) {
        clear(scoreboard, objective);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            scoreboard.getPlayerScore(line, objective).setScore(lines.length - i);
        }
    }

    private static void clear(ServerScoreboard scoreboard, ScoreboardObjective objective) {
        Collection<ScoreboardPlayerScore> existing = scoreboard.getAllPlayerScores(objective);
        for (ScoreboardPlayerScore score : existing) {
            scoreboard.resetPlayerScore(score.getPlayerName(), objective);
        }
    }

    @Override
    public void close() {
        ServerWorld world = this.game.gameWorld.getWorld();
        MinecraftServer server = world.getServer();

        ServerScoreboard scoreboard = server.getScoreboard();
        this.scoreboardTeams.values().forEach(scoreboard::removeTeam);

        scoreboard.removeObjective(this.objective);
    }
}
