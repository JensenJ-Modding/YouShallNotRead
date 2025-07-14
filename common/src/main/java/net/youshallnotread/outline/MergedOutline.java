package net.youshallnotread.outline;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class MergedOutline extends BatchedOutline {

    private final Supplier<Boolean> showCollisions;
    private final String mergeKey;
    private final AABB collisionBounds;
    private Set<MergedOutline> cachedOverlappingOutlines = new HashSet<>();
    private static final Set<MergedOutline> dirtyOutlines = new HashSet<>();

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

    @Override
    void setupVertexData() {
        super.setupVertexData();
        dirtyOutlines.remove(this);
    }

    public boolean hasMergedOutlinesChanged() {
        Set<MergedOutline> newCollidingOutlines = new HashSet<>();
        for (Map.Entry<String, Outline> entry : Outliner.OUTLINES.entrySet()) {
            if (!(entry.getValue() instanceof MergedOutline outline)) continue;
            if (outline == this) continue;
            if (!this.dimension().equals(outline.dimension())) continue;
            // TODO: Modify this so it only checks the 6 directly adjacent blocks, not diagonals
            if (this.collisionBounds().inflate(1).intersects(outline.collisionBounds())) {
                newCollidingOutlines.add(outline);
            }
        }

        boolean isEqual = newCollidingOutlines.equals(cachedOverlappingOutlines);
        cachedOverlappingOutlines = newCollidingOutlines;
        return !isEqual;
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

    public void markDirty() {
        dirtyOutlines.add(this);
        for (MergedOutline outline : cachedOverlappingOutlines) {
            if (dirtyOutlines.contains(outline)) continue;
            outline.markDirty();
        }
    }

    public boolean dirty() {
        return dirtyOutlines.contains(this);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        dirtyOutlines.remove(this);
        cachedOverlappingOutlines.clear();
    }
}
