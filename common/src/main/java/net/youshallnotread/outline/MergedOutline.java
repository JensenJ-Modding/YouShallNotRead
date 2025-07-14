package net.youshallnotread.outline;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import net.youshallnotread.Utils;
import org.joml.Vector3f;

public class MergedOutline extends BatchedOutline {

    private final Supplier<Boolean> showCollisions;
    private final Supplier<Vector3f> collisionColour;
    private final Supplier<Float> collisionThickness;
    private final String mergeKey;
    private final AABB collisionBounds;
    private Set<MergedOutline> cachedOverlappingOutlines = new HashSet<>();
    private static final Set<String> dirtyOutlines = new HashSet<>();

    public MergedOutline(Builder builder) {
        super(builder);
        this.showCollisions = builder.showCollisions;
        this.mergeKey = builder.mergeKey;
        this.collisionColour = builder.collisionColour;
        this.collisionThickness = builder.collisionThickness;

        if (this.type() == Type.BLOCK) {
            BlockPos minPos = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            BlockPos maxPos = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            for (BlockPos pos : this.blockPosCollection()) {
                maxPos = BlockPos.max(pos, maxPos);
                minPos = BlockPos.min(pos, minPos);
            }
            this.collisionBounds = AABB.encapsulatingFullBlocks(minPos, maxPos);
        } else {
            this.collisionBounds = null;
        }
    }

    @Override
    void setupVertexData() {
        super.setupVertexData();
    }

    public boolean hasMergedOutlinesChanged() {
        Set<MergedOutline> newCollidingOutlines = new HashSet<>();
        for (Map.Entry<String, Outline> entry : Outliner.OUTLINES.entrySet()) {
            if (!(entry.getValue() instanceof MergedOutline outline)) continue;
            if (outline == this) continue;
            if (!this.dimension().equals(outline.dimension())) continue;

            if (this.collisionBounds().inflate(1).intersects(outline.collisionBounds())) {
                if (this.mergeKey().equals(outline.mergeKey())) {
                    newCollidingOutlines.add(outline);
                }
            }
        }

        boolean isEqual = newCollidingOutlines.equals(cachedOverlappingOutlines);
        cachedOverlappingOutlines = newCollidingOutlines;
        dirtyOutlines.remove(this.key());
        return !isEqual;
    }

    public Set<MergedOutline> collidingOutlines() {
        return cachedOverlappingOutlines;
    }

    public String mergeKey() {
        return mergeKey;
    }

    public boolean showCollisions() {
        if (showCollisions == null) {
            return false;
        }
        return showCollisions.get();
    }

    public AABB collisionBounds() {
        return collisionBounds;
    }

    public Vector3f collisionColour() {
        if (collisionColour == null) {
            return new Vector3f(Utils.RBGFromInt(0xAA0000));
        }
        return collisionColour.get();
    }

    public float collisionThickness() {
        if (collisionThickness == null) {
            return 0.05f;
        }
        return collisionThickness.get();
    }

    public void markDirty() {
        dirtyOutlines.add(this.key());
        for (MergedOutline outline : cachedOverlappingOutlines) {
            if (dirtyOutlines.contains(outline.key())) continue;
            outline.markDirty();
        }
    }

    public boolean dirty() {
        return dirtyOutlines.contains(this.key());
    }

    public void remove() {
        dirtyOutlines.remove(this.key());
        cachedOverlappingOutlines.clear();
    }
}
