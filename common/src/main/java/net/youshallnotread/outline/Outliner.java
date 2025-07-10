package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.*;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import net.youshallnotread.Utils;
import net.youshallnotread.YouShallNotRead;

public class Outliner {

    static final Map<String, Outline> OUTLINES = new HashMap<>();
    private static boolean isBlockListDirty = false;

    public static void processOutlines(Level level) {
        YouShallNotRead.LOGGER.info(OUTLINES);

        List<String> outlinesToDestroy = getOutlinesToDestroy();
        calculateMergedOutlines();
        outlinesToDestroy.forEach(OUTLINES::remove);

        ResourceKey<Level> dimension = level.dimension();
        renderBlockOutlines(dimension);
        renderEntityOutlines(dimension);
    }

    public static List<String> getOutlinesToDestroy() {
        List<String> outlinesToRemove = new ArrayList<>();
        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            Outline outline = entry.getValue();
            if (!(outline.duration() < 0)) {
                if (LocalDateTime.now().isAfter(outline.createdTimestamp().plusSeconds((long) outline.duration()))) {
                    outlinesToRemove.add(entry.getKey());
                    isBlockListDirty = true;
                }
            }

            if (outline.type() == Outline.Type.ENTITY) {
                Entity entity = outline.entity();
                if (!entity.isAlive()) {
                    outlinesToRemove.add(entry.getKey());
                }
            }
        }

        return outlinesToRemove;
    }

    public static void calculateMergedOutlines() {
        if (!isBlockListDirty) return;

        Set<MergedOutline> outlinesToRecalculate = new HashSet<>();
        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            if (!(entry.getValue() instanceof MergedOutline outline)) continue;
            if (!outline.dirty()) continue;

            Set<MergedOutline> newOverlappingOutlines = outline.calculateOverlappingOutlines();
            if (newOverlappingOutlines.equals(outline.cachedOverlappingOutlines())) continue;

            outlinesToRecalculate.addAll(
                    Utils.findSymmetricDifference(newOverlappingOutlines, outline.cachedOverlappingOutlines()));
            outline.updateCachedOverlappingOutlines(newOverlappingOutlines);
        }

        for (MergedOutline outline : outlinesToRecalculate) {
            outline.markDirty();
            outline.updateCachedOverlappingOutlines(outline.calculateOverlappingOutlines());
        }
    }

    public static void renderBlockOutlines(ResourceKey<Level> dimension) {

        // We need to experiment with many drawcalls vs this merging vertex lists into one buffer performance

        // If list is clean we can use the cached vertex buffer, then return
        //   We need to work out criteria for dirtying the list
        //     changing dimensions
        //     if an outline goes out of view
        //     is culled/unculled
        //     manually added / removed outlines

        // List is dirty
        // This bit might want to be done on a separate render thread if performance hits are bad when generating new
        // buffers
        //  Create a temp vertex buffer which we can swap with the loaded one upon generation
        //  For each block based outline
        //    If outline is dirty, we need to generate its vertices, including colliding verts if the outline is merged
        //    We can then mark this outline as clean
        //    Add this outlines verts to the buffer, we can use cached values if outline was clean
        //  Render the buffer, mark list as clean

        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            Outline outline = entry.getValue();
            if (!outline.dimension().equals(dimension)) continue;
            if (!(outline.type() == Outline.Type.BLOCK || outline.type() == Outline.Type.BLOCKGROUP)) continue;
        }
    }

    public static void renderEntityOutlines(ResourceKey<Level> dimension) {
        //  Each entity outline will need its own draw call to allow live transformations to be used
        // without affecting the rest of the outlines
        //  Mark the entity outline as clean

        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            Outline outline = entry.getValue();
            if (!outline.dimension().equals(dimension)) continue;
            if (!(outline.type() == Outline.Type.ENTITY)) continue;
        }
    }

    public static void addOutline(Outline outline) {
        outline.markDirty();
        OUTLINES.put(outline.key(), outline);
        if (outline.type().equals(Outline.Type.ENTITY)) {
            refreshOutlines();
        }
    }

    public static void removeOutline(Outline outline) {
        removeOutline(outline.key());
    }

    public static void removeOutline(String key) {
        Outline existingOutline = OUTLINES.getOrDefault(key, null);
        OUTLINES.remove(key);
        if (existingOutline != null) {
            if (existingOutline.type().equals(Outline.Type.ENTITY)) {
                refreshOutlines();
            }
        }
    }

    public static void removeAllOutlines() {
        OUTLINES.clear();
        refreshOutlines();
    }

    public static boolean outlineExists(String key) {
        return OUTLINES.containsKey(key);
    }

    public static boolean outlineExists(Outline outline) {
        return outlineExists(outline.key());
    }

    public static void refreshOutlines() {
        isBlockListDirty = true;
    }
}
