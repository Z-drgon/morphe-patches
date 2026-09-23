package app.template.extension.geometrydashlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Marks every official Geometry Dash Lite level 100% complete with all secret
 * coins by rewriting the game's own save file (CCGameManager.dat) before the
 * game loads it.
 *
 * Save pipeline: file bytes -> XOR 0x0B -> URL-safe Base64 -> gzip ->
 * cocos2d-x XML plist. There is no checksum, so the file can be freely
 * rewritten.
 *
 * Runs once (SharedPreferences marker). The algorithm is additive and
 * idempotent: re-running only fills in what is still missing, so existing
 * progress is never double-counted or destroyed. Any failure is swallowed so
 * the game always starts normally.
 */
public final class SaveCompleter {

    private static final String PREFS = "gdl_complete_all";
    private static final String DONE_KEY = "done_v1";
    private static final String SAVE_NAME = "CCGameManager.dat";

    /** Official level id (1..22) -> stars awarded on completion. */
    private static final int[] STARS = {
            0,
            1, 2, 3, 4, 5, 6,          // 1-6
            7, 8, 9, 10, 11, 12,       // 7-12
            10, 14, 12, 12, 10, 14,    // 13-18
            10, 15, 12, 12             // 19-22
    };

    /** Official demon levels: Clubstep (14), Theory of Everything 2 (18), Deadlocked (20). */
    private static final int[] DEMONS = {14, 18, 20};

    /**
     * Official level id -> which of the 3 secret coins exist. Most levels have
     * all three; the exceptions are documented per level.
     */
    private static final int[][] COINS = new int[23][];

    static {
        for (int id = 1; id <= 22; id++) {
            COINS[id] = new int[]{1, 2, 3};
        }
        COINS[11] = new int[]{1, 3}; // Clutterfunk
        COINS[14] = new int[]{1, 2}; // Clubstep
        COINS[15] = new int[]{2};    // Electrodynamix
        COINS[18] = new int[]{1, 3}; // Theory of Everything 2
    }

    private SaveCompleter() {
    }

    public static void run(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (prefs.getBoolean(DONE_KEY, false)) return;

            File save = new File(context.getFilesDir(), SAVE_NAME);
            // Fresh install: the game creates the save during native startup,
            // which happens after this runs. Skip and retry on next launch.
            if (!save.exists()) return;

            Dict root = decodeSave(readAll(save));
            if (completeAll(root)) {
                writeAll(save, encodeSave(root));
            }
            prefs.edit().putBoolean(DONE_KEY, true).apply();
        } catch (Throwable ignored) {
            // Never crash the game: a failed edit just means no completions.
        }
    }

    // ------------------------------------------------------------------ //
    // Completion logic                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Sets 100% + all coins for levels 1..22. Counters in GS_value are only
     * incremented for newly completed levels/coins, so legitimate existing
     * progress is preserved. Returns true if anything changed.
     */
    private static boolean completeAll(Dict root) {
        boolean changed = false;
        Dict gsValue = dict(root, "GS_value");
        Dict gsCompleted = dict(root, "GS_completed");
        Dict gs10 = dict(root, "GS_10");
        Dict glm01 = dict(root, "GLM_01");

        for (int id = 1; id <= 22; id++) {
            String key = Integer.toString(id);

            Node existing = gs10.map.get(key);
            boolean newlyCompleted = !(existing instanceof Str)
                    || !((Str) existing).value.equals("100");
            if (newlyCompleted) {
                gs10.map.put(key, new Str("100"));
                addCounter(gsValue, "3", 1); // total completed official levels
                addCounter(gsValue, "6", STARS[id]); // stars
                if (isDemon(id)) addCounter(gsValue, "5", 1); // demons
                gsCompleted.map.put("n_" + id, TrueNode.INSTANCE);
                gsCompleted.map.put("star_" + id, TrueNode.INSTANCE);
                if (isDemon(id)) gsCompleted.map.put("demon_" + id, TrueNode.INSTANCE);
                changed = true;
            }

            // GLM_01 holds the in-memory level objects; k19 = normal %,
            // k20 = practice %. A minimal entry is enough: the game fills in
            // static level data (name, stars, difficulty) from its hardcoded
            // tables.
            Dict entry = dict(glm01, key);
            Node k19 = entry.map.get("k19");
            if (!(k19 instanceof IntNum) || ((IntNum) k19).value != 100L) {
                entry.map.put("k19", new IntNum(100));
                changed = true;
            }
            Node k20 = entry.map.get("k20");
            if (!(k20 instanceof IntNum) || ((IntNum) k20).value != 100L) {
                entry.map.put("k20", new IntNum(100));
                changed = true;
            }

            for (int coin : COINS[id]) {
                String coinKey = "unique_" + id + "_" + coin;
                if (!gsValue.map.containsKey(coinKey)) {
                    gsValue.map.put(coinKey, new Str("1"));
                    addCounter(gsValue, "8", 1); // secret coins collected
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean isDemon(int id) {
        for (int demon : DEMONS) {
            if (demon == id) return true;
        }
        return false;
    }

    private static void addCounter(Dict gsValue, String key, long delta) {
        Node node = gsValue.map.get(key);
        long current = 0;
        if (node instanceof Str) {
            try {
                current = Long.parseLong(((Str) node).value);
            } catch (NumberFormatException ignored) {
                current = 0;
            }
        }
        gsValue.map.put(key, new Str(Long.toString(current + delta)));
    }

    private static Dict dict(Dict parent, String key) {
        Node existing = parent.map.get(key);
        if (existing instanceof Dict) return (Dict) existing;
        Dict created = new Dict();
        parent.map.put(key, created);
        return created;
    }

    // ------------------------------------------------------------------ //
    // Save encode / decode                                               //
    // ------------------------------------------------------------------ //

    private static Dict decodeSave(byte[] bytes) throws Exception {
        byte[] xored = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            xored[i] = (byte) (bytes[i] ^ 0x0B);
        }
        // Normalize both Base64 alphabets before decoding.
        String b64 = new String(xored, StandardCharsets.US_ASCII).trim()
                .replace('-', '+')
                .replace('_', '/');
        byte[] gzipped = Base64.decode(b64, Base64.DEFAULT);
        byte[] xml = gunzip(gzipped);
        return parsePlist(new String(xml, StandardCharsets.UTF_8));
    }

    private static byte[] encodeSave(Dict root) throws Exception {
        StringBuilder sb = new StringBuilder();
        serialize(root, sb);
        byte[] deflated = gzip(sb.toString().getBytes(StandardCharsets.UTF_8));
        String b64 = Base64.encodeToString(deflated, Base64.URL_SAFE | Base64.NO_WRAP);
        byte[] ascii = b64.getBytes(StandardCharsets.US_ASCII);
        byte[] xored = new byte[ascii.length];
        for (int i = 0; i < ascii.length; i++) {
            xored[i] = (byte) (ascii[i] ^ 0x0B);
        }
        return xored;
    }

    private static byte[] gunzip(byte[] gzipped) throws Exception {
        InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static byte[] gzip(byte[] raw) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputStream gzip = new GZIPOutputStream(buf);
        try {
            gzip.write(raw);
        } finally {
            gzip.close();
        }
        return buf.toByteArray();
    }

    private static byte[] readAll(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void writeAll(File file, byte[] bytes) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    // ------------------------------------------------------------------ //
    // Minimal cocos2d-x plist model: <d>, <k>, <s>, <i>, <t/>, <f/>       //
    // ------------------------------------------------------------------ //

    private interface Node {
    }

    private static final class Dict implements Node {
        final LinkedHashMap<String, Node> map = new LinkedHashMap<>();
    }

    private static final class Str implements Node {
        final String value;

        Str(String value) {
            this.value = value;
        }
    }

    private static final class IntNum implements Node {
        final long value;

        IntNum(long value) {
            this.value = value;
        }
    }

    private static final class TrueNode implements Node {
        static final TrueNode INSTANCE = new TrueNode();

        private TrueNode() {
        }
    }

    private static final class FalseNode implements Node {
        static final FalseNode INSTANCE = new FalseNode();

        private FalseNode() {
        }
    }

    private static Dict parsePlist(String xml) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new java.io.StringReader(xml));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.getName().equals("d")) {
                return parseDict(parser);
            }
            event = parser.next();
        }
        throw new IllegalArgumentException("plist has no root <d>");
    }

    /** Entered with the parser on START_TAG "d"; leaves it on END_TAG "d". */
    private static Dict parseDict(XmlPullParser parser) throws Exception {
        LinkedHashMap<String, Node> map = new LinkedHashMap<>();
        int event = parser.nextTag();
        while (!(event == XmlPullParser.END_TAG && parser.getName().equals("d"))) {
            if (!(event == XmlPullParser.START_TAG && parser.getName().equals("k"))) {
                throw new IllegalArgumentException("expected <k> in dict");
            }
            String key = parser.nextText(); // consumes </k>
            event = parser.nextTag(); // START_TAG of the value
            String name = parser.getName();
            Node value;
            if (name.equals("d")) {
                value = parseDict(parser);
            } else if (name.equals("s")) {
                value = new Str(parser.nextText());
            } else if (name.equals("i")) {
                value = new IntNum(Long.parseLong(parser.nextText().trim()));
            } else if (name.equals("t")) {
                parser.nextTag(); // consume <t/>
                value = TrueNode.INSTANCE;
            } else if (name.equals("f")) {
                parser.nextTag(); // consume <f/>
                value = FalseNode.INSTANCE;
            } else {
                throw new IllegalArgumentException("unexpected <" + name + ">");
            }
            map.put(key, value);
            event = parser.nextTag();
        }
        Dict dict = new Dict();
        dict.map.putAll(map);
        return dict;
    }

    private static void serialize(Node node, StringBuilder sb) {
        if (node instanceof Dict) {
            sb.append("<d>");
            for (java.util.Map.Entry<String, Node> e : ((Dict) node).map.entrySet()) {
                sb.append("<k>").append(escape(e.getKey())).append("</k>");
                serialize(e.getValue(), sb);
            }
            sb.append("</d>");
        } else if (node instanceof Str) {
            sb.append("<s>").append(escape(((Str) node).value)).append("</s>");
        } else if (node instanceof IntNum) {
            sb.append("<i>").append(((IntNum) node).value).append("</i>");
        } else if (node instanceof TrueNode) {
            sb.append("<t/>");
        } else if (node instanceof FalseNode) {
            sb.append("<f/>");
        } else {
            throw new IllegalArgumentException("unknown node");
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
