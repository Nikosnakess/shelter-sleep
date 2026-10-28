package com.sheltersleep;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Logica pura (nessuna dipendenza da Minecraft) che decide se un letto e' in un "riparo" valido.
 *
 * Idea: partendo dal letto si riempie (flood fill) lo spazio libero COPERTO da un tetto.
 * Le celle libere senza tetto sopra (cielo aperto) non fanno parte della stanza: un buco nel muro
 * che da' sull'esterno e' quindi una "finestra" e conta solo come apertura.
 */
public final class ShelterCheck {

    public enum Kind { FREE, SOLID, DOOR, BED }

    public enum Result { OK, NO_ROOF, NO_SPACE, NO_WALLS, NO_DOOR }

    public interface Blocks {
        Kind kindAt(int x, int y, int z);
        boolean outOfHeight(int y);
    }

    public record Settings(int minVolume, int maxVolume, int maxRadius, int maxHeight,
                           int roofRange, double maxOpenRatio, double maxRoofHoleRatio,
                           boolean requireRoof, boolean requireDoor) {
        public static Settings defaults() {
            return new Settings(18, 1200, 20, 12, 10, 0.25, 0.0, true, true);
        }
    }

    /** Senza tetto richiesto e letto a cielo aperto: soffitto virtuale alto (livelli sopra il letto). */
    private static final int LID_BAND = 2;

    /** Max celle esplorate per capire se un'area scoperta e' un buco nel tetto o l'esterno. */
    private static final int HOLE_CLUSTER_CAP = 150;

    private static final int[][] DIRS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}
    };

    private ShelterCheck() {}

    /** Ritorna true se sopra la cella (entro roofRange blocchi) c'e' qualcosa di solido. */
    private static boolean covered(Blocks w, int x, int y, int z, int range) {
        for (int i = 1; i <= range; i++) {
            int yy = y + i;
            if (w.outOfHeight(yy)) {
                return false;
            }
            if (w.kindAt(x, yy, z) != Kind.FREE) {
                return true;
            }
        }
        return false;
    }

    private static boolean coveredOrLid(Blocks w, int x, int y, int z, int range, boolean lid, int bedY) {
        if (lid && y >= bedY && y <= bedY + LID_BAND) {
            return true;
        }
        return covered(w, x, y, z, range);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) ((y + 2048) & 0xFFF);
    }

    /** Colonna (x,z) di una chiave: serve a contare le colonne di stanza e di buco. */
    private static long column(long k) {
        return k >>> 12;
    }

    /**
     * Esplora (solo in orizzontale, allo stesso livello) le celle libere senza tetto a partire da (x,y,z).
     * Se il gruppo resta piccolo e' chiuso da muri/stanza (buco nel tetto); se supera il limite e' l'esterno.
     */
    private static Set<Long> exploreUncovered(Blocks w, int x, int y, int z, int range, boolean lid, int bedY) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        seen.add(key(x, y, z));
        q.add(new int[]{x, y, z});
        while (!q.isEmpty() && seen.size() <= HOLE_CLUSTER_CAP) {
            int[] c = q.poll();
            for (int i = 0; i < 4; i++) {
                int nx = c[0] + DIRS[i][0], nz = c[2] + DIRS[i][2];
                long nk = key(nx, c[1], nz);
                if (seen.contains(nk)) continue;
                if (w.kindAt(nx, c[1], nz) != Kind.FREE) continue;
                if (coveredOrLid(w, nx, c[1], nz, range, lid, bedY)) continue;
                seen.add(nk);
                q.add(new int[]{nx, c[1], nz});
            }
        }
        return seen;
    }

    public static Result check(Blocks w, int bx, int by, int bz, Settings s) {
        // 1) Tetto sopra il letto (se richiesto). Se non e' richiesto e il letto e' a cielo aperto,
        //    si simula un soffitto sopra il letto e la stanza viene giudicata solo dai muri.
        boolean lid = false;
        if (!covered(w, bx, by, bz, s.roofRange())) {
            if (s.requireRoof()) {
                return Result.NO_ROOF;
            }
            lid = true;
        }

        // 2) Flood fill dello spazio libero coperto
        Set<Long> region = new HashSet<>();
        Set<Long> doors = new HashSet<>();
        Set<Long> holeCells = new HashSet<>();      // celle senza tetto ma interne alla stanza (buchi nel tetto)
        Set<Long> outdoorCells = new HashSet<>();   // celle senza tetto che danno sull'esterno
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        region.add(key(bx, by, bz));
        queue.add(new int[]{bx, by, bz});
        int open = 0, closed = 0, doorFaces = 0;
        boolean leaked = false;

        outer:
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : DIRS) {
                int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                boolean horizontal = d[1] == 0;
                if (w.outOfHeight(ny)) {
                    continue;
                }
                Kind k = w.kindAt(nx, ny, nz);
                if (k == Kind.DOOR) {
                    doors.add(key(nx, ny, nz));
                    if (horizontal) doorFaces++;
                    continue;
                }
                if (k == Kind.SOLID) {
                    if (horizontal) closed++;
                    continue;
                }
                long nk = key(nx, ny, nz);
                if (region.contains(nk)) {
                    continue;
                }
                if (coveredOrLid(w, nx, ny, nz, s.roofRange(), lid, by)) {
                    if (Math.abs(nx - bx) > s.maxRadius() || Math.abs(nz - bz) > s.maxRadius()
                            || Math.abs(ny - by) > s.maxHeight()) {
                        leaked = true;
                        continue;
                    }
                    region.add(nk);
                    queue.add(new int[]{nx, ny, nz});
                    if (region.size() > s.maxVolume()) {
                        leaked = true;
                        break outer;
                    }
                } else if (horizontal) {
                    // Aria libera ma SENZA tetto: puo' essere l'esterno (finestra/varco) oppure un buco nel tetto.
                    if (holeCells.contains(nk)) {
                        continue;
                    }
                    if (!outdoorCells.contains(nk)) {
                        Set<Long> cluster = exploreUncovered(w, nx, ny, nz, s.roofRange(), lid, by);
                        if (cluster.size() > HOLE_CLUSTER_CAP) {
                            outdoorCells.addAll(cluster);   // si espande all'infinito: e' l'esterno
                        } else {
                            holeCells.addAll(cluster);      // chiuso da muri/stanza: buco nel tetto
                            continue;
                        }
                    }
                    open++;
                }
            }
        }

        // 3) Verdetto (ordine dei messaggi: tetto -> pareti/spazio -> porta)
        // Buchi nel tetto dentro la stanza: contano come "manca il tetto", non come mancanza di spazio.
        if (s.requireRoof() && !holeCells.isEmpty()) {
            Set<Long> holeCols = new HashSet<>();
            Set<Long> roomCols = new HashSet<>();
            for (long k : holeCells) holeCols.add(column(k));
            for (long k : region) roomCols.add(column(k));
            double holeRatio = (double) holeCols.size() / (holeCols.size() + roomCols.size());
            if (holeRatio > s.maxRoofHoleRatio()) {
                return Result.NO_ROOF;
            }
        }
        if (leaked) {
            return Result.NO_WALLS;          // area enorme / aperta: non e' una stanza
        }
        if (region.size() + holeCells.size() < s.minVolume()) {
            return Result.NO_SPACE;
        }
        int total = open + closed + doorFaces;
        double ratio = total == 0 ? 0.0 : (double) open / total;
        if (ratio > s.maxOpenRatio()) {
            return Result.NO_WALLS;          // troppe aperture: non e' chiuso
        }
        if (s.requireDoor() && doors.isEmpty()) {
            return Result.NO_DOOR;
        }
        return Result.OK;
    }
}
