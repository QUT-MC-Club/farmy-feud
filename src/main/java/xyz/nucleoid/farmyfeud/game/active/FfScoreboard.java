package xyz.nucleoid.farmyfeud.game.active;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.HashMap;
import java.util.Map;

public final class FfScoreboard implements AutoCloseable {
    private final FfActive game;

    private final SidebarWidget sidebar;
    private final Map<GameTeam, Team> scoreboardTeams = new HashMap<>();

    private FfScoreboard(FfActive game, SidebarWidget sidebar) {
        this.game = game;
        this.sidebar = sidebar;
    }

    public static FfScoreboard create(FfActive game, GlobalWidgets widgets) {
        Text title = new LiteralText("Farmy Feud").formatted(Formatting.GOLD, Formatting.BOLD);
        SidebarWidget sidebar = widgets.addSidebar(title);
        return new FfScoreboard(game, sidebar);
    }

    public void tick() {
        ServerWorld world = this.game.gameSpace.getWorld();
        long time = world.getTime();

        if (time % 20 == 0) {
            this.rerender(time);
        }
    }

    public void addPlayer(ServerPlayerEntity player, GameTeam team) {
        ServerWorld world = this.game.gameSpace.getWorld();
        MinecraftServer server = world.getServer();

        ServerScoreboard scoreboard = server.getScoreboard();
        scoreboard.addPlayerToTeam(player.getEntityName(), this.scoreboardTeam(team));
    }

    public Team scoreboardTeam(GameTeam team) {
        return this.scoreboardTeams.computeIfAbsent(team, t -> {
            ServerWorld world = this.game.gameSpace.getWorld();
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
        this.sidebar.set(content -> {
            long ticksRemaining = Math.max(this.game.endTime - time, 0);
            long sheepTicksRemaining = Math.max(this.game.nextSpawnTime - time, 0);

            content.writeLine(Formatting.RED.toString() + Formatting.BOLD + "Time left: " + Formatting.RESET + this.renderTime(ticksRemaining));
            content.writeLine("");

            content.writeLine(Formatting.BLUE + "Sheep in: " + Formatting.RESET + this.renderTime(sheepTicksRemaining));
            content.writeLine("");

            content.writeLine(Formatting.BOLD + "Teams:");
            this.game.teams().forEach(teamState -> {
                String nameFormat = teamState.team.getFormatting().toString() + Formatting.BOLD.toString();
                String descriptionFormat = Formatting.RESET.toString() + Formatting.GRAY.toString();

                String name = teamState.team.getDisplay();
                String description = teamState.getCapturedSheep() + " sheep";

                content.writeLine("  " + nameFormat + name + ": " + descriptionFormat + description);
            });
        });
    }

    private String renderTime(long ticks) {
        long seconds = (ticks / 20) % 60;
        long minutes = ticks / (20 * 60);

        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void close() {
        ServerWorld world = this.game.gameSpace.getWorld();
        MinecraftServer server = world.getServer();

        ServerScoreboard scoreboard = server.getScoreboard();
        this.scoreboardTeams.values().forEach(scoreboard::removeTeam);

        this.sidebar.close();
    }
}
