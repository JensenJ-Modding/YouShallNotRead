package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;
import net.youshallnotread.Utils;
import org.apache.commons.collections4.collection.CompositeCollection;

public class Outliner {

    public static float RENDER_DELTA;

    static final Map<String, Outline> OUTLINES = new HashMap<>();
    static final Map<UUID, Set<Outline>> SUSPENDED_OUTLINES = new HashMap<>();

    private static boolean isBatchedListDirty = false;

    public static void processOutlines(Level level, PoseStack stack) {
        // YouShallNotRead.LOGGER.info("Rendering {} outlines", OUTLINES.size());
        prepareOutlines();
        ResourceKey<Level> dimension = level.dimension();
        renderBatchedOutlines(dimension, stack);
        renderStandaloneOutlines(dimension, stack);
    }

    public static void prepareOutlines() {
        Set<Outline> outlinesToRemove = new HashSet<>();
        Set<Outline> outlinesToSuspend = new HashSet<>();
        Set<Outline> outlinesToRegenerate = new HashSet<>();
        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            Outline outline = entry.getValue();
            Function<Outline, Boolean> removeIf = outline.removeIfCallback();
            if (hasOutlineDurationExpired(outline) || (removeIf != null && removeIf.apply(outline))) {
                outlinesToRemove.add(outline);
                if (outline instanceof BatchedOutline) {
                    isBatchedListDirty = true;
                }
            }

            if (outline.type() == Outline.Type.ENTITY) {
                Entity entity = outline.entity();

                if (Utils.shouldRemoveOutline(entity)) {
                    outlinesToRemove.add(entry.getValue());
                    continue;
                }

                if (Utils.shouldSuspendOutline(entity) && !outlinesToRemove.contains(outline)) {
                    outlinesToSuspend.add(entry.getValue());
                    continue;
                }
            }

            Function<Outline, Boolean> regenIf = outline.regenerateIfCallback();
            if (regenIf != null && regenIf.apply(outline)) {
                outlinesToRegenerate.add(outline);
                continue;
            }

            if (!isBatchedListDirty) continue;
            if (!(outline instanceof MergedOutline mergedOutline)) continue;
            if (!mergedOutline.dirty()) continue;

            if (mergedOutline.hasMergedOutlinesChanged()) {
                outlinesToRegenerate.add(mergedOutline);
            }
        }

        outlinesToRemove.forEach(Outliner::removeOutline);
        outlinesToSuspend.forEach(Outliner::suspendOutline);
        outlinesToRegenerate.forEach(outline -> {
            outline.cleanup();
            outline.setupVertexData();
        });
    }

    public static boolean hasOutlineDurationExpired(Outline outline) {
        if (!(outline.duration() < 0)) {
            return LocalDateTime.now().isAfter(outline.createdTimestamp().plusSeconds((long) outline.duration()));
        }
        return false;
    }

    public static void renderBatchedOutlines(ResourceKey<Level> dimension, PoseStack stack) {
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

        if (isBatchedListDirty) {
            CompositeCollection<BatchedVertexBuffer.OutlineVertex> batchedVertices = new CompositeCollection<>();
            for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
                if (!(entry.getValue() instanceof BatchedOutline outline)) continue;
                if (!outline.dimension().equals(dimension)) continue;

                // if(outline instanceof MergedOutline mergedOutline){
                //    if(mergedOutline.dirty()){
                //        mergedOutline.cleanup();
                //        mergedOutline.setupVertexData();
                //    }
                // }

                // TODO: Only add if this outline should render
                batchedVertices.addComposited(outline.vertices());
            }
            BatchedVertexBuffer.cleanup();
            BatchedVertexBuffer.populateVertexBuffer(batchedVertices);
            isBatchedListDirty = false;
        }
        if (BatchedVertexBuffer.hasVertexData()) {
            OutlineRenderer.renderBatchedOutlines(stack);
        }
    }

    public static void renderStandaloneOutlines(ResourceKey<Level> dimension, PoseStack stack) {
        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            if (!(entry.getValue() instanceof StandaloneOutline outline)) continue;
            if (!outline.dimension().equals(dimension)) continue;
            OutlineRenderer.renderOutline(outline, stack);
        }
    }

    public static void addOutline(Outline outline) {
        if (outline instanceof MergedOutline mergedOutline) {
            mergedOutline.markDirty();
        }

        if (outline instanceof BatchedOutline) {
            refreshOutlines();
        }

        if (!outline.hasVertexData()) {
            outline.setupVertexData();
        }

        if (outline.type() == Outline.Type.ENTITY) {
            Set<Outline> outlines =
                    SUSPENDED_OUTLINES.getOrDefault(outline.entity().getUUID(), new HashSet<>());

            Consumer<Outline> consumer = outline.onUnsuspendCallback();
            if (consumer != null && outlines.contains(outline)) consumer.accept(outline);

            outlines.remove(outline);
            SUSPENDED_OUTLINES.put(outline.entity().getUUID(), outlines);
        }

        // FIXME: Currently if an outline is suspended, the changed callback does not trigger.
        //  Also, in the event an outline is added which has a key within the suspended outlines list,
        //  the outline within suspended outlines is not removed, meaning upon unsuspension, it can overwrite a newer
        // outline.
        if (OUTLINES.containsKey(outline.key())) {
            Outline oldOutline = OUTLINES.get(outline.key());
            BiConsumer<Outline, Outline> consumer = oldOutline.onChangedCallback();
            if (consumer != null) consumer.accept(oldOutline, outline);
        }

        OUTLINES.put(outline.key(), outline);
    }

    public static void removeOutline(Outline outline) {
        Consumer<Outline> consumer = outline.onRemoveCallback();
        if (consumer != null) consumer.accept(outline);
        removeOutline(outline.key());
    }

    public static void suspendOutline(Outline outline) {
        Consumer<Outline> consumer = outline.onSuspendCallback();
        if (consumer != null) consumer.accept(outline);
        suspendOutline(outline.key());
    }

    public static void removeOutline(String key) {
        Outline existingOutline = OUTLINES.getOrDefault(key, null);
        if (existingOutline != null) {
            if (existingOutline.type() == Outline.Type.ENTITY) {
                if (!SUSPENDED_OUTLINES.containsKey(existingOutline.entity().getUUID())) {
                    existingOutline.cleanup();
                }
            }
            if (existingOutline instanceof BatchedOutline) {
                refreshOutlines();
            }

            if (existingOutline instanceof MergedOutline mergedOutline) {
                mergedOutline.markDirty();
                mergedOutline.remove();
            }
            OUTLINES.remove(key);
        }
    }

    public static void suspendOutline(String key) {
        Outline existingOutline = OUTLINES.getOrDefault(key, null);
        if (existingOutline != null) {
            if (existingOutline.type() == Outline.Type.ENTITY) {
                Set<Outline> outlines =
                        SUSPENDED_OUTLINES.getOrDefault(existingOutline.entity().getUUID(), new HashSet<>());
                outlines.add(existingOutline);
                SUSPENDED_OUTLINES.put(existingOutline.entity().getUUID(), outlines);
            }
        }
        removeOutline(key);
    }

    public static void removeAllOutlines() {
        for (Map.Entry<String, Outline> entry : OUTLINES.entrySet()) {
            entry.getValue().cleanup();
        }
        OUTLINES.clear();
        SUSPENDED_OUTLINES.clear();
        refreshOutlines();
        BatchedVertexBuffer.cleanup();
    }

    public static boolean outlineExists(String key) {
        return OUTLINES.containsKey(key);
    }

    public static boolean outlineExists(Outline outlineA) {
        boolean exists = outlineExists(outlineA.key());
        if (!exists) return false;

        Outline outlineB = getOutline(outlineA.key());

        if (outlineA.type() != outlineB.type()) return false;

        switch (outlineA.type()) {
            case ENTITY -> {
                return outlineA.entity().getUUID() == outlineB.entity().getUUID();
            }
            case BLOCK -> {
                return outlineA.blockPosCollection().equals(outlineB.blockPosCollection())
                        && outlineA.dimension().equals(outlineB.dimension());
            }
            case LINE -> {
                return outlineA.line().equals(outlineB.line())
                        && outlineA.dimension().equals(outlineB.dimension());
            }
        }

        return false;
    }

    public static Set<Outline> getSuspendedOutlines(Entity entity) {
        return SUSPENDED_OUTLINES.getOrDefault(entity.getUUID(), null);
    }

    public static Outline getOutline(String key) {
        return OUTLINES.getOrDefault(key, null);
    }

    public static void refreshOutlines() {
        isBatchedListDirty = true;
    }
}
