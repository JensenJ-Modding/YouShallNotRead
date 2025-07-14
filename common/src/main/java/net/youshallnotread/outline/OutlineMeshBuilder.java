package net.youshallnotread.outline;

import java.util.*;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.Pair;
import org.joml.*;

// Credit: A lot of the code in this class, particularly to do with the mesh building for merged outlines is from the
// Ponder Library that Create uses.
// It's been modified somewhat to work more closely to our renderer here.
public class OutlineMeshBuilder {

    private static final int[][] EDGES = new int[][] {
        {0, 1}, {0, 2},
        {0, 4}, {1, 3},
        {1, 5}, {2, 3},
        {2, 6}, {3, 7},
        {4, 5}, {4, 6},
        {5, 7}, {6, 7},
    };

    public static void buildMesh(Outline outline, BiConsumer<Vector3d, Vector3f> vertexConsumer) {
        switch (outline.type()) {
            case ENTITY -> OutlineMeshBuilder.buildEntity(
                    outline.entity(), outline.colour(), outline.thickness(), outline.inflation(), vertexConsumer);
            case LINE -> OutlineMeshBuilder.buildLine(
                    outline.line().first(),
                    outline.line().second(),
                    outline.colour(),
                    outline.thickness(),
                    vertexConsumer);
            case BLOCK -> OutlineMeshBuilder.buildBlockGroup(outline, vertexConsumer);
        }
    }

    private static void buildBlockGroup(Outline outline, BiConsumer<Vector3d, Vector3f> vertexConsumer) {
        Cluster cluster = new Cluster();
        Set<BlockPos> positions = outline.blockPosCollection();
        if (outline instanceof MergedOutline merged) {
            Set<MergedOutline> overlappingOutlines = merged.collidingOutlines();

            for (MergedOutline mergedOutline : overlappingOutlines) {
                positions.addAll(mergedOutline.blockPosCollection());
            }

            positions.forEach((pos) -> cluster.include(pos, outline.blockPosCollection(), overlappingOutlines));
            merged.edges = cluster.edges;
        } else {
            positions.forEach((pos) -> cluster.include(pos, outline.blockPosCollection(), null));
        }

        if (outline.thickness() <= 0) return;

        if (outline.greedy()) {
            Map<Direction.Axis, List<Pair<BlockPos, Boolean>>> byAxis = new EnumMap<>(Direction.Axis.class);
            for (MergeEntry entry : cluster.edges) {
                byAxis.computeIfAbsent(entry.axis, a -> new ArrayList<>()).add(Pair.of(entry.pos, entry.colliding));
            }

            for (MergeEntry entry : cluster.collidingEdges) {
                byAxis.computeIfAbsent(entry.axis, a -> new ArrayList<>()).add(Pair.of(entry.pos, entry.colliding));
            }

            for (var axis : Direction.Axis.values()) {
                List<Pair<BlockPos, Boolean>> edges = byAxis.get(axis);
                if (edges == null) continue;

                edges.sort(Comparator.comparingInt(
                                (ob) -> ((Pair<BlockPos, Boolean>) ob).first().getX())
                        .thenComparingInt(
                                (ob) -> ((Pair<BlockPos, Boolean>) ob).first().getY())
                        .thenComparingInt(
                                (ob) -> ((Pair<BlockPos, Boolean>) ob).first().getZ()));

                Set<BlockPos> visited = new HashSet<>();
                for (Pair<BlockPos, Boolean> start : edges) {
                    if (!visited.add(start.first())) continue;

                    BlockPos end = start.first();
                    Direction dir =
                            switch (axis) {
                                case X -> Direction.EAST;
                                case Y -> Direction.UP;
                                case Z -> Direction.SOUTH;
                            };

                    while (true) {
                        Pair<BlockPos, Boolean> next = Pair.of(end.relative(dir), start.second());
                        if (!edges.contains(next) || !visited.add(next.first())) break;
                        end = next.first();
                    }

                    Vec3 startVec = new Vec3(
                            start.first().getX(),
                            start.first().getY(),
                            start.first().getZ());
                    BlockPos endOffset = end.offset(dir.getNormal());
                    Vec3 endVec = new Vec3(endOffset.getX(), endOffset.getY(), endOffset.getZ());

                    if (outline instanceof MergedOutline merged) {
                        if (merged.showCollisions() && start.second()) {
                            if (merged.collisionThickness() <= 0) continue;
                            buildLine(
                                    startVec,
                                    endVec,
                                    merged.collisionColour(),
                                    merged.collisionThickness(),
                                    vertexConsumer);
                        } else if (!start.second()) {
                            buildLine(startVec, endVec, outline.colour(), outline.thickness(), vertexConsumer);
                        }
                        continue;
                    }
                    buildLine(startVec, endVec, outline.colour(), outline.thickness(), vertexConsumer);
                }
            }
        } else {
            Set<MergeEntry> edges = cluster.edges;
            edges.addAll(cluster.collidingEdges);
            for (MergeEntry edge : cluster.edges) {
                BlockPos pos = edge.pos;
                Vec3 origin = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, edge.axis);
                if (outline instanceof MergedOutline merged) {
                    if (merged.showCollisions() && edge.colliding) {
                        if (merged.collisionThickness() <= 0) continue;
                        buildLine(
                                origin,
                                direction,
                                merged.collisionColour(),
                                merged.collisionThickness(),
                                vertexConsumer);
                    } else if (!edge.colliding) {
                        buildLine(origin, direction, outline.colour(), outline.thickness(), vertexConsumer);
                    }
                    continue;
                }
                buildLine(origin, direction, outline.colour(), outline.thickness(), vertexConsumer);
            }
        }
    }

    public static void buildEntity(
            Entity entity,
            Vector3f colour,
            float thickness,
            Vector3f outlineInflation,
            BiConsumer<Vector3d, Vector3f> vertexConsumer) {
        if (thickness <= 0) return;
        AABB bb = entity.getBoundingBox()
                .inflate(outlineInflation.x, outlineInflation.y, outlineInflation.z)
                .move(new Vec3(entity.getX(), entity.getY(), entity.getZ()).multiply(-1, -1, -1));

        Vec3[] corners = getCornerPositions(bb);

        for (int[] edge : EDGES) {
            buildLine(corners[edge[0]], corners[edge[1]], colour, thickness, vertexConsumer);
        }
    }

    private static Vec3[] getCornerPositions(AABB bb) {
        Vec3 min = bb.getMinPosition();
        Vec3 max = bb.getMaxPosition();

        return new Vec3[] {
            new Vec3(min.x, min.y, min.z),
            new Vec3(max.x, min.y, min.z),
            new Vec3(min.x, max.y, min.z),
            new Vec3(max.x, max.y, min.z),
            new Vec3(min.x, min.y, max.z),
            new Vec3(max.x, min.y, max.z),
            new Vec3(min.x, max.y, max.z),
            new Vec3(max.x, max.y, max.z),
        };
    }

    private static void buildLine(
            Vec3 origin,
            Direction direction,
            Vector3f colour,
            float outlineWidth,
            BiConsumer<Vector3d, Vector3f> vertexConsumer) {
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

    private static void buildLine(
            Vec3 start, Vec3 end, Vector3f colour, float thickness, BiConsumer<Vector3d, Vector3f> vertexConsumer) {
        Vec3 dir = end.subtract(start);
        Vec3 norm = dir.normalize();
        Vec3 offset = norm.scale(thickness / 2.0);

        Vec3 newStart = start.subtract(offset);
        Vec3 newEnd = end.add(offset);

        double minX = java.lang.Math.min(newStart.x, newEnd.x);
        double minY = java.lang.Math.min(newStart.y, newEnd.y);
        double minZ = java.lang.Math.min(newStart.z, newEnd.z);
        double maxX = java.lang.Math.max(newStart.x, newEnd.x);
        double maxY = java.lang.Math.max(newStart.y, newEnd.y);
        double maxZ = java.lang.Math.max(newStart.z, newEnd.z);

        double dx = maxX - minX == 0 ? thickness : 0;
        double dy = maxY - minY == 0 ? thickness : 0;
        double dz = maxZ - minZ == 0 ? thickness : 0;

        buildCuboid(
                vertexConsumer,
                new Vec3(minX - dx / 2, minY - dy / 2, minZ - dz / 2),
                new Vec3(maxX + dx / 2, maxY + dy / 2, maxZ + dz / 2),
                colour);
    }

    private static void buildCuboid(
            BiConsumer<Vector3d, Vector3f> vertexConsumer, Vec3 minPos, Vec3 maxPos, Vector3f colour) {
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

        private final Set<MergeEntry> edges;
        private final Set<MergeEntry> collidingEdges;

        public Cluster() {
            edges = new HashSet<>();
            collidingEdges = new HashSet<>();
        }

        public void include(BlockPos pos, Set<BlockPos> outlineBlockSet, Set<MergedOutline> otherOutlines) {
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

                                boolean colliding = false;
                                if (otherOutlines != null) {
                                    if (outlineBlockSet.contains(pos)) {
                                        for (MergedOutline other : otherOutlines) {
                                            if (other.blockPosCollection().contains(entryPos)) {
                                                colliding = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                MergeEntry entry = new MergeEntry(axis, entryPos, false);
                                boolean skipOutline = false;
                                if (otherOutlines != null) {
                                    for (MergedOutline other : otherOutlines) {
                                        if (other.edges.contains(entry)) {
                                            skipOutline = true;
                                            break;
                                        }
                                    }
                                }

                                if (!edges.remove(entry)) {
                                    if (!skipOutline) {
                                        edges.add(entry);
                                    }
                                }

                                if (colliding) {
                                    MergeEntry collidingEntry = new MergeEntry(axis, entryPos, true);
                                    if (!collidingEdges.remove(collidingEntry)) {
                                        collidingEdges.add(collidingEntry);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public record MergeEntry(Direction.Axis axis, BlockPos pos, boolean colliding) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MergeEntry other)) return false;

            return this.axis == other.axis && this.pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode() * 31 + axis.ordinal();
        }
    }
}
