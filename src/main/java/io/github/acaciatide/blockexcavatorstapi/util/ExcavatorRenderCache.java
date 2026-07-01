package io.github.acaciatide.blockexcavatorstapi.util;

import io.github.acaciatide.blockexcavatorstapi.events.init.ClientInitListener;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.util.math.Direction;
import net.modificationstation.stationapi.api.util.math.StationBlockPos;

import java.util.*;

public class ExcavatorRenderCache {

    private ExcavatorRenderCache() {}

    public static class LineSegment {
        public final float x1;
        public final float y1;
        public final float z1;
        public final float x2;
        public final float y2;
        public final float z2;
        public LineSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
        }
    }

    // 方向配列を事前にキャッシュし、配列のコピー生成を防ぐ
    private static final Direction[] DIRECTIONS = Direction.values();

    private static volatile int lastX = Integer.MIN_VALUE;
    private static volatile int lastY = Integer.MIN_VALUE;
    private static volatile int lastZ = Integer.MIN_VALUE;
    private static volatile int lastId = -1;
    private static volatile int lastMeta = -1;
    
    // 計算済みの「外側だけを残した」線のリスト。スレッド間の可視性を保証するためvolatileを付与する
    public static volatile List<LineSegment> cachedLines = Collections.emptyList();
    
    // 計算済みの対象ブロック数。スレッド間の可視性を保証するためvolatileを付与する
    public static volatile int cachedBlockCount = 0;

    /**
     * 辺の向き（軸）と始点座標（x, y, z）を1つの 64ビット long 値にパックする。
     * ビット割り当て：
     * - 軸 (axis): 2 ビット (ビット 62 〜 63) -> 0=X軸, 1=Y軸, 2=Z軸
     * - X 座標: 26 ビット (ビット 36 〜 61) -> 表現範囲: -33,554,432 〜 33,554,431
     * - Y 座標: 10 ビット (ビット 26 〜 35) -> 表現範囲: -512 〜 511
     * - Z 座標: 26 ビット (ビット 0 〜 25)  -> 表現範囲: -33,554,432 〜 33,554,431
     * 
     * @param axis 軸のID (0=X, 1=Y, 2=Z)
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @return パックされた long 値
     */
    private static long encodeEdge(byte axis, int x, int y, int z) {
        return ((long) (axis & 3) << 62) |
               ((long) (x & 0x3FFFFFF) << 36) |
               ((long) (y & 0x3FF) << 26) |
               (z & 0x3FFFFFF);
    }

    // Mapに辺のキーと方向のビットマスクを記録する。ラムダの生成を防ぐため、プリミティブ直操作で行う
    private static void addEdge(Long2ByteMap map, byte axis, int x, int y, int z, Direction dir) {
        long key = encodeEdge(axis, x, y, z);
        byte mask = (byte) (1 << dir.ordinal());
        byte oldVal = map.get(key); // キーが存在しない場合はデフォルト値 0 が返る
        map.put(key, (byte) (oldVal | mask));
    }

    /**
     * キャッシュの更新。ブロック群から「外側の輪郭」だけを抽出した線リストを生成する。
     */
    public static void updateCache(World world, PlayerEntity player, HitResult hit) {
        if (!ClientInitListener.isExcavatorKeyPressed() || hit == null || hit.type != HitResultType.BLOCK) {
            cachedLines = Collections.emptyList();
            cachedBlockCount = 0;
            lastX = lastY = lastZ = Integer.MIN_VALUE;
            lastId = -1;
            lastMeta = -1;
            return;
        }

        int id = world.getBlockId(hit.blockX, hit.blockY, hit.blockZ);
        if (id <= 0) {
            cachedLines = Collections.emptyList();
            return;
        }

        int meta = world.getBlockMeta(hit.blockX, hit.blockY, hit.blockZ);

        // ブロック座標、ID、メタデータのいずれかが変わっていれば再計算する
        if (hit.blockX == lastX && hit.blockY == lastY && hit.blockZ == lastZ && id == lastId && meta == lastMeta) {
            return;
        }

        lastX = hit.blockX;
        lastY = hit.blockY;
        lastZ = hit.blockZ;
        lastId = id;
        lastMeta = meta;

        Block block = Block.BLOCKS[id];
        // 通常の破壊と同じリストを取得する
        Set<BlockPos> targets = VeinMinerUtil.getVeinBlocks(world, player, lastX, lastY, lastZ, block, meta, hit.side);

        if (targets.isEmpty()) {
            cachedLines = Collections.emptyList();
            cachedBlockCount = 0;
            return;
        }
        
        cachedBlockCount = targets.size();

        LongSet targetSet = new LongOpenHashSet((int) ((float) targets.size() / 0.75f) + 1);
        for (BlockPos p : targets) {
            targetSet.add(StationBlockPos.asLong(p.getX(), p.getY(), p.getZ()));
        }

        // 輪郭メッシュ抽出ロジック
        int estimatedEdges = targets.size() * 12;
        int initialCapacity = (int) ((float) estimatedEdges / 0.75f) + 1;
        Long2ByteMap edgeNormals = new Long2ByteOpenHashMap(initialCapacity);
        edgeNormals.defaultReturnValue((byte) 0);

        for (BlockPos pos : targets) {
            int bx = pos.getX();
            int by = pos.getY();
            int bz = pos.getZ();

            // 各ブロックの6方向の面を調べる
            for (Direction dir : DIRECTIONS) {
                int nx = bx + dir.getOffsetX();
                int ny = by + dir.getOffsetY();
                int nz = bz + dir.getOffsetZ();

                // 隣が破壊対象でなければ、その面は「外部に露出している面（シルエットの一部）」と判定する
                if (!targetSet.contains(StationBlockPos.asLong(nx, ny, nz))) {
                    // 露出した面の外周（4辺）を記録する
                    if (dir == Direction.DOWN) { // Y- 面 (底面)
                        addEdge(edgeNormals, (byte)0, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by, bz, dir);
                    } else if (dir == Direction.UP) { // Y+ 面 (上面)
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by+1, bz, dir);
                    } else if (dir == Direction.NORTH) { // Z- 面 (奥面)
                        addEdge(edgeNormals, (byte)0, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz, dir);
                    } else if (dir == Direction.SOUTH) { // Z+ 面 (手前面)
                        addEdge(edgeNormals, (byte)0, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)0, bx, by+1, bz+1, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz+1, dir);
                    } else if (dir == Direction.WEST) { // X- 面 (左側面)
                        addEdge(edgeNormals, (byte)1, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx, by+1, bz, dir);
                    } else if (dir == Direction.EAST) { // X+ 面 (右側面)
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz, dir);
                        addEdge(edgeNormals, (byte)1, bx+1, by, bz+1, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by, bz, dir);
                        addEdge(edgeNormals, (byte)2, bx+1, by+1, bz, dir);
                    }
                }
            }
        }

        // 余分な内側の線をフィルタリングする
        List<LineSegment> result = new ArrayList<>(targets.size() * 2);
        
        // オブジェクトのボクシングを防ぐため、プリミティブ直操作で走査する
        for (Long2ByteMap.Entry entry : edgeNormals.long2ByteEntrySet()) {
            byte mask = entry.getByteValue();
            // 一番外側の「角・コーナー」であれば、異なる向きの露出面が交わるので方向のビット数が2つ以上になる
            if (Integer.bitCount(mask & 0xFF) > 1) {
                long key = entry.getLongKey();
                // 64ビット long 値から各要素（軸、x, y, z 座標）を抽出する。
                // - axis: ビット 62〜63 を抽出し、下位2ビットを取り出す。
                // - x: ビット 36〜61 を抽出し、下位26ビットを取り出す。
                // - y: ビット 26〜35 を抽出し、下位10ビットを取り出す。
                // - z: ビット 0〜25 を抽出し、下位26ビットを取り出す。
                byte axis = (byte) ((key >> 62) & 3);
                int x = (int) ((key >> 36) & 0x3FFFFFF);
                int y = (int) ((key >> 26) & 0x3FF);
                int z = (int) (key & 0x3FFFFFF);

                // 26ビット（X, Z）および 10ビット（Y）の符号ビットを検出し、負の座標を復元する（符号拡張）。
                // - X と Z: 26ビット目の符号ビット（0x2000000）が立っている場合、負の座標と判定して
                //   上位6ビットを 1 で埋める（x |= 0xFC000000）ことで、32ビット符号付き整数へと正しく復元する。
                // - Y: 10ビット目の符号ビット（0x200）が立っている場合、負の座標と判定して
                //   上位22ビットを 1 で埋める（y |= 0xFFFFFC00）ことで、32ビット符号付き整数へと正しく復元する。
                if ((x & 0x2000000) != 0) x |= 0xFC000000;
                if ((y & 0x200) != 0) y |= 0xFFFFFC00;
                if ((z & 0x2000000) != 0) z |= 0xFC000000;

                if (axis == 0) {
                    result.add(new LineSegment(x, y, z, x + 1, y, z));
                } else if (axis == 1) {
                    result.add(new LineSegment(x, y, z, x, y + 1, z));
                } else if (axis == 2) {
                    result.add(new LineSegment(x, y, z, x, y, z + 1));
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
        lastId = -1;
        lastMeta = -1;
    }
}
