package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class Outline {
    private final float duration;
    private final String key;
    private boolean dirty;
    private final LocalDateTime createdTimestamp;
    private final float thickness;
    private final int colour;

    private final Type type;

    private final ResourceKey<Level> dimension;
    private Collection<BlockPos> blockPosCollection;
    private BlockPos blockPos;

    private Entity entity;

    public Outline(Builder builder) {
        this.key = builder.key;
        this.duration = builder.duration;
        this.createdTimestamp = LocalDateTime.now();
        this.colour = builder.colour;
        this.thickness = builder.thickness;
        this.type = builder.type;
        this.dimension = builder.dimension;

        switch (this.type) {
            case ENTITY -> this.entity = builder.entity;
            case BLOCK -> this.blockPos = builder.blockPos;
            case BLOCKGROUP -> this.blockPosCollection = builder.blockPosCollection;
        }
    }

    public String key() {
        return key;
    }

    public float duration() {
        return duration;
    }

    public LocalDateTime createdTimestamp() {
        return createdTimestamp;
    }

    public float thickness() {
        return thickness;
    }

    public int colour() {
        return colour;
    }

    public Type type() {
        return type;
    }

    public ResourceKey<Level> dimension() {
        if (dimension == null) {
            if (entity != null) {
                return entity.level().dimension();
            }
            throw new IllegalStateException("A dimension must be specified when creating a bounds of type");
        }
        return dimension;
    }

    public Collection<BlockPos> blockPosCollection() {
        return blockPosCollection;
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public Entity entity() {
        return entity;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public enum Type {
        NONE,
        BLOCK,
        BLOCKGROUP,
        ENTITY
    }

    public static class Builder {

        boolean merge = false;
        boolean showCollisions = false;
        String mergeKey = "";
        private float duration = -1;
        private float thickness = 1;
        private int colour = 0xFFFFFF;
        private String key = "";
        private Type type = Type.NONE;

        protected ResourceKey<Level> dimension = null;
        protected Collection<BlockPos> blockPosCollection = null;
        protected BlockPos blockPos = null;
        protected Entity entity = null;

        public Builder duration(float duration) {
            this.duration = duration;
            return this;
        }

        public Builder merge(boolean showCollisions, String mergeKey) {
            this.merge = true;
            this.showCollisions = showCollisions;
            this.mergeKey = mergeKey;
            return this;
        }

        public Builder bounds(Collection<BlockPos> blocks, ResourceKey<Level> dimension) {
            this.blockPosCollection = blocks;
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.BLOCKGROUP;
            return this;
        }

        public Builder bounds(BlockPos blockPos, ResourceKey<Level> dimension) {
            this.blockPos = blockPos;
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.BLOCK;
            return this;
        }

        public Builder bounds(Entity entity) {
            this.entity = entity;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.ENTITY;
            return this;
        }

        public Builder thickness(float thickness) {
            this.thickness = thickness;
            return this;
        }

        public Builder colour(int colour) {
            this.colour = colour;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Outline build() {
            if (key.isEmpty()) {
                throw new IllegalStateException("Outline must have a key specified");
            }
            if (type == Type.NONE) {
                throw new IllegalStateException("Outline must have a type specified");
            }
            if (merge) {
                if (!(type == Type.BLOCK || type == Type.BLOCKGROUP)) {
                    throw new IllegalStateException("This type of outline does not support merging");
                }
                return new MergedOutline(this);
            }
            return new Outline(this);
        }
    }
}
