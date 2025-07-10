package net.youshallnotread.outline;

import java.time.LocalDateTime;
import java.util.Collection;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import org.joml.Vector4f;

public class Outline {
    private final float duration;
    private final String key;
    private final LocalDateTime createdTimestamp;
    private final float thickness;
    private final Vector4f colour;

    private final Type type;
    private VertexBuffer vertexBuffer;

    private final ResourceKey<Level> dimension;
    private Collection<BlockPos> blockPosCollection;
    private BlockPos blockPos;
    private Pair<Vec3, Vec3> line;
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
            case LINE -> this.line = builder.line;
            case BLOCK -> this.blockPos = builder.blockPos;
            case BLOCKGROUP -> this.blockPosCollection = builder.blockPosCollection;
        }
    }

    void populateVertexBuffer() {
        RenderSystem.assertOnRenderThread();
        Tesselator tesselator = Tesselator.getInstance();

        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        switch (this.type()) {
            case ENTITY -> {
                OutlineMeshBuilder.buildMesh(
                        this.entity(), this.colour(), this.thickness(), (position, colour) -> buffer.addVertex(
                                        (float) position.x, (float) position.y, (float) position.z)
                                .setColor(colour.x, colour.y, colour.z, colour.w));
            }
            case LINE -> {}
            case BLOCK -> {}
            case BLOCKGROUP -> {}
        }

        vertexBuffer.bind();
        vertexBuffer.upload(buffer.build());
        VertexBuffer.unbind();
    }

    void cleanup() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

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

    public Vector4f colour() {
        return colour;
    }

    public Type type() {
        return type;
    }

    public VertexBuffer buffer() {
        return vertexBuffer;
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
        private float thickness = 1;
        private Vector4f colour = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        private String key = "";
        private Type type = Type.NONE;

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

        public Builder colour(Vector4f colour) {
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
