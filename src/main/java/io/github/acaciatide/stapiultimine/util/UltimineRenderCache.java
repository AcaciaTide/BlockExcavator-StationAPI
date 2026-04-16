package io.github.acaciatide.stapiultimine.util;

import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import net.minecraft.block.Block;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.world.World;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.Direction;
import net.modificationstation.stationapi.api.util.math.MutableBlockPos;

public class UltimineRenderCache {

    public static class LineSegment {
        public final float x1, y1, z1, x2, y2, z2;
        public LineSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
        }
    }

    private static class Edge {
        final byte axis; // 0=X, 1=Y, 2=Z
        final int x, y, z;

        Edge(byte axis, int x, int y, int z) {
            this.axis = axis;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;
            Edge edge = (Edge) o;
            return axis == edge.axis && x == edge.x && y == edge.y && z == edge.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(axis, x, y, z);
        }
    }

    private static int lastX = Integer.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static int lastZ = Integer.MIN_VALUE;
    
    // 計算済みの「外側だけを残した」線のリスト
    public static List<LineSegment> cachedLines = Collections.emptyList();
    
    // 計算済みの対象ブロック数
    public static int cachedBlockCount = 0;

    /**
     * MapにEdgeとそれを共有する面の法線(方向)を記録する
     */
    private static void addEdge(Map<Edge, Set<Direction>> map, byte axis, int x, int y, int z, Direction dir) {
        Edge e = new Edge(axis, x, y, z);
        map.computeIfAbsent(e, k -> new HashSet<>()).add(dir);
    }

    /**
     * キャッシュの更新。ブロック群から「外側の輪郭」だけを抽出した線リストを生成する。
     */
    public static void updateCache(World world, HitResult hit) {
        if (!ClientInitListener.isUltimineKeyPressed() || hit == null || hit.type != HitResultType.BLOCK) {
            cachedLines = Collections.emptyList();
            cachedBlockCount = 0;
            lastX = lastY = lastZ = Integer.MIN_VALUE;
            return;
        }

        // ブロック座標が変わっていなければ再計算をスキップ
        if (hit.blockX == lastX && hit.blockY == lastY && hit.blockZ == lastZ) {
            return;
        }

        lastX = hit.blockX;
        lastY = hit.blockY;
        lastZ = hit.blockZ;

        int id = world.getBlockId(lastX, lastY, lastZ);
        if (id <= 0) {
            cachedLines = Collections.emptyList();
            return;
        }

        Block block = Block.BLOCKS[id];
        int meta = world.getBlockMeta(lastX, lastY, lastZ);
        // 通常の破壊と同じリストを取得する
        Set<BlockPos> targets = VeinMinerUtil.getVeinBlocks(world, lastX, lastY, lastZ, block, meta);

        if (targets.isEmpty()) {
            cachedLines = Collections.emptyList();
            cachedBlockCount = 0;
            return;
        }
        
        cachedBlockCount = targets.size();

        // 輪郭メッシュ抽出ロジック
        Map<Edge, Set<Direction>> edgeNormals = new HashMap<>();
        MutableBlockPos searchPos = new MutableBlockPos();

        for (BlockPos pos : targets) {
            int bx = pos.getX();
            int by = pos.getY();
            int bz = pos.getZ();

            // 各ブロックの6方向の面を調べる
            for (Direction dir : Direction.values()) {
                searchPos.set(bx + dir.getOffsetX(), by + dir.getOffsetY(), bz + dir.getOffsetZ());

                // 隣が破壊対象でなければ、その面は「外部に露出している面（シルエットの一部）」
                if (!targets.contains(searchPos)) {
                    // 露出した面の外周（4辺）を記録する
                    if (dir == Direction.DOWN) { // Y- 面
                        addEdge(edgeNormals, (byte)0, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by, bz, dir);
                    } else if (dir == Direction.UP) { // Y+ 面
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by+1, bz, dir);
                    } else if (dir == Direction.EAST) { // Z- 面
                        addEdge(edgeNormals, (byte)0, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz, dir);
                    } else if (dir == Direction.WEST) { // Z+ 面
                        addEdge(edgeNormals, (byte)0, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz+1, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz+1, dir);
                    } else if (dir == Direction.NORTH) { // X- 面
                        addEdge(edgeNormals, (byte)1, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx, by+1, bz, dir);
                    } else if (dir == Direction.SOUTH) { // X+ 面
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by+1, bz, dir);
                    }
                }
            }
        }

        // 余分な内側の線をフィルタリング
        List<LineSegment> result = new ArrayList<>();
        
        for (Map.Entry<Edge, Set<Direction>> entry : edgeNormals.entrySet()) {
            // 一番外側の「角・コーナー」であれば、異なる向きの露出面が交わるので方向(法線)が2つ以上になる。
            // 面積が1（方向が1つ）のものは、平らな面上にある隣接ブロックのつなぎ目＝除外する
            if (entry.getValue().size() > 1) {
                Edge e = entry.getKey();
                if (e.axis == 0) {
                    result.add(new LineSegment(e.x, e.y, e.z, e.x + 1, e.y, e.z));
                } else if (e.axis == 1) {
                    result.add(new LineSegment(e.x, e.y, e.z, e.x, e.y + 1, e.z));
                } else if (e.axis == 2) {
                    result.add(new LineSegment(e.x, e.y, e.z, e.x, e.y, e.z + 1));
                }
            }
        }
        cachedLines = result;
    }
    
    /**
     * キャッシュを強制的にリセットし、次回のupdateCacheで再計算を走らせるようにする。
     */
    public static void resetCache() {
        lastX = lastY = lastZ = Integer.MIN_VALUE;
    }
}
