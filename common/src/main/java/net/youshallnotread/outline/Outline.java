package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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
import org.joml.Vector4f;

public abstract class Outline {
    private final float duration;
    private final String key;
    private final LocalDateTime createdTimestamp;
    private final float thickness;
    private final Vector4f colour;
    private final Vector3f inflation;

    private final Type type;

    private final ResourceKey<Level> dimension;
    private Collection<BlockPos> blockPosCollection;
    private BlockPos blockPos;
    private Pair<Vec3, Vec3> line;
    private Entity entity;

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
            case BLOCK -> this.blockPos = builder.blockPos;
            case BLOCKGROUP -> this.blockPosCollection = builder.blockPosCollection;
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
        return duration;
    }

    public LocalDateTime createdTimestamp() {
        return createdTimestamp;
    }

    public float thickness() {
        return thickness;
    }

    public Vector3f inflation() {
        return inflation;
    }

    public Vector4f colour() {
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
            throw new IllegalStateException("A dimension must be specified when creating a bounds of this type");
        }
        return dimension;
    }

    public Collection<BlockPos> blockPosCollection() {
        return blockPosCollection;
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public void setEntity(Entity newEntity) {
        entity = newEntity;
    }

    public Entity entity() {
        return entity;
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
        return line;
    }

    public enum Type {
        NONE,
        BLOCK,
        BLOCKGROUP,
        ENTITY,
        LINE
    }

    public static class Builder {

        boolean merge = false;
        boolean showCollisions = false;
        String mergeKey = "";
        private float duration = -1;
        private float thickness = 0.1f;
        private Vector3f inflation = new Vector3f(0);
        private Vector4f colour = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        private String key = "";
        private Type type = Type.NONE;
        private BiConsumer<Outline, Outline> onChangedCallback = null;
        private Consumer<Outline> onRemoveCallback = null;
        private Consumer<Outline> onSuspendCallback = null;
        private Consumer<Outline> onUnsuspendCallback = null;
        private Function<Outline, Boolean> removeIfCallback = null;
        private Function<Outline, Boolean> regenerateIfCallback = null;

        protected ResourceKey<Level> dimension = null;
        protected Collection<BlockPos> blockPosCollection = null;
        protected BlockPos blockPos = null;
        protected Pair<Vec3, Vec3> line = null;
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

        public Builder bounds(Vec3 start, Vec3 end, ResourceKey<Level> dimension) {
            this.line = Pair.of(start, end);
            this.dimension = dimension;
            if (this.type != Type.NONE)
                throw new IllegalStateException("The bounds for this outline have already been set");
            this.type = Type.LINE;
            return this;
        }

        public Builder thickness(float thickness) {
            this.thickness = thickness;
            return this;
        }

        public Builder inflation(Vector3f inflation) {
            this.inflation = inflation;
            return this;
        }

        public Builder colour(Vector4f colour) {
            this.colour = colour;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
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
                case BLOCK, BLOCKGROUP -> {
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
