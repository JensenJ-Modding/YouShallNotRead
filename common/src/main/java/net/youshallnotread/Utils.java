package net.youshallnotread;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Vector4f;

public class Utils {

    public static boolean shouldRemoveOutline(Entity entity) {
        if (entity == null) return true;
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.equals(Entity.RemovalReason.KILLED)) return true;
        if (entity instanceof LivingEntity living) {
            return living.isDeadOrDying();
        }
        return false;
    }

    public static boolean shouldSuspendOutline(Entity entity) {
        return entity.getRemovalReason() != null;
    }

    public static Vector4f RBGFromInt(int value) {
        float r = ((value >> 16) & 0xFF) / 255f;
        float g = ((value >> 8) & 0xFF) / 255f;
        float b = (value & 0xFF) / 255f;
        return new Vector4f(r, g, b, 1.0f);
    }
}
