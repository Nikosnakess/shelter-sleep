import com.sheltersleep.ShelterCheck;
import com.sheltersleep.ShelterCheck.Kind;
import com.sheltersleep.ShelterCheck.Result;
import java.util.HashMap;
import java.util.Map;

/** Test dell'algoritmo con stanze finte. Esegui:  java -cp out tests/ShelterCheckTest.java  (vedi README) */
public class ShelterCheckTest {
    static class World implements ShelterCheck.Blocks {
        final Map<Long, Kind> m = new HashMap<>();
        final Kind below, above;
        World(Kind belowGround) { this.below = belowGround; this.above = Kind.FREE; }
        static long k(int x, int y, int z) { return ((long) x << 40) ^ ((long) z << 16) ^ (y + 100); }
        public Kind kindAt(int x, int y, int z) {
            Kind v = m.get(k(x, y, z));
            if (v != null) return v;
            return y <= 0 ? below : above;
        }
        public boolean outOfHeight(int y) { return y < -64 || y > 320; }
        void set(int x, int y, int z, Kind kind) { m.put(k(x, y, z), kind); }
        void box(int x1, int x2, int y1, int y2, int z1, int z2, Kind kind) {
            for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) set(x, y, z, kind);
        }
    }

    /** Casa con interno 4x4, alta 3, tetto, porta a (1,*,-1). */
    static World house() {
        World w = new World(Kind.SOLID);
        w.box(-1, 4, 1, 3, -1, 4, Kind.SOLID);      // blocco pieno
        w.box(0, 3, 1, 3, 0, 3, Kind.FREE);         // svuota l'interno
        w.box(-1, 4, 4, 4, -1, 4, Kind.SOLID);      // tetto
        w.set(1, 1, -1, Kind.DOOR); w.set(1, 2, -1, Kind.DOOR);
        w.set(2, 1, 2, Kind.BED); w.set(3, 1, 2, Kind.BED);
        return w;
    }


    /** Stanza con interno 5x5 (x,z 0..4), alta 3, tetto a y=4, porta a (1,*,-1), letto in (1,1,1). */
    static World room5() {
        World w = new World(Kind.SOLID);
        w.box(-1, 5, 1, 3, -1, 5, Kind.SOLID);
        w.box(0, 4, 1, 3, 0, 4, Kind.FREE);
        w.box(-1, 5, 4, 4, -1, 5, Kind.SOLID);
        w.set(1, 1, -1, Kind.DOOR); w.set(1, 2, -1, Kind.DOOR);
        w.set(1, 1, 1, Kind.BED); w.set(2, 1, 1, Kind.BED);
        return w;
    }
    static Result runBig(World w) { return ShelterCheck.check(w, 1, 1, 1, ShelterCheck.Settings.defaults()); }

    static int fails = 0;
    static void expect(String name, Result got, Result want) {
        boolean ok = got == want;
        if (!ok) fails++;
        System.out.printf("%-58s %-9s %s%n", name, got, ok ? "OK" : "<-- ATTESO " + want);
    }
    static Result run(World w) { return ShelterCheck.check(w, 2, 1, 2, ShelterCheck.Settings.defaults()); }

    public static void main(String[] a) {
        World w;

        w = new World(Kind.SOLID); w.set(2, 1, 2, Kind.BED);
        expect("Letto in campo aperto", run(w), Result.NO_ROOF);

        expect("Casa 4x4 con porta, senza finestre", run(house()), Result.OK);

        w = house(); w.set(-1, 2, 1, Kind.FREE);
        expect("Casa 4x4 con porta + finestra (buco 1x1)", run(w), Result.OK);

        w = house(); w.box(-1, -1, 2, 3, 0, 2, Kind.FREE); // finestra grande 3x2 = 6 facce
        expect("Casa con finestra grande 3x2", run(w), Result.OK);

        w = house(); w.set(1, 1, -1, Kind.SOLID); w.set(1, 2, -1, Kind.SOLID);
        expect("Casa chiusa senza porta (muro pieno)", run(w), Result.NO_DOOR);

        w = house(); w.set(1, 1, -1, Kind.FREE); w.set(1, 2, -1, Kind.FREE);
        expect("Casa con varco vuoto al posto della porta", run(w), Result.NO_DOOR);

        w = house(); w.set(1, 4, -1, Kind.SOLID); w.set(2, 4, 2, Kind.FREE);
        expect("Buco nel tetto sopra il letto", run(w), Result.NO_ROOF);

        w = house(); w.set(0, 4, 0, Kind.FREE);
        expect("Buco nel tetto in un angolo (letto coperto)", run(w), Result.NO_ROOF);

        w = house(); w.box(-1, -1, 1, 3, -1, 4, Kind.FREE); w.box(4, 4, 1, 3, -1, 4, Kind.FREE);
        expect("Due pareti intere mancanti (50% aperto)", run(w), Result.NO_WALLS);

        w = new World(Kind.SOLID); w.box(-1, 4, 4, 4, -1, 4, Kind.SOLID);
        w.set(1, 1, -1, Kind.DOOR); w.set(2, 1, 2, Kind.BED);
        expect("Tettoia 6x6 senza pareti + porta isolata", run(w), Result.NO_WALLS);

        w = new World(Kind.SOLID);
        w.box(-1, 2, 1, 2, -1, 2, Kind.SOLID); w.box(0, 1, 1, 1, 0, 1, Kind.FREE);
        w.box(-1, 2, 3, 3, -1, 2, Kind.SOLID);
        w.set(0, 1, -1, Kind.DOOR); w.set(0, 2, -1, Kind.DOOR); w.set(0, 1, 0, Kind.BED); w.set(1, 1, 0, Kind.BED);
        expect("Stanzino 2x2 alto 1 con porta", ShelterCheck.check(w, 0, 1, 0, ShelterCheck.Settings.defaults()), Result.NO_SPACE);

        w = new World(Kind.SOLID); w.box(-30, 30, 1, 10, -30, 30, Kind.SOLID); w.box(-25, 25, 1, 8, -25, 25, Kind.FREE);
        w.set(2, 1, 2, Kind.BED);
        expect("Caverna enorme (aperta) senza porta", run(w), Result.NO_WALLS);

        w = house(); w.set(0, 1, 0, Kind.SOLID); w.set(0, 2, 0, Kind.SOLID); w.set(3, 1, 0, Kind.SOLID); // bauli, tavolo
        expect("Casa con mobili (blocchi solidi interni)", run(w), Result.OK);

        w = house(); w.set(2, 1, -1, Kind.SOLID); w.set(2, 2, -1, Kind.SOLID); // pannelli di vetro = solidi
        expect("Casa con vetro/sbarre come finestra", run(w), Result.OK);

        w = house(); w.box(0, 3, 0, 0, 0, 3, Kind.SOLID); // pavimento 'rialzato' di 1 (moquette-like)
        w.set(2, 1, 2, Kind.BED);
        expect("Casa con pavimento rialzato", run(w), Result.OK);

        // Mini-stanza minima accettata: 3x3 interno, alta 2 (18 celle)
        w = new World(Kind.SOLID); w.box(-1, 3, 1, 3, -1, 3, Kind.SOLID); w.box(0, 2, 1, 2, 0, 2, Kind.FREE);
        w.set(1, 1, -1, Kind.DOOR); w.set(1, 2, -1, Kind.DOOR); w.set(1, 1, 1, Kind.BED); w.set(2, 1, 1, Kind.BED);
        expect("Stanza minima 3x3x2", ShelterCheck.check(w, 1, 1, 1, ShelterCheck.Settings.defaults()), Result.OK);


        // --- Stanza 5x5 (come il test reale), alta 3, porta, letto in un angolo ---
        World big = room5();
        expect("Stanza 5x5 intera, con porta", runBig(big), Result.OK);

        w = room5(); w.set(4, 4, 4, Kind.FREE);
        expect("5x5: 1 blocco di tetto rotto lontano dal letto", runBig(w), Result.NO_ROOF);

        w = room5(); w.box(3, 4, 4, 4, 3, 4, Kind.FREE);
        expect("5x5: 4 blocchi di tetto rotti lontano dal letto", runBig(w), Result.NO_ROOF);

        w = room5(); w.box(0, 4, 4, 4, 0, 1, Kind.FREE);
        expect("5x5: due file di tetto mancanti", runBig(w), Result.NO_ROOF);

        w = room5(); w.set(2, 4, 4, Kind.FREE); w.set(2, 3, 4, Kind.FREE);
        w.set(-1, 4, -1, Kind.FREE);
        expect("5x5: buco tetto + angolo del tetto sopra il muro", runBig(w), Result.NO_ROOF);

        w = room5(); w.set(-1, 4, -1, Kind.FREE); w.set(5, 4, 5, Kind.FREE);
        expect("5x5: tolti blocchi di tetto SOPRA i muri (stanza coperta)", runBig(w), Result.OK);

        w = room5(); w.set(4, 4, 4, Kind.FREE);
        expect("5x5: buco nel tetto ma ammesso (soglia 20%)",
                ShelterCheck.check(w, 1, 1, 1, new ShelterCheck.Settings(18, 1200, 20, 12, 10, 0.25, 0.20, true, true)), Result.OK);

        w = room5(); w.set(-1, 2, 2, Kind.FREE);
        expect("5x5: finestra nel muro (buco 1x1)", runBig(w), Result.OK);

        w = room5(); w.set(-1, 2, 2, Kind.FREE); w.set(4, 4, 4, Kind.FREE);
        expect("5x5: finestra + buco nel tetto", runBig(w), Result.NO_ROOF);

        w = room5(); w.set(4, 4, 4, Kind.FREE); w.set(1, 1, -1, Kind.SOLID); w.set(1, 2, -1, Kind.SOLID);
        expect("5x5: buco nel tetto E senza porta (prima il tetto)", runBig(w), Result.NO_ROOF);

        w = room5(); w.box(0, 4, 4, 4, 0, 4, Kind.FREE);
        expect("5x5: tetto completamente assente", runBig(w), Result.NO_ROOF);


        // --- Interruttori: tetto / porta / dimensione minima ---
        ShelterCheck.Settings noDoor = new ShelterCheck.Settings(18, 1200, 20, 12, 10, 0.25, 0.0, true, false);
        ShelterCheck.Settings noRoof = new ShelterCheck.Settings(18, 1200, 20, 12, 10, 0.25, 0.0, false, true);
        ShelterCheck.Settings neither = new ShelterCheck.Settings(18, 1200, 20, 12, 10, 0.25, 0.0, false, false);

        w = room5(); w.set(1, 1, -1, Kind.SOLID); w.set(1, 2, -1, Kind.SOLID);
        expect("[porta OFF] stanza senza porta", ShelterCheck.check(w, 1, 1, 1, noDoor), Result.OK);
        expect("[porta ON]  stessa stanza senza porta", runBig(w), Result.NO_DOOR);

        w = room5(); w.set(1, 1, -1, Kind.FREE); w.set(1, 2, -1, Kind.FREE);
        expect("[porta OFF] varco vuoto al posto della porta", ShelterCheck.check(w, 1, 1, 1, noDoor), Result.OK);

        w = room5(); w.set(4, 4, 4, Kind.FREE);
        expect("[tetto OFF] casa col tetto bucato", ShelterCheck.check(w, 1, 1, 1, noRoof), Result.OK);

        w = room5(); w.set(4, 4, 4, Kind.FREE); w.set(-1, 2, 2, Kind.FREE);
        expect("[tetto OFF] casa col tetto bucato + finestra", ShelterCheck.check(w, 1, 1, 1, noRoof), Result.OK);

        w = room5(); w.box(-1, 5, 4, 4, -1, 5, Kind.FREE);
        expect("[tetto OFF] stanza murata senza tetto, con porta", ShelterCheck.check(w, 1, 1, 1, noRoof), Result.OK);
        expect("[tetto ON]  stessa stanza senza tetto", runBig(w), Result.NO_ROOF);

        w = room5(); w.box(-1, 5, 4, 4, -1, 5, Kind.FREE); w.set(1, 1, -1, Kind.SOLID); w.set(1, 2, -1, Kind.SOLID);
        expect("[tetto OFF] murata senza tetto E senza porta", ShelterCheck.check(w, 1, 1, 1, noRoof), Result.NO_DOOR);
        expect("[tetto OFF + porta OFF] murata, niente tetto, niente porta", ShelterCheck.check(w, 1, 1, 1, neither), Result.OK);

        w = new World(Kind.SOLID); w.set(2, 1, 2, Kind.BED);
        expect("[tetto OFF] letto in campo aperto, nessun muro", ShelterCheck.check(w, 2, 1, 2, noRoof), Result.NO_WALLS);

        w = new World(Kind.SOLID); w.set(2, 1, 2, Kind.BED); w.set(1, 1, 1, Kind.DOOR);
        expect("[tetto+porta OFF] letto in campo aperto, nessun muro", ShelterCheck.check(w, 2, 1, 2, neither), Result.NO_WALLS);

        w = room5(); w.box(-1, 5, 4, 4, -1, 5, Kind.FREE); w.set(-1, 2, 2, Kind.FREE);
        expect("[tetto OFF] murata senza tetto + finestra (limite noto)", ShelterCheck.check(w, 1, 1, 1, noRoof), Result.NO_WALLS);

        // Dimensione minima configurabile: stanza 2x2 interna, alta 2 = 8 celle
        w = new World(Kind.SOLID); w.box(-1, 2, 1, 3, -1, 2, Kind.SOLID); w.box(0, 1, 1, 2, 0, 1, Kind.FREE);
        w.set(0, 1, -1, Kind.DOOR); w.set(0, 2, -1, Kind.DOOR); w.set(0, 1, 0, Kind.BED); w.set(1, 1, 0, Kind.BED);
        expect("[minimo 18] stanzetta 2x2x2 (8 celle)", ShelterCheck.check(w, 0, 1, 0, ShelterCheck.Settings.defaults()), Result.NO_SPACE);
        expect("[minimo 8]  stessa stanzetta 2x2x2",
                ShelterCheck.check(w, 0, 1, 0, new ShelterCheck.Settings(8, 1200, 20, 12, 10, 0.25, 0.0, true, true)), Result.OK);
        expect("[minimo 9]  stessa stanzetta 2x2x2",
                ShelterCheck.check(w, 0, 1, 0, new ShelterCheck.Settings(9, 1200, 20, 12, 10, 0.25, 0.0, true, true)), Result.NO_SPACE);

        System.out.println(fails == 0 ? "\nTUTTI I TEST OK" : "\nTEST FALLITI: " + fails);
        if (fails > 0) System.exit(1);
    }
}
