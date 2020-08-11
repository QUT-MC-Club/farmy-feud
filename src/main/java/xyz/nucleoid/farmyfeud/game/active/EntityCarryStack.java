package xyz.nucleoid.farmyfeud.game.active;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class EntityCarryStack<T extends Entity> {
    private static final UUID SLOW_MODIFIER_ID = UUID.fromString("19462f6a-7346-4e27-846f-68864be3ddcb");

    private final int maximumHeight;
    private final List<T> stack = new ArrayList<>();

    public EntityCarryStack(int maximumHeight) {
        this.maximumHeight = maximumHeight;
    }

    public boolean tryAdd(ServerPlayerEntity player, T entity) {
        if (this.stack.size() >= this.maximumHeight) {
            return false;
        }

        if (this.stack.contains(entity)) {
            return false;
        }

        Entity tail = player;
        if (!this.stack.isEmpty()) {
            tail = this.stack.get(this.stack.size() - 1);
        }

        entity.startRiding(tail, true);
        this.stack.add(entity);

        this.onStackChange(player);

        return true;
    }

    public List<T> dropAll(ServerPlayerEntity player) {
        if (this.stack.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> stack = new ArrayList<>(this.stack);
        this.stack.clear();

        for (T entity : stack) {
            entity.stopRiding();
        }

        this.onStackChange(player);

        return stack;
    }

    private void onStackChange(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(player));

        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(SLOW_MODIFIER_ID);

        if (!this.stack.isEmpty()) {
            double baseValue = attribute.getBaseValue();
            double targetValue = baseValue / (this.stack.size() * 0.3 + 1);

            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    SLOW_MODIFIER_ID, "carry slow",
                    targetValue - baseValue, EntityAttributeModifier.Operation.ADDITION
            );

            attribute.addTemporaryModifier(modifier);
        }
    }

    public int getHeight() {
        return this.stack.size();
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
}
