package net.gegy1000.farmyfeud.entity;

import net.gegy1000.farmyfeud.game.active.FfActive;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.gegy1000.plasmid.util.BlockBounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

public final class FarmSheepEntity extends SheepEntity {
    private final FfActive game;
    private GameTeam ownerTeam;
    private BlockBounds home;

    public FarmSheepEntity(World world, FfActive game) {
        super(EntityType.SHEEP, world);
        this.game = game;
    }

    public void setOwnerTeam(@Nullable GameTeam team, BlockBounds home) {
        this.ownerTeam = team;
        this.home = home;

        this.getNavigation().stop();

        if (team != null) {
            this.setCustomName(null);
            this.setColor(team.getDye());
        } else {
            this.setCustomName(new LiteralText("jeb_"));
            this.setCustomNameVisible(false);
            this.setColor(DyeColor.WHITE);
        }
    }

    @Nullable
    public BlockBounds getHome() {
        return this.home;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WanderToHomeGoal());
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    protected void mobTick() {
        this.game.tickSheep(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity vehicle = this.getVehicle();
        if (vehicle != null && source.getSource() != null) {
            return vehicle.damage(source, amount);
        }

        return super.damage(source, amount);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    @Nullable
    public GameTeam getOwnerTeam() {
        return this.ownerTeam;
    }

    private class WanderToHomeGoal extends Goal {
        public WanderToHomeGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return FarmSheepEntity.this.home != null && FarmSheepEntity.this.random.nextInt(20) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return !FarmSheepEntity.this.getNavigation().isIdle();
        }

        @Override
        public void start() {
            Vec3d target = this.getWanderTarget();
            FarmSheepEntity.this.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 0.8);
        }

        private Vec3d getWanderTarget() {
            BlockBounds home = FarmSheepEntity.this.home;
            BlockPos homeMin = home.getMin();
            BlockPos homeMax = home.getMax();

            Random random = FarmSheepEntity.this.random;

            double x = FarmSheepEntity.this.getX();
            double z = FarmSheepEntity.this.getZ();

            int targetX = random.nextInt(homeMax.getX() - homeMin.getX() + 1) + homeMin.getX();
            int targetZ = random.nextInt(homeMax.getZ() - homeMin.getZ() + 1) + homeMin.getZ();

            double deltaX = x - targetX;
            double deltaZ = z - targetZ;

            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance > 16.0) {
                deltaX = deltaX / distance * 16.0;
                deltaZ = deltaZ / distance * 16.0;

                targetX = MathHelper.floor(x + deltaX);
                targetZ = MathHelper.floor(z + deltaZ);
            }

            int topY = FarmSheepEntity.this.world.getTopY(Heightmap.Type.MOTION_BLOCKING, targetX, targetZ);
            return new Vec3d(targetX + 0.5, topY, targetZ + 0.5);
        }

        @Override
        public void stop() {
            FarmSheepEntity.this.getNavigation().stop();
        }
    }
}
