package net.youshallnotread.outline;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class MergedOutline extends Outline {

    private final boolean showCollisions;
    private final String mergeKey;
    private final AABB collisionBounds;
    private Set<MergedOutline> cachedOverlappingOutlines = new HashSet<>();
    private boolean dirty;

    public MergedOutline(Builder builder) {
        super(builder);
        this.showCollisions = builder.showCollisions;
        this.mergeKey = builder.mergeKey;

        switch (this.type()) {
            case BLOCK -> this.collisionBounds = AABB.encapsulatingFullBlocks(this.blockPos(), this.blockPos());
            case BLOCKGROUP -> {
                BlockPos minPos = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
                BlockPos maxPos = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
                for (BlockPos pos : this.blockPosCollection()) {
                    maxPos = BlockPos.max(pos, maxPos);
                    minPos = BlockPos.min(pos, minPos);
                }
                this.collisionBounds = AABB.encapsulatingFullBlocks(minPos, maxPos);
            }
            default -> this.collisionBounds = null;
        }
    }

    public Set<MergedOutline> calculateOverlappingOutlines() {
        Set<MergedOutline> newCollidingOutlines = new HashSet<>();
        for (Map.Entry<String, Outline> entry : Outliner.OUTLINES.entrySet()) {
            if (!(entry.getValue() instanceof MergedOutline outline)) continue;
            if (outline == this) continue;
            if (!this.dimension().equals(outline.dimension())) continue;
            if (this.collisionBounds().inflate(1).intersects(outline.collisionBounds())) {
                newCollidingOutlines.add(outline);
            }
        }
        return newCollidingOutlines;
    }

    public String mergeKey() {
        return mergeKey;
    }

    public boolean showCollisions() {
        return showCollisions;
    }

    public AABB collisionBounds() {
        return collisionBounds;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public Set<MergedOutline> cachedOverlappingOutlines() {
        return cachedOverlappingOutlines;
    }

    public void updateCachedOverlappingOutlines(Set<MergedOutline> outlines) {
        cachedOverlappingOutlines = outlines;
    }
}
