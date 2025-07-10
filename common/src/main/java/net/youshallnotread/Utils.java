package net.youshallnotread;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Utils {

    public static <T> Set<T> findSymmetricDifference(Set<T> set1, Set<T> set2) {
        Map<T, Integer> map = new HashMap<>();
        set1.forEach(e -> putKey(map, e));
        set2.forEach(e -> putKey(map, e));
        return map.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static <T> void putKey(Map<T, Integer> map, T key) {
        if (map.containsKey(key)) {
            map.replace(key, Integer.MAX_VALUE);
        } else {
            map.put(key, 1);
        }
    }

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
}
