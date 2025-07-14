package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import org.joml.Vector3f;

public abstract class Outline {
    private final String key;
    private final LocalDateTime createdTimestamp;
    private final Supplier<Float> duration;
    private final Supplier<Float> thickness;
    private final Supplier<Vector3f> colour;
    private final Supplier<Vector3f> inflation;
    private final Supplier<Boolean> greedy;

    private final Type type;

    private final Supplier<ResourceKey<Level>> dimension;
    private Supplier<Set<BlockPos>> blockPosCollection;
    private Supplier<Pair<Vec3, Vec3>> line;
    private Supplier<Entity> entity;

    private final BiConsumer<Outline, Outline> onChangedCallback;
    private final Consumer<Outline> onRemoveCallback;
    private final Consumer<Outline> onSuspendCallback;
    private final Consumer<Outline> onUnsuspendCallback;
    private final Function<Outline, Boolean> removeIfCallback;
    private final Function<Outline, Boolean> regenerateIfCallback;

    public Outline(Builder builder) {
        this.key = builder.key;
        this.duration = builder.duration;
        this.createdTimestamp = LocalDateTime.now();
        this.colour = builder.colour;
        this.thickness = builder.thickness;
        this.inflation = builder.inflation;
        this.type = builder.type;
        this.greedy = builder.greedy;
        this.dimension = builder.dimension;
        this.onChangedCallback = builder.onChangedCallback;
        this.onRemoveCallback = builder.onRemoveCallback;
        this.onSuspendCallback = builder.onSuspendCallback;
        this.onUnsuspendCallback = builder.onUnsuspendCallback;
        this.removeIfCallback = builder.removeIfCallback;
        this.regenerateIfCallback = builder.regenerateIfCallback;

        switch (this.type) {
            case ENTITY -> this.entity = builder.entity;
            case LINE -> this.line = builder.line;
            case BLOCK -> this.blockPosCollection = builder.blockPosCollection;
        }
    }

    abstract void setupVertexData();

    abstract boolean hasVertexData();

    abstract void cleanup();

    void transform(PoseStack pose) {
        if (this.type() == Type.ENTITY) {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 cameraPos = cam.getPosition();
            Vec3 pos = entity().getPosition(Outliner.RENDER_DELTA);

            double xDiff = cameraPos.x - pos.x;
            double yDiff = cameraPos.y - pos.y;
            double zDiff = cameraPos.z - pos.z;
            pose.mulPose(Axis.XP.rotation((float) Math.toRadians(cam.getXRot())));
            pose.mulPose(Axis.YP.rotation((float) Math.toRadians(cam.getYRot())));
            pose.translate(xDiff, -yDiff, zDiff);
        }
    }

    public String key() {
        return key;
    }

    public float duration() {
        if (duration == null) {
            return -1;
        }
        return duration.get();
    }

    public LocalDateTime createdTimestamp() {
        return createdTimestamp;
    }

    public float thickness() {
        if (thickness == null) {
            return 0.05f;
        }
        return thickness.get();
    }

    public Vector3f inflation() {
        if (inflation == null) {
            return new Vector3f(0);
        }
        return inflation.get();
    }

    public Vector3f colour() {
        if (colour == null) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }
        return colour.get();
    }

    public Type type() {
        return type;
    }

    public boolean greedy() {
        if (greedy == null) {
            return false;
        }
        return greedy.get();
    }

    public ResourceKey<Level> dimension() {
        if (dimension == null) {
            if (entity != null) {
                return entity.get().level().dimension();
            }
            throw new IllegalStateException("A dimension must be specified when creating a bounds of this type");
        }
        return dimension.get();
    }

    public Set<BlockPos> blockPosCollection() {
        return blockPosCollection.get();
    }

    public void setEntity(Supplier<Entity> newEntity) {
        entity = newEntity;
    }

    public Entity entity() {
        return entity.get();
    }

    public BiConsumer<Outline, Outline> onChangedCallback() {
        return onChangedCallback;
    }

    public Consumer<Outline> onRemoveCallback() {
        return onRemoveCallback;
    }

    public Consumer<Outline> onSuspendCallback() {
        return onSuspendCallback;
    }

    public Consumer<Outline> onUnsuspendCallback() {
        return onUnsuspendCallback;
    }

    public Function<Outline, Boolean> removeIfCallback() {
        return removeIfCallback;
    }

    public Function<Outline, Boolean> regenerateIfCallback() {
        return regenerateIfCallback;
    }

    public Pair<Vec3, Vec3> line() {
        return line.get();
    }

    public enum Type {
        NONE,
        BLOCK,
        ENTITY,
        LINE
    }

    public static class Builder {

        private String key = "";
        private Type type = Type.NONE;

        private Supplier<Float> duration = null;
        private Supplier<Float> thickness = null;
        private Supplier<Vector3f> inflation = null;
        private Supplier<Vector3f> colour = null;
        private Supplier<Boolean> greedy = null;

        boolean merge = false;
        Supplier<Boolean> showCollisions = null;
        String mergeKey = "";
        Supplier<Vector3f> collisionColour = null;
        Supplier<Float> collisionThickness = null;

        private BiConsumer<Outline, Outline> onChangedCallback = null;
        private Consumer<Outline> onRemoveCallback = null;
        private Consumer<Outline> onSuspendCallback = null;
        private Consumer<Outline> onUnsuspendCallback = null;
        private Function<Outline, Boolean> removeIfCallback = null;
        private Function<Outline, Boolean> regenerateIfCallback = null;

        protected Supplier<ResourceKey<Level>> dimension = null;
        protected Supplier<Set<BlockPos>> blockPosCollection = null;
        protected Supplier<Pair<Vec3, Vec3>> line = null;
        protected Supplier<Entity> entity = null;

        public Builder duration(Supplier<Float> duration) {
            this.duration = duration;
            return this;
        }

        public Builder merge(Supplier<Boolean> showCollisions, String mergeKey) {
            this.merge = true;
            this.showCollisions = showCollisions;
            this.mergeKey = mergeKey;
            return this;
        }

        public Builder collisionColour(Supplier<Vector3f> collisionColour) {
            this.collisionColour = collisionColour;
            return this;
        }

        public Builder collisionThickness(Supplier<Float> collisionThickness) {
            this.collisionThickness = collisionThickness;
            return this;
        }

        public Builder boundsBlockGroup(Supplier<Set<BlockPos>> blocks, Supplier<ResourceKey<Level>> dimension) {
            this.blockPosCollection = blocks;
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.BLOCK;
            return this;
        }

        public Builder boundsBlock(Supplier<BlockPos> blockPos, Supplier<ResourceKey<Level>> dimension) {
            this.blockPosCollection = () -> {
                Set<BlockPos> set = new HashSet<>();
                set.add(blockPos.get());
                return set;
            };
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.BLOCK;
            return this;
        }

        public Builder boundsEntity(Supplier<Entity> entity) {
            this.entity = entity;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.ENTITY;
            return this;
        }

        public Builder boundsLine(Supplier<Pair<Vec3, Vec3>> line, Supplier<ResourceKey<Level>> dimension) {
            this.line = line;
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.LINE;
            return this;
        }

        public Builder thickness(Supplier<Float> thickness) {
            this.thickness = thickness;
            return this;
        }

        public Builder inflation(Supplier<Vector3f> inflation) {
            this.inflation = inflation;
            return this;
        }

        public Builder colour(Supplier<Vector3f> colour) {
            this.colour = colour;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder greedy(Supplier<Boolean> greedy) {
            this.greedy = greedy;
            return this;
        }

        public Builder onChanged(BiConsumer<Outline, Outline> onChangedCallback) {
            this.onChangedCallback = onChangedCallback;
            return this;
        }

        public Builder onRemoved(Consumer<Outline> onRemoveCallback) {
            this.onRemoveCallback = onRemoveCallback;
            return this;
        }

        public Builder onSuspended(Consumer<Outline> onSuspendCallback) {
            this.onSuspendCallback = onSuspendCallback;
            return this;
        }

        public Builder onUnsuspended(Consumer<Outline> onUnsuspendCallback) {
            this.onUnsuspendCallback = onUnsuspendCallback;
            return this;
        }

        public Builder removeIf(Function<Outline, Boolean> removeIfCallback) {
            this.removeIfCallback = removeIfCallback;
            return this;
        }

        public Builder regenerateIf(Function<Outline, Boolean> regenerateIfCallback) {
            this.regenerateIfCallback = regenerateIfCallback;
            return this;
        }

        public Outline build() {
            if (key.isEmpty()) {
                throw new IllegalStateException("Outline must have a key specified");
            }

            switch (type) {
                case NONE -> throw new IllegalStateException("Outline must have a type specified");
                case BLOCK -> {
                    if (merge) {
                        return new MergedOutline(this);
                    } else {
                        return new BatchedOutline(this);
                    }
                }
                default -> {
                    if (merge) {
                        throw new IllegalStateException("This type of outline does not support merging");
                    }
                    return new StandaloneOutline(this);
                }
            }
        }
    }
}
