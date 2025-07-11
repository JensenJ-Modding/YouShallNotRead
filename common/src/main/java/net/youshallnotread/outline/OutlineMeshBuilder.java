package net.youshallnotread.outline;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.*;

// NOTE: A lot of this code is copied, pasted and modified from the Catnip Library for Create / Ponder
// I would have used the lib directly as we already have it included for Ponder, but they were private classes
// This was easier than mixins, and this works with our rendering pipeline
public class OutlineMeshBuilder {

    public static void buildMesh(Outline outline, BiConsumer<Vector3d, Vector4f> vertexConsumer) {
        switch (outline.type()) {
            case ENTITY -> OutlineMeshBuilder.buildMesh(
                    outline.entity(), outline.colour(), outline.thickness(), vertexConsumer);
            case LINE -> {}
            case BLOCK -> OutlineMeshBuilder.buildMesh(
                    outline.blockPos(), outline.colour(), outline.thickness(), vertexConsumer);
            case BLOCKGROUP -> OutlineMeshBuilder.buildMesh(
                    outline.blockPosCollection(), outline.colour(), outline.thickness(), vertexConsumer);
        }
    }

    public static void buildMesh(
            Iterable<BlockPos> positions,
            Vector4f colour,
            float outlineWidth,
            BiConsumer<Vector3d, Vector4f> vertexConsumer) {
        Cluster cluster = new Cluster();
        positions.forEach(cluster::include);

        if (outlineWidth <= 0) return;
        if (cluster.isEmpty()) return;
        cluster.visibleEdges.forEach(edge -> {
            BlockPos pos = edge.pos;
            Vec3 origin = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, edge.axis);
            buildCuboidLine(vertexConsumer, origin, direction, outlineWidth, colour);
        });
    }

    public static void buildMesh(
            BlockPos pos, Vector4f colour, float outlineWidth, BiConsumer<Vector3d, Vector4f> vertexConsumer) {
        if (outlineWidth <= 0) return;
        buildCuboid(
                vertexConsumer,
                pos.getCenter().subtract(0.5, 0.5, 0.5),
                pos.getCenter().add(0.5, 0.5, 0.5),
                colour);
    }

    public static void buildMesh(
            Entity entity, Vector4f colour, float outlineWidth, BiConsumer<Vector3d, Vector4f> vertexConsumer) {
        if (outlineWidth <= 0) return;
        AABB boundingBox = entity.getBoundingBox()
                .move(new Vec3(entity.getX(), entity.getY(), entity.getZ()).multiply(-1, -1, -1));
        buildCuboid(vertexConsumer, boundingBox.getMinPosition(), boundingBox.getMaxPosition(), colour);
    }

    private static void buildCuboidLine(
            BiConsumer<Vector3d, Vector4f> vertexConsumer,
            Vec3 origin,
            Direction direction,
            float outlineWidth,
            Vector4f colour) {
        float halfWidth = outlineWidth / 2;
        Vec3 minPos = new Vec3(origin.x() - halfWidth, origin.y() - halfWidth, origin.z() - halfWidth);
        Vec3 maxPos = new Vec3(origin.x() + halfWidth, origin.y() + halfWidth, origin.z() + halfWidth);

        switch (direction) {
            case DOWN -> minPos = minPos.add(0, -1, 0);
            case UP -> maxPos = maxPos.add(0, 1, 0);
            case NORTH -> minPos = minPos.add(0, 0, -1);
            case SOUTH -> maxPos = maxPos.add(0, 0, 1);
            case WEST -> minPos = minPos.add(-1, 0, 0);
            case EAST -> maxPos = maxPos.add(1, 0, 0);
        }

        buildCuboid(vertexConsumer, minPos, maxPos, colour);
    }

    private static void buildCuboid(
            BiConsumer<Vector3d, Vector4f> vertexConsumer, Vec3 minPos, Vec3 maxPos, Vector4f colour) {
        double minX = minPos.x();
        double minY = minPos.y();
        double minZ = minPos.z();
        double maxX = maxPos.x();
        double maxY = maxPos.y();
        double maxZ = maxPos.z();

        // down
        vertexConsumer.accept(new Vector3d(minX, minY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(minX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, maxZ), colour);

        // up
        vertexConsumer.accept(new Vector3d(minX, maxY, minZ), colour);
        vertexConsumer.accept(new Vector3d(minX, maxY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, maxY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, maxY, minZ), colour);

        // north
        vertexConsumer.accept(new Vector3d(maxX, maxY, minZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(minX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(minX, maxY, minZ), colour);

        // south
        vertexConsumer.accept(new Vector3d(minX, maxY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(minX, minY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, maxY, maxZ), colour);

        // west
        vertexConsumer.accept(new Vector3d(minX, maxY, minZ), colour);
        vertexConsumer.accept(new Vector3d(minX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(minX, minY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(minX, maxY, maxZ), colour);

        // east
        vertexConsumer.accept(new Vector3d(maxX, maxY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, maxZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, minY, minZ), colour);
        vertexConsumer.accept(new Vector3d(maxX, maxY, minZ), colour);
    }

    private static class Cluster {

        private BlockPos anchor;
        private final Set<MergeEntry> visibleEdges;

        public Cluster() {
            visibleEdges = new HashSet<>();
        }

        public boolean isEmpty() {
            return anchor == null;
        }

        public void include(BlockPos pos) {
            if (anchor == null) anchor = pos;

            pos = pos.subtract(anchor);

            // 12 EDGES
            for (Direction.Axis axis : Direction.Axis.values()) {
                for (Direction.Axis axis2 : Direction.Axis.values()) {
                    if (axis == axis2) continue;
                    for (Direction.Axis axis3 : Direction.Axis.values()) {
                        if (axis == axis3) continue;
                        if (axis2 == axis3) continue;

                        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis2);
                        Direction direction2 = Direction.get(Direction.AxisDirection.POSITIVE, axis3);

                        for (int offset : new int[] {0, 1}) {
                            BlockPos entryPos = pos.relative(direction, offset);
                            for (int offset2 : new int[] {0, 1}) {
                                entryPos = entryPos.relative(direction2, offset2);
                                MergeEntry entry = new MergeEntry(axis, entryPos);
                                if (!visibleEdges.remove(entry)) visibleEdges.add(entry);
                            }
                        }
                    }

                    break;
                }
            }
        }
    }

    private record MergeEntry(Direction.Axis axis, BlockPos pos) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MergeEntry other)) return false;

            return this.axis == other.axis && this.pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return this.pos.hashCode() * 31 + axis.ordinal();
        }
    }
}
