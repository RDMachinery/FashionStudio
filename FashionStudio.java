import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fashion Studio — a dress-up design studio for fashion lovers.
 *
 * Choose garments, colours and prints for a fashion-illustration model,
 * get friendly styling notes on your colour palette and print mixing,
 * save looks to a lookbook and export them as pictures.
 *
 * Run with:  java FashionStudio.java
 *
 * Copyright Mario Gianota 2026. All rights reserved.
 */
public class FashionStudio {

    static final String VERSION = "1.0";

    // =====================================================================
    //  Colours
    // =====================================================================

    record NamedColor(String name, Color color, boolean neutral) {}

    static final List<NamedColor> PALETTE = new ArrayList<>();
    static void nc(String n, int rgb, boolean neutral) { PALETTE.add(new NamedColor(n, new Color(rgb), neutral)); }
    static {
        nc("Black", 0x1C1C1E, true);     nc("Charcoal", 0x3E4047, true);  nc("Grey marl", 0x9B9B9F, true);
        nc("Stone", 0xCFC6B8, true);     nc("White", 0xFBFBF8, true);     nc("Ivory", 0xF5EEDC, true);
        nc("Cream", 0xEADCBE, true);     nc("Beige", 0xD6BF9C, true);     nc("Camel", 0xC0955F, true);
        nc("Cognac", 0x9A5B2E, true);    nc("Chocolate", 0x56362A, true); nc("Khaki", 0xA59A6C, true);
        nc("Navy", 0x1F2A4A, true);      nc("Denim blue", 0x4A6A93, true);
        nc("Olive", 0x666B38, false);    nc("Sage", 0xA2B08C, false);     nc("Emerald", 0x1C7A55, false);
        nc("Teal", 0x1F6E78, false);     nc("Mint", 0xBFE3D2, false);     nc("Sky blue", 0x9FC6E7, false);
        nc("Cobalt", 0x2447A6, false);   nc("Lilac", 0xC4B3E0, false);    nc("Plum", 0x5D2A4E, false);
        nc("Fuchsia", 0xD0307B, false);  nc("Blush", 0xF1C6C3, false);    nc("Coral", 0xF07D69, false);
        nc("Red", 0xC4202F, false);      nc("Burgundy", 0x6B1D2C, false); nc("Rust", 0xB0532C, false);
        nc("Terracotta", 0xC56B4B, false); nc("Mustard", 0xD3A12C, false); nc("Butter", 0xF3E2A0, false);
    }

    static final List<NamedColor> SKIN = List.of(
            new NamedColor("Fair", new Color(0xF6D9C6), false),
            new NamedColor("Light", new Color(0xEBC1A1), false),
            new NamedColor("Medium", new Color(0xD5A079), false),
            new NamedColor("Tan", new Color(0xB67C54), false),
            new NamedColor("Brown", new Color(0x8C5A3B), false),
            new NamedColor("Deep", new Color(0x5C3A27), false));

    static final List<NamedColor> HAIR_COLOURS = List.of(
            new NamedColor("Black", new Color(0x1A1414), false),
            new NamedColor("Dark brown", new Color(0x3B2418), false),
            new NamedColor("Chestnut", new Color(0x6B3A22), false),
            new NamedColor("Auburn", new Color(0x8C3B1E), false),
            new NamedColor("Honey blonde", new Color(0xC99A55), false),
            new NamedColor("Platinum", new Color(0xE6DCC4), false),
            new NamedColor("Silver", new Color(0xB8B8BE), false),
            new NamedColor("Pink", new Color(0xE58FB0), false));

    static final String[] HAIR_STYLES = {"Long waves", "Sleek bob", "Top bun", "Pixie crop", "Curls", "Ponytail"};
    static final String[] BACKDROPS = {"Studio", "Runway", "Paris", "Blush paper", "Plain white"};

    static Color col(String name) {
        for (NamedColor c : PALETTE) if (c.name().equals(name)) return c.color();
        throw new IllegalArgumentException(name);
    }

    static double dist(Color a, Color b) {
        int dr = a.getRed() - b.getRed(), dg = a.getGreen() - b.getGreen(), db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr * 0.3 + dg * dg * 0.59 + db * db * 0.11) * 1.7;
    }

    static NamedColor nearest(Color c) {
        NamedColor best = PALETTE.get(0);
        for (NamedColor n : PALETTE) if (dist(c, n.color()) < dist(c, best.color())) best = n;
        return best;
    }

    static String colourName(Color c) {
        NamedColor n = nearest(c);
        return dist(c, n.color()) > 28 ? n.name() + "-ish" : n.name();
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    static double lum(Color c) { return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0; }

    static String hex(Color c) { return String.format("#%06X", c.getRGB() & 0xFFFFFF); }

    // =====================================================================
    //  Wardrobe model
    // =====================================================================

    enum Print {
        SOLID("Solid"), STRIPES("Breton stripes"), PINSTRIPE("Pinstripe"), POLKA("Polka dots"),
        GINGHAM("Gingham"), TARTAN("Tartan"), HOUNDSTOOTH("Houndstooth"), HERRINGBONE("Herringbone"),
        LEOPARD("Leopard"), FLORAL("Ditsy floral"), DENIM("Denim"), KNIT("Rib knit");
        final String label;
        Print(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    enum Cat {
        TOP("Top", "None", "T-shirt", "Blouse", "Turtleneck", "Crop top", "Camisole"),
        BOTTOM("Bottoms", "None", "Straight trousers", "Wide-leg trousers", "Skinny jeans", "Shorts",
                "A-line skirt", "Pencil skirt", "Mini skirt", "Pleated midi skirt"),
        DRESS("Dress", "None", "Shift dress", "Fit-and-flare dress", "Maxi dress", "Slip dress", "Wrap dress"),
        OUTER("Outerwear", "None", "Blazer", "Trench coat", "Cropped jacket", "Cardigan", "Long coat"),
        SHOES("Shoes", "Barefoot", "Court heels", "Ankle boots", "Knee boots", "Trainers", "Ballet flats", "Loafers"),
        BAG("Bag", "None", "Crossbody", "Tote", "Clutch"),
        HAT("Hat", "None", "Beret", "Sun hat", "Fedora", "Beanie");

        final String label;
        final String[] styles;
        Cat(String label, String... styles) { this.label = label; this.styles = styles; }
        int index(String style) {
            for (int i = 0; i < styles.length; i++) if (styles[i].equals(style)) return i;
            throw new IllegalArgumentException(style);
        }
    }

    static final class Piece {
        int style;
        Color c1, c2;
        Print print;
        Piece(int style, Color c1, Color c2, Print print) { this.style = style; this.c1 = c1; this.c2 = c2; this.print = print; }
        Piece copy() { return new Piece(style, c1, c2, print); }
    }

    static final class Outfit {
        String name = "Untitled look";
        final Piece[] pieces = new Piece[Cat.values().length];
        int skin = 1, hair = 0, backdrop = 0;
        Color hairColor = HAIR_COLOURS.get(1).color();
        boolean tuck, belt, sunglasses, pearls;

        Outfit() {
            for (Cat c : Cat.values()) pieces[c.ordinal()] = new Piece(0, col("Black"), col("Ivory"), Print.SOLID);
        }

        Piece get(Cat c) { return pieces[c.ordinal()]; }
        String style(Cat c) { return c.styles[get(c).style]; }
        boolean has(Cat c) { return get(c).style != 0; }

        Outfit copy() {
            Outfit o = new Outfit();
            o.name = name; o.skin = skin; o.hair = hair; o.backdrop = backdrop; o.hairColor = hairColor;
            o.tuck = tuck; o.belt = belt; o.sunglasses = sunglasses; o.pearls = pearls;
            for (int i = 0; i < pieces.length; i++) o.pieces[i] = pieces[i].copy();
            return o;
        }

        String toLine() {
            StringBuilder sb = new StringBuilder();
            sb.append("name=").append(name.replace('|', '/').replace('\n', ' ').replace('\r', ' '));
            sb.append("|skin=").append(skin).append("|hair=").append(hair).append("|hairColor=").append(hex(hairColor));
            sb.append("|backdrop=").append(backdrop).append("|flags=")
              .append(tuck ? "T" : "-").append(belt ? "B" : "-").append(sunglasses ? "S" : "-").append(pearls ? "P" : "-");
            for (Cat c : Cat.values()) {
                Piece p = get(c);
                sb.append('|').append(c.name()).append('=').append(p.style).append(',')
                  .append(hex(p.c1)).append(',').append(hex(p.c2)).append(',').append(p.print.name());
            }
            return sb.toString();
        }

        static Outfit parse(String line) {
            Outfit o = new Outfit();
            for (String part : line.split("\\|")) {
                int i = part.indexOf('=');
                if (i < 0) continue;
                String k = part.substring(0, i), v = part.substring(i + 1);
                try {
                    switch (k) {
                        case "name" -> o.name = v;
                        case "skin" -> o.skin = clamp(Integer.parseInt(v), 0, SKIN.size() - 1);
                        case "hair" -> o.hair = clamp(Integer.parseInt(v), 0, HAIR_STYLES.length - 1);
                        case "hairColor" -> o.hairColor = Color.decode(v);
                        case "backdrop" -> o.backdrop = clamp(Integer.parseInt(v), 0, BACKDROPS.length - 1);
                        case "flags" -> {
                            o.tuck = v.contains("T"); o.belt = v.contains("B");
                            o.sunglasses = v.contains("S"); o.pearls = v.contains("P");
                        }
                        default -> {
                            Cat c = Cat.valueOf(k);
                            String[] f = v.split(",");
                            Piece p = o.get(c);
                            p.style = clamp(Integer.parseInt(f[0]), 0, c.styles.length - 1);
                            p.c1 = Color.decode(f[1]);
                            p.c2 = Color.decode(f[2]);
                            p.print = Print.valueOf(f[3]);
                        }
                    }
                } catch (Exception ignored) { /* skip anything unreadable */ }
            }
            return o;
        }
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    static Outfit defaultOutfit() {
        Outfit o = new Outfit();
        wear(o, Cat.TOP, "Blouse", "White", "Navy", Print.SOLID);
        wear(o, Cat.BOTTOM, "Straight trousers", "Navy", "Ivory", Print.SOLID);
        wear(o, Cat.SHOES, "Ballet flats", "Black", "White", Print.SOLID);
        wear(o, Cat.BAG, "Crossbody", "Cognac", "Ivory", Print.SOLID);
        o.tuck = true;
        o.name = "My first look";
        return o;
    }

    // =====================================================================
    //  Style presets
    // =====================================================================

    static final String[] PRESETS = {"Parisian Chic", "Minimalist", "Boho", "Preppy", "Y2K", "Dark Academia",
            "Quiet Luxury", "Sixties Mod", "Wild Thing", "Coastal Summer"};

    static void wear(Outfit o, Cat c, String style, String c1, String c2, Print pr) {
        Piece p = o.get(c);
        p.style = c.index(style); p.c1 = col(c1); p.c2 = col(c2); p.print = pr;
    }

    static void clearPieces(Outfit o) {
        for (Cat c : Cat.values()) { o.get(c).style = 0; o.get(c).print = Print.SOLID; }
        o.tuck = o.belt = o.sunglasses = o.pearls = false;
    }

    /** Dresses the model in a named aesthetic. Skin tone and hair colour are left alone. */
    static void applyPreset(Outfit o, String name) {
        clearPieces(o);
        o.name = name;
        switch (name) {
            case "Parisian Chic" -> {
                wear(o, Cat.TOP, "T-shirt", "Ivory", "Navy", Print.STRIPES);
                wear(o, Cat.BOTTOM, "Straight trousers", "Denim blue", "Ivory", Print.DENIM);
                wear(o, Cat.OUTER, "Trench coat", "Beige", "Cognac", Print.SOLID);
                wear(o, Cat.SHOES, "Ballet flats", "Black", "Ivory", Print.SOLID);
                wear(o, Cat.BAG, "Crossbody", "Black", "Ivory", Print.SOLID);
                wear(o, Cat.HAT, "Beret", "Black", "Black", Print.SOLID);
                o.hair = 1; o.tuck = true; o.backdrop = 2;
            }
            case "Minimalist" -> {
                wear(o, Cat.TOP, "Turtleneck", "Black", "Ivory", Print.SOLID);
                wear(o, Cat.BOTTOM, "Wide-leg trousers", "Camel", "Ivory", Print.SOLID);
                wear(o, Cat.SHOES, "Loafers", "Black", "White", Print.SOLID);
                wear(o, Cat.BAG, "Tote", "Black", "Ivory", Print.SOLID);
                o.hair = 2; o.tuck = true; o.belt = true; o.backdrop = 0;
            }
            case "Boho" -> {
                wear(o, Cat.DRESS, "Maxi dress", "Terracotta", "Cream", Print.FLORAL);
                wear(o, Cat.OUTER, "Cardigan", "Cream", "Cream", Print.KNIT);
                wear(o, Cat.SHOES, "Ankle boots", "Cognac", "Cream", Print.SOLID);
                wear(o, Cat.BAG, "Crossbody", "Cognac", "Cream", Print.SOLID);
                wear(o, Cat.HAT, "Sun hat", "Beige", "Terracotta", Print.SOLID);
                o.hair = 0; o.belt = true; o.backdrop = 3;
            }
            case "Preppy" -> {
                wear(o, Cat.TOP, "Blouse", "White", "Navy", Print.SOLID);
                wear(o, Cat.BOTTOM, "A-line skirt", "Navy", "Emerald", Print.TARTAN);
                wear(o, Cat.OUTER, "Blazer", "Navy", "Ivory", Print.SOLID);
                wear(o, Cat.SHOES, "Loafers", "Burgundy", "White", Print.SOLID);
                wear(o, Cat.BAG, "Tote", "Cognac", "Ivory", Print.SOLID);
                o.hair = 5; o.tuck = true; o.pearls = true; o.backdrop = 0;
            }
            case "Y2K" -> {
                wear(o, Cat.TOP, "Crop top", "Blush", "White", Print.SOLID);
                wear(o, Cat.BOTTOM, "Mini skirt", "Denim blue", "White", Print.DENIM);
                wear(o, Cat.SHOES, "Trainers", "White", "Fuchsia", Print.SOLID);
                wear(o, Cat.BAG, "Crossbody", "Lilac", "White", Print.SOLID);
                o.hair = 2; o.sunglasses = true; o.backdrop = 1;
            }
            case "Dark Academia" -> {
                wear(o, Cat.TOP, "Turtleneck", "Chocolate", "Ivory", Print.SOLID);
                wear(o, Cat.BOTTOM, "Pleated midi skirt", "Stone", "Charcoal", Print.HOUNDSTOOTH);
                wear(o, Cat.OUTER, "Blazer", "Charcoal", "Grey marl", Print.HERRINGBONE);
                wear(o, Cat.SHOES, "Loafers", "Black", "White", Print.SOLID);
                wear(o, Cat.BAG, "Crossbody", "Chocolate", "Ivory", Print.SOLID);
                o.hair = 0; o.tuck = true; o.backdrop = 0;
            }
            case "Quiet Luxury" -> {
                wear(o, Cat.TOP, "Blouse", "Cream", "Ivory", Print.SOLID);
                wear(o, Cat.BOTTOM, "Straight trousers", "Camel", "Ivory", Print.SOLID);
                wear(o, Cat.OUTER, "Cardigan", "Navy", "Navy", Print.KNIT);
                wear(o, Cat.SHOES, "Court heels", "Camel", "Ivory", Print.SOLID);
                wear(o, Cat.BAG, "Tote", "Camel", "Ivory", Print.SOLID);
                o.hair = 1; o.tuck = true; o.belt = true; o.pearls = true; o.backdrop = 0;
            }
            case "Sixties Mod" -> {
                wear(o, Cat.DRESS, "Shift dress", "White", "Black", Print.POLKA);
                wear(o, Cat.SHOES, "Knee boots", "White", "Black", Print.SOLID);
                wear(o, Cat.BAG, "Clutch", "Black", "White", Print.SOLID);
                o.hair = 1; o.backdrop = 1;
            }
            case "Wild Thing" -> {
                wear(o, Cat.DRESS, "Slip dress", "Camel", "Black", Print.LEOPARD);
                wear(o, Cat.OUTER, "Long coat", "Black", "Ivory", Print.SOLID);
                wear(o, Cat.SHOES, "Court heels", "Red", "Black", Print.SOLID);
                wear(o, Cat.BAG, "Clutch", "Red", "Black", Print.SOLID);
                o.hair = 4; o.sunglasses = true; o.backdrop = 1;
            }
            default -> { // Coastal Summer
                wear(o, Cat.TOP, "Camisole", "White", "Ivory", Print.SOLID);
                wear(o, Cat.BOTTOM, "Wide-leg trousers", "Ivory", "Sky blue", Print.PINSTRIPE);
                wear(o, Cat.SHOES, "Ballet flats", "Beige", "White", Print.SOLID);
                wear(o, Cat.BAG, "Tote", "Beige", "Sky blue", Print.SOLID);
                wear(o, Cat.HAT, "Sun hat", "Beige", "Sky blue", Print.SOLID);
                o.hair = 0; o.tuck = true; o.backdrop = 3;
            }
        }
    }

    static final String[] SURPRISE_NAMES = {"Gallery Opening", "Sunday Market", "Left Bank Stroll", "Front Row",
            "Off-Duty Model", "First Date", "Garden Party", "Weekend in Milan", "After Hours", "Autumn Edit",
            "Capsule Wardrobe", "Brunch Club", "Rainy Day Chic", "Matinee", "Seaside Escape", "Studio Visit"};

    /** A random look built the way stylists do it: neutrals as a base, one or two accents, at most a print or two. */
    static void surprise(Outfit o, Random r) {
        clearPieces(o);
        List<String> neutrals = List.of("Black", "Charcoal", "Ivory", "White", "Cream", "Camel", "Navy", "Beige", "Chocolate", "Grey marl", "Stone");
        List<NamedColor> accents = new ArrayList<>();
        for (NamedColor n : PALETTE) if (!n.neutral()) accents.add(n);
        String accent = accents.get(r.nextInt(accents.size())).name();
        String base = neutrals.get(r.nextInt(neutrals.size()));
        String other = neutrals.get(r.nextInt(neutrals.size()));
        Print[] prints = {Print.STRIPES, Print.POLKA, Print.GINGHAM, Print.LEOPARD, Print.FLORAL, Print.HOUNDSTOOTH, Print.TARTAN, Print.PINSTRIPE};
        boolean printUsed = false;

        if (r.nextDouble() < 0.3) {
            Piece d = o.get(Cat.DRESS);
            d.style = 1 + r.nextInt(Cat.DRESS.styles.length - 1);
            d.c1 = col(r.nextBoolean() ? accent : base);
            d.c2 = col(other);
            if (r.nextDouble() < 0.4) { d.print = prints[r.nextInt(prints.length)]; printUsed = true; }
        } else {
            Piece t = o.get(Cat.TOP);
            t.style = 1 + r.nextInt(Cat.TOP.styles.length - 1);
            t.c1 = col(r.nextDouble() < 0.6 ? accent : other);
            t.c2 = col(base);
            if (r.nextDouble() < 0.35) { t.print = prints[r.nextInt(prints.length)]; printUsed = true; }
            Piece b = o.get(Cat.BOTTOM);
            b.style = 1 + r.nextInt(Cat.BOTTOM.styles.length - 1);
            b.c1 = col(base);
            b.c2 = col(other);
            String bs = o.style(Cat.BOTTOM);
            if (bs.equals("Skinny jeans") || (r.nextDouble() < 0.3 && !bs.contains("Pleated"))) {
                b.print = Print.DENIM; b.c1 = col("Denim blue");
            } else if (!printUsed && r.nextDouble() < 0.25) {
                b.print = prints[r.nextInt(prints.length)];
            }
            o.tuck = r.nextBoolean();
        }
        if (r.nextDouble() < 0.5) {
            Piece c = o.get(Cat.OUTER);
            c.style = 1 + r.nextInt(Cat.OUTER.styles.length - 1);
            c.c1 = col(List.of("Camel", "Black", "Navy", "Cream", "Charcoal", "Beige", "Chocolate").get(r.nextInt(7)));
            c.c2 = col("Ivory");
            if (o.style(Cat.OUTER).equals("Cardigan")) c.print = Print.KNIT;
        }
        Piece s = o.get(Cat.SHOES);
        s.style = 1 + r.nextInt(Cat.SHOES.styles.length - 1);
        s.c1 = col(r.nextDouble() < 0.3 ? accent : List.of("Black", "Cognac", "Camel", "White", "Burgundy", "Beige").get(r.nextInt(6)));
        s.c2 = col("White");
        if (r.nextDouble() < 0.65) {
            Piece b = o.get(Cat.BAG);
            b.style = 1 + r.nextInt(Cat.BAG.styles.length - 1);
            b.c1 = r.nextBoolean() ? s.c1 : col(accent);
            b.c2 = col("Ivory");
        }
        if (r.nextDouble() < 0.25) {
            Piece h = o.get(Cat.HAT);
            h.style = 1 + r.nextInt(Cat.HAT.styles.length - 1);
            h.c1 = col(List.of("Black", "Camel", "Beige", "Cream", "Navy").get(r.nextInt(5)));
            h.c2 = col(accent);
        }
        o.belt = r.nextDouble() < 0.3;
        o.sunglasses = r.nextDouble() < 0.2;
        o.pearls = r.nextDouble() < 0.15;
        o.hair = r.nextInt(HAIR_STYLES.length);
        o.name = SURPRISE_NAMES[r.nextInt(SURPRISE_NAMES.length)];
    }

    // =====================================================================
    //  Geometry helpers (figure coordinates: x = 0 is the centre line, y down)
    // =====================================================================

    record P(double x, double y) { double distTo(P o) { return Math.hypot(x - o.x, y - o.y); } }
    static P p(double x, double y) { return new P(x, y); }
    static List<P> L(P... ps) { return new ArrayList<>(Arrays.asList(ps)); }
    static P mid(P a, P b) { return p((a.x() + b.x()) / 2, (a.y() + b.y()) / 2); }

    /** Closed path smoothed through midpoints. Repeat a point to make a sharp corner. */
    static Path2D smooth(List<P> pts) {
        Path2D path = new Path2D.Double();
        int n = pts.size();
        P first = mid(pts.get(n - 1), pts.get(0));
        path.moveTo(first.x(), first.y());
        for (int i = 0; i < n; i++) {
            P c = pts.get(i), m = mid(c, pts.get((i + 1) % n));
            path.quadTo(c.x(), c.y(), m.x(), m.y());
        }
        path.closePath();
        return path;
    }

    static Path2D poly(List<P> pts) {
        Path2D path = new Path2D.Double();
        path.moveTo(pts.get(0).x(), pts.get(0).y());
        for (int i = 1; i < pts.size(); i++) path.lineTo(pts.get(i).x(), pts.get(i).y());
        path.closePath();
        return path;
    }

    /** Takes the left half (top centre → down the left side → bottom centre) and adds the mirrored right half. */
    static List<P> sym(List<P> left) {
        List<P> out = new ArrayList<>(left);
        for (int i = left.size() - 1; i >= 0; i--) {
            P q = left.get(i);
            if (Math.abs(q.x()) > 0.01) out.add(p(-q.x(), q.y()));
        }
        return out;
    }

    static Shape mirror(Shape s) { return AffineTransform.getScaleInstance(-1, 1).createTransformedShape(s); }

    static Path2D quad(double x1, double y1, double cx, double cy, double x2, double y2) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1); p.quadTo(cx, cy, x2, y2);
        return p;
    }

    static void line(Graphics2D g, double x1, double y1, double x2, double y2) { g.draw(new Line2D.Double(x1, y1, x2, y2)); }
    static void lineM(Graphics2D g, double x1, double y1, double x2, double y2) { line(g, x1, y1, x2, y2); line(g, -x1, y1, -x2, y2); }

    static double interp(double[] ys, double[] xs, double y) {
        if (y <= ys[0]) return xs[0];
        for (int i = 1; i < ys.length; i++)
            if (y <= ys[i]) { double t = (y - ys[i - 1]) / (ys[i] - ys[i - 1]); return xs[i - 1] + t * (xs[i] - xs[i - 1]); }
        return xs[xs.length - 1];
    }

    // Body profile: half-width of the torso at height y, and of each leg.
    static final double[] BY = {116, 122, 128, 150, 168, 195, 215, 245, 275, 300};
    static final double[] BX = {9, 34, 51, 45, 41, 31, 28, 37, 46, 45};
    static final double[] LY = {290, 330, 380, 430, 500, 555};
    static final double[] LO = {46, 41, 37, 32.5, 26, 19};
    static final double[] IY = {305, 330, 380, 430, 500, 555};
    static final double[] IX = {0, 3, 4, 5, 6, 7};
    static double bodyW(double y) { return interp(BY, BX, y); }
    static double legOut(double y) { return interp(LY, LO, y); }
    static double legIn(double y) { return interp(IY, IX, y); }

    static final double FOOT_X = -13.5;
    static final P SHOULDER = p(-46, 133), ELBOW = p(-58, 222), WRIST = p(-63, 298);

    static final int CREW = 0, VNECK = 1, TURTLE = 2, SCOOP = 3, STRAP = 4, BOAT = 5, DEEPV = 6;

    static List<P> neckPts(int type) {
        return switch (type) {
            case VNECK -> L(p(0, 146), p(0, 146), p(-13, 115), p(-13, 115), p(-31, 120), p(-50, 126));
            case DEEPV -> L(p(0, 168), p(0, 168), p(-14, 115), p(-14, 115), p(-31, 120), p(-50, 126));
            case TURTLE -> L(p(0, 99), p(-11, 99), p(-11, 99), p(-12, 116), p(-31, 120), p(-50, 126));
            case SCOOP -> L(p(0, 138), p(-11, 135), p(-17, 124), p(-18, 117), p(-18, 117), p(-33, 121), p(-50, 126));
            case BOAT -> L(p(0, 117), p(-22, 116), p(-38, 119), p(-50, 126));
            case STRAP -> L(p(0, 150), p(-16, 147), p(-33, 146), p(-33, 146), p(-41, 152));
            default -> L(p(0, 121), p(-8, 120), p(-13, 115), p(-13, 115), p(-31, 120), p(-50, 126));
        };
    }

    static Shape headShape() {
        Path2D h = new Path2D.Double();
        h.moveTo(0, 42);
        h.curveTo(14, 42, 22, 54, 22, 68);
        h.curveTo(22, 86, 12, 98, 0, 98);
        h.curveTo(-12, 98, -22, 86, -22, 68);
        h.curveTo(-22, 54, -14, 42, 0, 42);
        h.closePath();
        return h;
    }

    static Shape handShape() { return new Ellipse2D.Double(WRIST.x() - 6.3, WRIST.y() - 1, 11.5, 25); }

    static Shape armShape() {
        Area a = new Area(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Line2D.Double(SHOULDER.x(), SHOULDER.y(), ELBOW.x(), ELBOW.y())));
        a.add(new Area(new BasicStroke(12.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Line2D.Double(ELBOW.x(), ELBOW.y(), WRIST.x(), WRIST.y()))));
        a.add(new Area(handShape()));
        return a;
    }

    static Shape legShape() {
        List<P> l = new ArrayList<>();
        l.add(p(0, 290));
        for (double y = 285; y <= 555; y += 15) l.add(p(-legOut(y), y));
        l.add(p(-legOut(555), 560));
        l.add(p(-legIn(555), 560));
        for (double y = 555; y >= 320; y -= 15) l.add(p(-legIn(y), y));
        l.add(p(0, 306));
        return smooth(l);
    }

    static Area bodyArea() {
        List<P> t = L(p(0, 104), p(-8.5, 104), p(-8.5, 116), p(-30, 121), p(-48, 126), p(-51, 132));
        for (double y = 140; y <= 300; y += 10) t.add(p(-bodyW(y), y));
        t.add(p(-40, 306));
        t.add(p(0, 309));
        Area a = new Area(smooth(sym(t)));
        Shape leg = legShape();
        a.add(new Area(leg)); a.add(new Area(mirror(leg)));
        Shape arm = armShape();
        a.add(new Area(arm)); a.add(new Area(mirror(arm)));
        Shape foot = new Ellipse2D.Double(FOOT_X - 6.8, 548, 13.6, 47);
        a.add(new Area(foot)); a.add(new Area(mirror(foot)));
        return a;
    }

    /** A sleeve along the left arm covering the given fraction of its length. */
    static Shape sleeve(double frac, double width) {
        P[] pts = {p(-45, 127), ELBOW, WRIST};
        double total = 0;
        for (int i = 1; i < pts.length; i++) total += pts[i].distTo(pts[i - 1]);
        double target = frac * total, acc = 0;
        Path2D path = new Path2D.Double();
        path.moveTo(pts[0].x(), pts[0].y());
        for (int i = 1; i < pts.length; i++) {
            double d = pts[i].distTo(pts[i - 1]);
            if (acc + d >= target) {
                double t = (target - acc) / d;
                path.lineTo(pts[i - 1].x() + (pts[i].x() - pts[i - 1].x()) * t, pts[i - 1].y() + (pts[i].y() - pts[i - 1].y()) * t);
                break;
            }
            path.lineTo(pts[i].x(), pts[i].y());
            acc += d;
        }
        return new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(path);
    }

    // =====================================================================
    //  Fabric
    // =====================================================================

    static final Map<String, Paint> PAINTS = new HashMap<>();

    static Paint paintFor(Piece p) {
        if (p.print == Print.SOLID) return p.c1;
        String key = p.print + ":" + p.c1.getRGB() + ":" + p.c2.getRGB();
        return PAINTS.computeIfAbsent(key, k -> makeTexture(p.print, p.c1, p.c2));
    }

    static final String[] HOUNDSTOOTH = {"11110001", "11110011", "11110110", "11111100",
                                         "00110000", "01100000", "11000000", "10000001"};

    static Paint makeTexture(Print pr, Color a, Color b) {
        int T = switch (pr) {
            case STRIPES -> 14; case PINSTRIPE -> 9; case POLKA -> 14; case GINGHAM -> 12; case TARTAN -> 28;
            case HOUNDSTOOTH -> 8; case HERRINGBONE -> 12; case LEOPARD -> 40; case FLORAL -> 26;
            case DENIM -> 6; case KNIT -> 7; default -> 8;
        };
        final int R = 4;
        BufferedImage img = new BufferedImage(T * R, T * R, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(R, R);
        g.setColor(a);
        g.fillRect(0, 0, T, T);
        switch (pr) {
            case STRIPES -> { g.setColor(b); g.fill(new Rectangle2D.Double(0, 8, T, 4.5)); }
            case PINSTRIPE -> { g.setColor(b); g.fill(new Rectangle2D.Double(4.2, 0, 0.7, T)); }
            case POLKA -> {
                g.setColor(b);
                g.fill(new Ellipse2D.Double(3.5 - 2.4, 3.5 - 2.4, 4.8, 4.8));
                g.fill(new Ellipse2D.Double(10.5 - 2.4, 10.5 - 2.4, 4.8, 4.8));
            }
            case GINGHAM -> {
                g.setColor(mix(a, b, 0.5));
                g.fill(new Rectangle2D.Double(0, 0, 6, T));
                g.fill(new Rectangle2D.Double(0, 0, T, 6));
                g.setColor(b);
                g.fill(new Rectangle2D.Double(0, 0, 6, 6));
            }
            case TARTAN -> {
                g.setColor(alpha(b, 150));
                g.fill(new Rectangle2D.Double(0, 0, 10, T));
                g.fill(new Rectangle2D.Double(0, 0, T, 10));
                Color thin = lum(b) < 0.5 ? mix(b, Color.WHITE, 0.65) : mix(b, Color.BLACK, 0.6);
                g.setColor(alpha(thin, 200));
                g.fill(new Rectangle2D.Double(17, 0, 1.2, T));
                g.fill(new Rectangle2D.Double(0, 17, T, 1.2));
                g.setColor(alpha(mix(b, Color.BLACK, 0.45), 150));
                g.fill(new Rectangle2D.Double(4.4, 0, 1.0, T));
                g.fill(new Rectangle2D.Double(0, 4.4, T, 1.0));
            }
            case HOUNDSTOOTH -> {
                g.setColor(b);
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (HOUNDSTOOTH[r].charAt(c) == '1') g.fill(new Rectangle2D.Double(c, r, 1, 1));
            }
            case HERRINGBONE -> {
                g.setColor(b);
                g.setStroke(new BasicStroke(1.0f));
                Shape old = g.getClip();
                g.clip(new Rectangle2D.Double(0, 0, 6, T));
                for (int k = -3; k <= 5; k++) line(g, 0, k * 3, 6, k * 3 + 6);
                g.setClip(old);
                g.clip(new Rectangle2D.Double(6, 0, 6, T));
                for (int k = -3; k <= 5; k++) line(g, 6, k * 3 + 6, 12, k * 3);
                g.setClip(old);
            }
            case LEOPARD -> {
                Random r = new Random(7);
                Color inner = mix(a, b, 0.35);
                for (int i = 0; i < 9; i++) {
                    double x = r.nextDouble() * T, y = r.nextDouble() * T;
                    double w = 5 + r.nextDouble() * 4, h = 4 + r.nextDouble() * 4;
                    double s1 = r.nextDouble() * 360;
                    for (int dx = -T; dx <= T; dx += T)
                        for (int dy = -T; dy <= T; dy += T) {
                            g.setColor(inner);
                            g.fill(new Ellipse2D.Double(x + dx - w / 2, y + dy - h / 2, w, h));
                            g.setColor(b);
                            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            for (int k = 0; k < 3; k++)
                                g.draw(new Arc2D.Double(x + dx - w / 2 - 1.2, y + dy - h / 2 - 1.2, w + 2.4, h + 2.4,
                                        s1 + k * 120, 80, Arc2D.OPEN));
                        }
                }
                g.setColor(b);
                for (int i = 0; i < 8; i++) {
                    double x = r.nextDouble() * T, y = r.nextDouble() * T;
                    for (int dx = -T; dx <= T; dx += T)
                        for (int dy = -T; dy <= T; dy += T)
                            g.fill(new Ellipse2D.Double(x + dx, y + dy, 1.8, 1.5));
                }
            }
            case FLORAL -> {
                Color leaf = mix(new Color(0x4E7D4A), a, 0.2);
                Color centre = dist(b, new Color(0xF2D16B)) < 60 ? Color.WHITE : new Color(0xF2D16B);
                double[][] flowers = {{7, 7}, {20, 19}};
                for (double[] f : flowers) {
                    g.setColor(leaf);
                    g.fill(AffineTransform.getRotateInstance(0.7, f[0] + 4, f[1] + 3)
                            .createTransformedShape(new Ellipse2D.Double(f[0] + 2, f[1] + 2, 5, 2.4)));
                    g.setColor(b);
                    for (int k = 0; k < 5; k++) {
                        double ang = k * Math.PI * 2 / 5;
                        g.fill(new Ellipse2D.Double(f[0] + Math.cos(ang) * 2.8 - 2.2, f[1] + Math.sin(ang) * 2.8 - 2.2, 4.4, 4.4));
                    }
                    g.setColor(centre);
                    g.fill(new Ellipse2D.Double(f[0] - 1.5, f[1] - 1.5, 3, 3));
                }
                g.setColor(b);
                g.fill(new Ellipse2D.Double(19.5, 5, 1.8, 1.8));
                g.fill(new Ellipse2D.Double(5, 19.5, 1.8, 1.8));
            }
            case DENIM -> {
                g.setStroke(new BasicStroke(0.8f));
                g.setColor(mix(a, Color.WHITE, 0.22));
                for (int x = -6; x <= 6; x += 3) line(g, x, 0, x + 6, 6);
                g.setColor(mix(a, Color.BLACK, 0.15));
                for (int x = -6; x <= 6; x += 3) line(g, x + 1.5, 0, x + 7.5, 6);
            }
            case KNIT -> {
                g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(mix(a, Color.BLACK, 0.22));
                for (double y = 0; y < T; y += 3.5) {
                    Path2D v = new Path2D.Double();
                    v.moveTo(0.5, y); v.lineTo(3.5, y + 3.5); v.lineTo(6.5, y);
                    g.draw(v);
                }
                g.setColor(mix(a, Color.WHITE, 0.15));
                for (double y = 0; y < T; y += 3.5) line(g, 1.2, y + 0.3, 3.5, y + 2.6);
            }
            default -> { }
        }
        g.dispose();
        return new TexturePaint(img, new Rectangle2D.Double(0, 0, T, T));
    }

    static Color edge(Color c) { return lum(c) < 0.16 ? mix(c, Color.WHITE, 0.28) : mix(c, Color.BLACK, 0.45); }
    static Color detail(Piece p) { return alpha(edge(p.c1), 170); }

    static void shade(Graphics2D g, Shape s, int strength) {
        Rectangle2D b = s.getBounds2D();
        if (b.getWidth() < 2) return;
        g.setPaint(new LinearGradientPaint((float) b.getMinX(), 0f, (float) b.getMaxX(), 0f,
                new float[]{0f, 0.22f, 0.55f, 1f},
                new Color[]{new Color(0, 0, 0, strength), new Color(0, 0, 0, 0),
                        new Color(255, 255, 255, strength / 2), new Color(0, 0, 0, strength + 5)}));
        g.fill(s);
    }

    /** Fills a garment shape with fabric, soft shading and an outline. */
    static void cloth(Graphics2D g, Shape s, Piece p) {
        g.setPaint(paintFor(p));
        g.fill(s);
        shade(g, s, 42);
        g.setColor(edge(p.c1));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void clipDraw(Graphics2D g, Shape clip, Runnable r) {
        Shape old = g.getClip();
        g.clip(clip);
        r.run();
        g.setClip(old);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    static final double FW = 460, FH = 640;

    static void render(Graphics2D g, Outfit o, int w, int h, boolean title) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawBackdrop(g, o.backdrop, w, h);
        double top = title ? h * 0.10 : 0;   // keep the look's name clear of the model
        double s = Math.min(w / FW, (h - top) / FH);
        AffineTransform old = g.getTransform();
        g.translate(w / 2.0, top + (h - top - FH * s) / 2.0);
        g.scale(s, s);
        g.translate(0, 12);
        drawFigure(g, o);
        g.setTransform(old);
        if (title) drawTitle(g, o, w, h);
    }

    static void drawTitle(Graphics2D g, Outfit o, int w, int h) {
        Color tc = o.backdrop == 1 ? new Color(240, 235, 230) : new Color(48, 36, 42);
        int fs = (int) Math.max(15, Math.min(w, h) * 0.05);
        float x = (float) (w * 0.04), y = (float) (h * 0.05 + 6);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, fs / 3)));
        g.setColor(alpha(tc, 160));
        g.drawString("F A S H I O N   S T U D I O", x, y);
        g.setFont(new Font("Serif", Font.ITALIC, fs));
        g.setColor(tc);
        g.drawString(o.name, x, y + fs * 1.1f);
    }

    static void drawBackdrop(Graphics2D g, int type, int w, int h) {
        double cx = w / 2.0;
        switch (type) {
            case 1 -> { // Runway
                g.setPaint(new GradientPaint(0, 0, new Color(22, 22, 30), 0, h, new Color(48, 45, 60)));
                g.fillRect(0, 0, w, h);
                Path2D floor = new Path2D.Double();
                floor.moveTo(cx - w * 0.10, h * 0.52); floor.lineTo(cx + w * 0.10, h * 0.52);
                floor.lineTo(cx + w * 0.44, h); floor.lineTo(cx - w * 0.44, h); floor.closePath();
                g.setPaint(new GradientPaint(0, (float) (h * 0.52), new Color(66, 64, 80), 0, h, new Color(122, 118, 140)));
                g.fill(floor);
                for (int i = 0; i <= 12; i++) {
                    double t = i / 12.0, y = h * 0.52 + t * h * 0.48, dx = w * 0.10 + t * w * 0.34, r = 1.2 + t * 3.5;
                    g.setColor(new Color(255, 228, 170, 210));
                    g.fill(new Ellipse2D.Double(cx - dx - r, y - r, 2 * r, 2 * r));
                    g.fill(new Ellipse2D.Double(cx + dx - r, y - r, 2 * r, 2 * r));
                }
                Path2D beam = new Path2D.Double();
                beam.moveTo(cx - w * 0.03, 0); beam.lineTo(cx + w * 0.03, 0);
                beam.lineTo(cx + w * 0.26, h); beam.lineTo(cx - w * 0.26, h); beam.closePath();
                g.setPaint(new GradientPaint(0, 0, new Color(255, 250, 235, 75), 0, h, new Color(255, 250, 235, 18)));
                g.fill(beam);
            }
            case 2 -> { // Paris
                g.setPaint(new GradientPaint(0, 0, new Color(188, 213, 236), 0, h, new Color(248, 223, 212)));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(255, 255, 255, 150));
                double[][] clouds = {{0.10, 0.13, 0.20, 0.045}, {0.17, 0.11, 0.14, 0.04}, {0.58, 0.07, 0.22, 0.04}, {0.66, 0.09, 0.14, 0.035}};
                for (double[] c : clouds) g.fill(new Ellipse2D.Double(w * c[0], h * c[1], w * c[2], h * c[3]));
                double H = h * 0.62, tx = w * 0.84, by = h * 0.9;
                Path2D t = new Path2D.Double();
                t.moveTo(tx - H * 0.012, by - H * 0.93);
                t.quadTo(tx - H * 0.04, by - H * 0.35, tx - H * 0.2, by);
                t.lineTo(tx - H * 0.12, by);
                t.quadTo(tx, by - H * 0.27, tx + H * 0.12, by);
                t.lineTo(tx + H * 0.2, by);
                t.quadTo(tx + H * 0.04, by - H * 0.35, tx + H * 0.012, by - H * 0.93);
                t.closePath();
                Color tower = new Color(122, 132, 152, 120);
                g.setColor(tower);
                g.fill(t);
                g.fill(new Rectangle2D.Double(tx - H * 0.105, by - H * 0.31, H * 0.21, H * 0.02));
                g.fill(new Rectangle2D.Double(tx - H * 0.05, by - H * 0.6, H * 0.1, H * 0.015));
                g.setStroke(new BasicStroke((float) Math.max(1, H * 0.006)));
                line(g, tx, by - H * 0.93, tx, by - H);
                g.setColor(new Color(231, 219, 206));
                g.fill(new Rectangle2D.Double(0, h * 0.9, w, h * 0.1));
                g.setColor(new Color(205, 190, 175));
                g.setStroke(new BasicStroke(1f));
                line(g, 0, h * 0.9, w, h * 0.9);
                for (int i = -6; i <= 6; i++) line(g, cx + i * w * 0.06, h * 0.9, cx + i * w * 0.11, h);
            }
            case 3 -> { // Blush paper
                g.setColor(new Color(246, 226, 225));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(214, 160, 165, 70));
                g.setStroke(new BasicStroke(1.4f));
                double r = Math.min(w, h) * 0.36;
                g.draw(new Ellipse2D.Double(cx - r, h * 0.45 - r, 2 * r, 2 * r));
                Random rnd = new Random(3);
                for (int i = 0; i < 60; i++) {
                    double x = rnd.nextDouble() * w, y = rnd.nextDouble() * h;
                    g.fill(new Ellipse2D.Double(x, y, 3, 3));
                }
            }
            case 4 -> { g.setColor(Color.WHITE); g.fillRect(0, 0, w, h); }
            default -> { // Studio
                float rad = (float) (Math.max(w, h) * 0.75);
                g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, h * 0.42), rad,
                        new float[]{0f, 1f}, new Color[]{new Color(246, 241, 236), new Color(214, 205, 196)}));
                g.fillRect(0, 0, w, h);
                g.setPaint(new GradientPaint(0, (float) (h * 0.82), new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 25)));
                g.fillRect(0, (int) (h * 0.82), w, h);
            }
        }
    }

    static void drawFigure(Graphics2D g, Outfit o) {
        Color skin = SKIN.get(o.skin).color();
        g.setColor(new Color(0, 0, 0, o.backdrop == 1 ? 90 : 38));
        g.fill(new Ellipse2D.Double(-70, 588, 140, 16));

        drawHairBack(g, o);

        Area body = bodyArea();
        g.setColor(skin);
        g.fill(body);
        shade(g, body, 26);
        g.setColor(mix(skin, Color.BLACK, 0.32));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);

        Shape head = headShape();
        g.setColor(skin);
        g.fill(head);
        shade(g, head, 18);
        g.setColor(mix(skin, Color.BLACK, 0.32));
        g.draw(head);
        drawFace(g, o, skin);

        drawShoes(g, o);
        if (o.has(Cat.DRESS)) {
            drawDress(g, o);
        } else {
            boolean tuck = o.tuck && o.has(Cat.BOTTOM) && o.has(Cat.TOP);
            if (tuck) { drawTop(g, o, true); drawBottom(g, o); }
            else { drawBottom(g, o); drawTop(g, o, false); }
        }
        if (o.belt) drawBelt(g, o);
        if (o.pearls) drawPearls(g, o);
        drawOuter(g, o);
        drawBag(g, o, skin);
        drawHairFront(g, o);
        drawHat(g, o);
        if (o.sunglasses) drawSunglasses(g);
    }

    static void drawFace(Graphics2D g, Outfit o, Color skin) {
        Color dark = new Color(58, 40, 36);
        for (int sx = -1; sx <= 1; sx += 2) {
            double ex = 9 * sx;
            Path2D eye = new Path2D.Double();
            eye.moveTo(ex - 5, 71); eye.quadTo(ex, 66.5, ex + 5, 71); eye.quadTo(ex, 74, ex - 5, 71); eye.closePath();
            g.setColor(new Color(246, 241, 236));
            g.fill(eye);
            g.setColor(dark);
            g.fill(new Ellipse2D.Double(ex - 2, 68.6, 4, 4));
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(quad(ex - 5.5, 71, ex, 65.6, ex + 5.5, 70.6));
            g.setColor(mix(o.hairColor, Color.BLACK, 0.25));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(quad(ex - 6, 64.5, ex, 61.5, ex + 6, 64.5));
            g.setColor(new Color(225, 115, 115, 34));
            g.fill(new Ellipse2D.Double(13 * sx - 4.5, 78, 9, 5));
        }
        g.setColor(mix(skin, Color.BLACK, 0.35));
        g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(quad(1, 74, 2.8, 80, 0, 81));
        Color lip = mix(skin, new Color(176, 58, 82), 0.6);
        Path2D lips = new Path2D.Double();
        lips.moveTo(-6, 87.5); lips.quadTo(-3, 84.5, 0, 86); lips.quadTo(3, 84.5, 6, 87.5); lips.quadTo(0, 93, -6, 87.5); lips.closePath();
        g.setColor(lip);
        g.fill(lips);
        g.setColor(mix(lip, Color.BLACK, 0.35));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(quad(-6, 87.5, 0, 88.8, 6, 87.5));
    }

    static void hairFill(Graphics2D g, Shape s, Color hc) {
        g.setColor(hc);
        g.fill(s);
        shade(g, s, 35);
        g.setColor(mix(hc, Color.BLACK, 0.38));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void drawHairBack(Graphics2D g, Outfit o) {
        Shape s = switch (o.hair) {
            case 0 -> smooth(sym(L(p(0, 37), p(-22, 42), p(-29, 64), p(-30, 96), p(-27, 122), p(0, 124))));
            case 1 -> smooth(sym(L(p(0, 37), p(-23, 43), p(-30, 66), p(-31, 96), p(-32, 110), p(-32, 110), p(-14, 112), p(0, 112))));
            case 2 -> new Ellipse2D.Double(-14, 18, 28, 26);
            case 3 -> null;
            case 4 -> {
                Area a = new Area(new Ellipse2D.Double(-36, 30, 72, 80));
                for (int deg = 130; deg <= 410; deg += 20) {
                    double t = Math.toRadians(deg);
                    double x = 36 * Math.cos(t), y = 70 + 40 * Math.sin(t);
                    a.add(new Area(new Ellipse2D.Double(x - 11, y - 11, 22, 22)));
                }
                yield a;
            }
            default -> {
                Path2D t = new Path2D.Double();
                t.moveTo(12, 44); t.quadTo(42, 44, 38, 84); t.quadTo(33, 122, 40, 168);
                t.quadTo(24, 140, 24, 100); t.quadTo(24, 66, 12, 54); t.closePath();
                yield t;
            }
        };
        if (s != null) hairFill(g, s, o.hairColor);
    }

    static void drawHairFront(Graphics2D g, Outfit o) {
        Color hc = o.hairColor;
        List<P> line = switch (o.hair) {
            case 0 -> L(p(-30, 0), p(30, 0), p(30, 82), p(23, 82), p(20, 58), p(9, 48), p(-4, 47), p(-16, 53), p(-23, 66), p(-23, 82), p(-30, 82));
            case 1 -> L(p(-30, 0), p(30, 0), p(30, 88), p(24, 88), p(21, 61), p(10, 58.5), p(0, 59.5), p(-10, 58.5), p(-21, 61), p(-24, 88), p(-30, 88));
            case 3 -> L(p(-30, 0), p(30, 0), p(30, 76), p(23, 76), p(21, 57), p(6, 55), p(-10, 61), p(-21, 63), p(-23, 76), p(-30, 76));
            case 4 -> L(p(-30, 0), p(30, 0), p(30, 80), p(24, 80), p(19, 57), p(0, 51), p(-19, 57), p(-24, 80), p(-30, 80));
            default -> L(p(-30, 0), p(30, 0), p(30, 74), p(23, 74), p(18, 53), p(0, 47), p(-18, 53), p(-23, 74), p(-30, 74));
        };
        Area cap = new Area(new Ellipse2D.Double(-24.5, 38, 49, 62));
        cap.intersect(new Area(poly(line)));
        hairFill(g, cap, hc);
        if (o.hair == 0) {
            Shape strand = smooth(L(p(-21, 58), p(-26, 76), p(-28, 104), p(-33, 134), p(-32, 164), p(-37, 196), p(-37, 196),
                    p(-29, 203), p(-24, 178), p(-23, 148), p(-19, 118), p(-17, 94), p(-18, 70)));
            hairFill(g, strand, hc);
            hairFill(g, mirror(strand), hc);
            g.setColor(alpha(mix(hc, Color.WHITE, 0.3), 110));
            g.setStroke(new BasicStroke(0.9f));
            g.draw(quad(-24, 90, -31, 140, -31, 185));
            g.draw(quad(24, 90, 31, 140, 31, 185));
        }
        if (o.hair == 4) {
            Area curls = new Area();
            for (double x = -20; x <= 20; x += 8) curls.add(new Area(new Ellipse2D.Double(x - 6, 50 + x * x / 60 - 6, 12, 12)));
            hairFill(g, curls, hc);
        }
        g.setColor(alpha(mix(hc, Color.WHITE, 0.35), 120));
        g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(quad(-15, 52, -6, 44.5, 5, 46));
        g.draw(quad(-19, 60, -12, 49, -2, 46.5));
    }

    static void drawShoes(Graphics2D g, Outfit o) {
        if (!o.has(Cat.SHOES)) return;
        Piece p = o.get(Cat.SHOES);
        String s = o.style(Cat.SHOES);
        double fx = FOOT_X;
        Shape shoe;
        switch (s) {
            case "Court heels", "Ballet flats", "Loafers" -> {
                double top = s.equals("Court heels") ? 567 : s.equals("Ballet flats") ? 577 : 565;
                double ctrl = s.equals("Court heels") ? 575 : s.equals("Ballet flats") ? 581 : 562;
                Path2D sh = new Path2D.Double();
                sh.moveTo(fx - 7.5, top); sh.quadTo(fx, ctrl, fx + 7.5, top);
                sh.lineTo(fx + 7.2, 586); sh.quadTo(fx + 3, 600, fx, 600); sh.quadTo(fx - 3, 600, fx - 7.2, 586);
                sh.closePath();
                shoe = sh;
            }
            case "Trainers" -> shoe = new RoundRectangle2D.Double(fx - 8.5, 560, 17, 41, 9, 9);
            default -> { // boots
                double top = s.equals("Knee boots") ? 424 : 520;
                List<P> l = new ArrayList<>();
                for (double y = top; y <= 555; y += 15) l.add(p(-(legOut(y) + 2), y));
                l.add(p(fx - 7.8, 584));
                l.add(p(fx - 3, 601));
                l.add(p(fx + 3, 601));
                l.add(p(fx + 7.5, 584));
                for (double y = 555; y >= top; y -= 15) l.add(p(-(legIn(y) - 1.5), y));
                l.add(p(-(legIn(top) - 1.5), top));
                l.add(p(-(legOut(top) + 2), top));
                l.add(p(-(legOut(top) + 2), top));
                shoe = smooth(l);
            }
        }
        for (int side = 0; side < 2; side++) {
            final int fside = side;
            Shape sh = side == 0 ? shoe : mirror(shoe);
            cloth(g, sh, p);
            Color dc = detail(p);
            switch (s) {
                case "Trainers" -> {
                    clipDraw(g, sh, () -> {
                        Shape sole = fside == 0 ? new Rectangle2D.Double(fx - 10, 592, 20, 10) : mirror(new Rectangle2D.Double(fx - 10, 592, 20, 10));
                        g.setColor(p.c2); g.fill(sole);
                    });
                    g.setColor(dc);
                    g.setStroke(new BasicStroke(0.9f));
                    double sx = side == 0 ? 1 : -1;
                    for (double y = 567; y <= 580; y += 4.5) line(g, sx * (fx - 3.5), y, sx * (fx + 3.5), y);
                    g.setColor(edge(p.c1));
                    g.draw(sh);
                }
                case "Loafers" -> {
                    g.setColor(dc);
                    g.setStroke(new BasicStroke(1.0f));
                    double sx = side == 0 ? 1 : -1;
                    line(g, sx * (fx - 7), 574, sx * (fx + 7), 574);
                    g.draw(new Rectangle2D.Double(Math.min(sx * (fx - 2.5), sx * (fx + 2.5)), 571.5, 5, 2.5));
                }
                case "Ballet flats" -> {
                    double sx = side == 0 ? 1 : -1;
                    g.setColor(mix(p.c1, Color.BLACK, 0.3));
                    g.fill(new Ellipse2D.Double(sx * fx - 3, 578, 2.8, 2.6));
                    g.fill(new Ellipse2D.Double(sx * fx + 0.2, 578, 2.8, 2.6));
                }
                case "Ankle boots", "Knee boots" -> {
                    g.setColor(dc);
                    g.setStroke(new BasicStroke(1.0f));
                    double sx = side == 0 ? 1 : -1;
                    line(g, sx * (fx - 7.5), 592, sx * (fx + 7.5), 592);
                }
                default -> { }
            }
        }
    }

    static Shape bodice(int neck, double ease, double hem, double flare) {
        List<P> l = neckPts(neck);
        double y0 = neck == STRAP ? 158 : 138;
        for (double y = y0; y < hem - 4; y += 8) l.add(p(-(bodyW(y) + ease), y));
        double hx = bodyW(hem) + ease + flare;
        l.add(p(-hx, hem)); l.add(p(-hx, hem)); l.add(p(0, hem + 1.5));
        return smooth(sym(l));
    }

    static void drawTop(Graphics2D g, Outfit o, boolean tucked) {
        if (!o.has(Cat.TOP)) return;
        Piece p = o.get(Cat.TOP);
        String s = o.style(Cat.TOP);
        int neck;
        double ease, hem, sf, sw, flare = 0;
        switch (s) {
            case "T-shirt" -> { neck = CREW; ease = 4; hem = 282; sf = 0.3; sw = 23; }
            case "Blouse" -> { neck = VNECK; ease = 6; hem = 287; sf = 1; sw = 21; flare = 3; }
            case "Turtleneck" -> { neck = TURTLE; ease = 3; hem = 280; sf = 1; sw = 18.5; }
            case "Crop top" -> { neck = SCOOP; ease = 2.5; hem = 198; sf = 0.2; sw = 21; }
            default -> { neck = STRAP; ease = 3; hem = 270; sf = 0; sw = 0; }
        }
        if (tucked && hem > 226) { hem = 226; flare = 0; }
        if (sf > 0) {
            Shape sl = sleeve(sf, sw);
            cloth(g, sl, p);
            cloth(g, mirror(sl), p);
        }
        Shape body = bodice(neck, ease, hem, flare);
        cloth(g, body, p);
        g.setColor(detail(p));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final double fhem = hem;
        switch (s) {
            case "T-shirt" -> clipDraw(g, body, () -> g.draw(quad(-14, 118.5, 0, 129, 14, 118.5)));
            case "Blouse" -> {
                Shape collar = poly(L(p(-13.5, 114.5), p(-25, 120), p(-15, 136), p(-2, 143)));
                cloth(g, collar, p);
                cloth(g, mirror(collar), p);
                for (double y = 155; y < fhem - 8; y += 17) {
                    g.setColor(mix(p.c1, Color.WHITE, 0.55));
                    g.fill(new Ellipse2D.Double(-2, y - 2, 4, 4));
                    g.setColor(detail(p));
                    g.draw(new Ellipse2D.Double(-2, y - 2, 4, 4));
                }
            }
            case "Turtleneck" -> clipDraw(g, body, () -> { line(g, -12, 104, 12, 104); line(g, -12, 109, 12, 109); });
            case "Camisole" -> {
                g.setColor(edge(p.c1));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                lineM(g, -24, 147.5, -27, 121);
            }
            default -> { }
        }
    }

    static double skirtX(double y, double hemY, double hemHalf, double ease, double flareStart) {
        double hipX = 46 + ease;
        double body = (y <= 300 ? bodyW(y) : 45) + ease;
        if (hemHalf >= hipX) {
            if (y <= flareStart) return body;
            double t = (y - flareStart) / (hemY - flareStart);
            double startX = bodyW(flareStart) + ease;
            return Math.max(body, startX + (hemHalf - startX) * t);
        }
        if (y <= 275) return body;
        double t = (y - 275) / (hemY - 275);
        return hipX + (hemHalf - hipX) * t;
    }

    static double trouserHem(String s) {
        return switch (s) { case "Wide-leg trousers" -> 566; case "Skinny jeans" -> 553; case "Shorts" -> 338; default -> 560; };
    }

    static double trouserOuter(String s, double y) {
        double e = 4;
        double hip = Math.max(bodyW(Math.min(y, 300)), y > 285 ? legOut(y) : 0) + e;
        return switch (s) {
            case "Wide-leg trousers" -> Math.max(hip, 50 + 8 * clamp01((y - 290) / 276.0));
            case "Skinny jeans" -> y < 300 ? hip - 1.5 : legOut(y) + 2.5;
            case "Shorts" -> Math.max(hip, 48);
            default -> y < 300 ? hip : Math.max(legOut(y) + e, 36);
        };
    }

    static double trouserInner(String s, double y) {
        return switch (s) {
            case "Wide-leg trousers" -> 1.5;
            case "Skinny jeans" -> Math.max(1, legIn(y) - 1);
            case "Shorts" -> 3;
            default -> 3.5;
        };
    }

    static Shape trousers(String s) {
        double hem = trouserHem(s), w0 = bodyW(211) + 4;
        List<P> l = L(p(0, 211), p(-w0, 211), p(-w0, 211));
        for (double y = 219; y < hem - 4; y += 8) l.add(p(-trouserOuter(s, y), y));
        double ox = trouserOuter(s, hem), ix = trouserInner(s, hem);
        l.add(p(-ox, hem)); l.add(p(-ox, hem)); l.add(p(-ix, hem)); l.add(p(-ix, hem));
        for (double y = hem - 8; y > 322; y -= 10) l.add(p(-trouserInner(s, y), y));
        l.add(p(0, 314));
        return smooth(sym(l));
    }

    static double[] skirtSpec(String s) { // ease, hem, hemHalf
        return switch (s) {
            case "A-line skirt" -> new double[]{4, 430, 72};
            case "Pencil skirt" -> new double[]{3, 418, 41};
            case "Mini skirt" -> new double[]{4, 322, 58};
            default -> new double[]{5, 472, 82};
        };
    }

    static Shape skirt(String s) {
        double[] k = skirtSpec(s);
        double e = k[0], hem = k[1], half = k[2], w0 = bodyW(211) + e;
        List<P> l = L(p(0, 211), p(-w0, 211), p(-w0, 211));
        for (double y = 219; y < hem - 4; y += 8) l.add(p(-skirtX(y, hem, half, e, 215), y));
        l.add(p(-half, hem)); l.add(p(-half, hem)); l.add(p(0, hem + 2));
        return smooth(sym(l));
    }

    static void drawBottom(Graphics2D g, Outfit o) {
        if (!o.has(Cat.BOTTOM)) return;
        Piece p = o.get(Cat.BOTTOM);
        String s = o.style(Cat.BOTTOM);
        boolean trousers = s.contains("trousers") || s.contains("jeans") || s.equals("Shorts");
        Shape sh = trousers ? trousers(s) : skirt(s);
        cloth(g, sh, p);
        Color dc = detail(p);
        clipDraw(g, sh, () -> {
            g.setColor(dc);
            g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line(g, -50, 220, 50, 220);
            if (trousers) {
                double hem = trouserHem(s);
                if (s.equals("Straight trousers")) lineM(g, -24, 262, -20, hem);
                if (s.equals("Wide-leg trousers")) lineM(g, -25, 262, -30, hem);
                if (s.equals("Shorts")) line(g, -60, hem - 6, 60, hem - 6);
                if (p.print == Print.DENIM || s.equals("Skinny jeans")) {
                    g.setColor(new Color(214, 160, 70, 210));
                    g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{2f, 1.5f}, 0f));
                    g.draw(quad(-33, 221, -29, 237, -17, 222));
                    g.draw(quad(33, 221, 29, 237, 17, 222));
                    g.draw(quad(5, 221, 5, 256, -1, 257));
                } else {
                    g.draw(quad(1.5, 220, 1.5, 256, -3, 258));
                }
            } else {
                double[] k = skirtSpec(s);
                double hem = k[1], half = k[2];
                switch (s) {
                    case "Pleated midi skirt" -> {
                        for (int i = -6; i <= 6; i++) {
                            g.setColor(i % 2 == 0 ? dc : alpha(Color.WHITE, 60));
                            line(g, i * (bodyW(222) + 5) / 6.5, 222, i * half / 6.5, hem + 2);
                        }
                    }
                    case "A-line skirt" -> { lineM(g, -16, 285, -28, hem); lineM(g, -34, 300, -52, hem); }
                    default -> { }
                }
            }
        });
    }

    static void drawDress(Graphics2D g, Outfit o) {
        Piece p = o.get(Cat.DRESS);
        String s = o.style(Cat.DRESS);
        int neck;
        double e, flareStart, hem, half, sf, sw;
        switch (s) {
            case "Shift dress" -> { neck = BOAT; e = 6; flareStart = 150; hem = 386; half = 54; sf = 0; sw = 0; }
            case "Fit-and-flare dress" -> { neck = SCOOP; e = 2.5; flareStart = 215; hem = 425; half = 84; sf = 0.17; sw = 22; }
            case "Maxi dress" -> { neck = VNECK; e = 4; flareStart = 215; hem = 578; half = 82; sf = 0.28; sw = 25; }
            case "Slip dress" -> { neck = STRAP; e = 3; flareStart = 215; hem = 448; half = 52; sf = 0; sw = 0; }
            default -> { neck = DEEPV; e = 4; flareStart = 215; hem = 432; half = 68; sf = 1; sw = 19.5; }
        }
        List<P> l = neckPts(neck);
        double y0 = neck == STRAP ? 158 : 138;
        for (double y = y0; y < hem - 4; y += 8) l.add(p(-skirtX(y, hem, half, e, flareStart), y));
        l.add(p(-half, hem)); l.add(p(-half, hem)); l.add(p(0, hem + 2));
        Shape sh = smooth(sym(l));
        if (sf > 0) {
            Shape sl = sleeve(sf, sw);
            cloth(g, sl, p);
            cloth(g, mirror(sl), p);
        }
        cloth(g, sh, p);
        Color dc = detail(p);
        final double fhem = hem, fhalf = half;
        clipDraw(g, sh, () -> {
            g.setColor(dc);
            g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (!s.equals("Shift dress") && !s.equals("Slip dress")) line(g, -50, 215, 50, 215);
            if (fhalf > 60) { lineM(g, -12, 232, -fhalf * 0.42, fhem); lineM(g, -26, 240, -fhalf * 0.78, fhem); }
            if (s.equals("Wrap dress")) g.draw(quad(0, 168, 14, 200, 32, 214));
        });
        if (s.equals("Slip dress")) {
            g.setColor(edge(p.c1));
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            lineM(g, -24, 147.5, -27, 121);
        }
        if (s.equals("Wrap dress")) {
            Shape bow1 = new Ellipse2D.Double(30, 207, 12, 7), bow2 = new Ellipse2D.Double(27, 213, 10, 7);
            cloth(g, bow1, p);
            cloth(g, bow2, p);
            g.setColor(edge(p.c1));
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line(g, 33, 217, 38, 252);
            line(g, 31, 217, 32, 246);
        }
    }

    static void drawBelt(Graphics2D g, Outfit o) {
        Color c = o.has(Cat.SHOES) ? o.get(Cat.SHOES).c1 : col("Cognac");
        double half = bodyW(212) + (o.has(Cat.DRESS) ? 5 : o.has(Cat.TOP) ? 7 : 5);
        Shape belt = new RoundRectangle2D.Double(-half, 207, half * 2, 10, 3, 3);
        g.setColor(c);
        g.fill(belt);
        shade(g, belt, 40);
        g.setColor(edge(c));
        g.setStroke(new BasicStroke(1f));
        g.draw(belt);
        g.setColor(new Color(0xC9A24B));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new RoundRectangle2D.Double(-5.5, 205, 11, 14, 3, 3));
        g.setStroke(new BasicStroke(1.1f));
        line(g, 0, 207, 0, 217);
    }

    static void drawPearls(Graphics2D g, Outfit o) {
        boolean high = (!o.has(Cat.DRESS) && o.style(Cat.TOP).equals("Turtleneck"));
        double y0 = high ? 121 : 118, x0 = high ? 15 : 13, cy = high ? 172 : 150;
        for (int i = 0; i <= 14; i++) {
            double t = i / 14.0;
            double x = (1 - t) * (1 - t) * -x0 + 2 * (1 - t) * t * 0 + t * t * x0;
            double y = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * cy + t * t * y0;
            g.setColor(new Color(0xF4EFE6));
            g.fill(new Ellipse2D.Double(x - 2.3, y - 2.3, 4.6, 4.6));
            g.setColor(new Color(0xBDB3A2));
            g.setStroke(new BasicStroke(0.5f));
            g.draw(new Ellipse2D.Double(x - 2.3, y - 2.3, 4.6, 4.6));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x - 1.3, y - 1.4, 1.2, 1.2));
        }
    }

    static Shape panel(double e, double hem, double flare, double front, double frontTop, boolean nipped) {
        List<P> l = L(p(-12, 111), p(-15, 116), p(-32, 120), p(-(51 + e * 0.4), 125.5));
        for (double y = 136; y < hem - 4; y += 8) {
            double base = y <= 300 ? (nipped ? bodyW(y) : Math.max(bodyW(y), y > 150 ? 43 : 0)) : 45 + (y - 300) * flare;
            l.add(p(-(base + e), y));
        }
        double hx = (hem <= 300 ? (nipped ? bodyW(hem) : Math.max(bodyW(hem), 43)) : 45 + (hem - 300) * flare) + e;
        l.add(p(-hx, hem)); l.add(p(-hx, hem));
        l.add(p(-front, hem)); l.add(p(-front, hem));
        l.add(p(-front, frontTop)); l.add(p(-front, frontTop));
        return smooth(l);
    }

    static List<P> lapel(String s) {
        return switch (s) {
            case "Blazer" -> L(p(-12, 111), p(-27, 131), p(-26, 150), p(-19, 147), p(1, 205));
            case "Trench coat" -> L(p(-12, 111), p(-31, 133), p(-31, 160), p(-23, 157), p(7, 190));
            case "Long coat" -> L(p(-12, 111), p(-30, 133), p(-29, 166), p(-22, 162), p(-7, 205));
            case "Cropped jacket" -> L(p(-12, 111), p(-25, 121), p(-18, 137), p(-9, 124));
            default -> null;
        };
    }

    static void drawLapel(Graphics2D g, String s, Piece p, boolean right) {
        List<P> l = lapel(s);
        if (l == null) return;
        Shape lap = right ? mirror(poly(l)) : poly(l);
        cloth(g, lap, p);
        g.setColor(new Color(0, 0, 0, 22));
        g.fill(lap);
        g.setColor(edge(p.c1));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(lap);
    }

    static void button(Graphics2D g, Piece p, double x, double y) {
        Shape b = new Ellipse2D.Double(x - 3, y - 3, 6, 6);
        g.setColor(mix(p.c1, Color.BLACK, 0.45));
        g.fill(b);
        g.setColor(alpha(Color.WHITE, 70));
        g.fill(new Ellipse2D.Double(x - 1.8, y - 2, 2, 2));
    }

    static void drawOuter(Graphics2D g, Outfit o) {
        if (!o.has(Cat.OUTER)) return;
        Piece p = o.get(Cat.OUTER);
        String s = o.style(Cat.OUTER);
        double e, hem, flare, front, frontTop, sf = 1, sw = 22;
        boolean nipped;
        switch (s) {
            case "Blazer" -> { e = 7; hem = 296; flare = 0; front = -1; frontTop = 205; nipped = true; }
            case "Trench coat" -> { e = 9; hem = 452; flare = 0.13; front = -7; frontTop = 190; nipped = false; }
            case "Cropped jacket" -> { e = 6; hem = 222; flare = 0; front = 9; frontTop = 118; nipped = true; sf = 0.93; }
            case "Cardigan" -> { e = 6; hem = 302; flare = 0; front = 8; frontTop = 160; nipped = true; sw = 21; }
            default -> { e = 10; hem = 490; flare = 0.1; front = 7; frontTop = 205; nipped = false; }
        }
        Shape sl = sleeve(sf, sw);
        cloth(g, sl, p);
        cloth(g, mirror(sl), p);
        Shape left = panel(e, hem, flare, front, frontTop, nipped), right = mirror(left);
        cloth(g, right, p);
        drawLapel(g, s, p, true);
        cloth(g, left, p);
        drawLapel(g, s, p, false);
        Color dc = detail(p);
        g.setColor(dc);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (s) {
            case "Blazer" -> {
                button(g, p, -3, 228);
                lineM(g, -40, 262, -24, 262);
            }
            case "Trench coat" -> {
                double half = Math.max(bodyW(214), 43) + e + 1.5;
                Shape belt = new Rectangle2D.Double(-half, 208, half * 2, 11);
                cloth(g, belt, p);
                g.setColor(edge(p.c1));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new RoundRectangle2D.Double(-30, 206, 11, 15, 2, 2));
                cloth(g, new Rectangle2D.Double(-31, 219, 6, 24), p);
                for (double y : new double[]{196, 240, 268}) { button(g, p, 2, y); button(g, p, -14, y); }
            }
            case "Cropped jacket" -> {
                g.draw(new Rectangle2D.Double(-36, 150, 13, 10));
                g.draw(new Rectangle2D.Double(23, 150, 13, 10));
                line(g, -50, hem - 6, 50, hem - 6);
                button(g, p, -12.5, 140); button(g, p, -12.5, 170); button(g, p, -12.5, 200);
            }
            case "Cardigan" -> {
                Path2D band = new Path2D.Double();
                band.moveTo(-12, 112); band.lineTo(-8, frontTop); band.lineTo(-8, hem);
                Shape bandL = new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(band);
                for (Shape b : new Shape[]{bandL, mirror(bandL)}) {
                    g.setColor(mix(p.c1, Color.BLACK, 0.1));
                    g.fill(b);
                    g.setColor(edge(p.c1));
                    g.setStroke(new BasicStroke(0.8f));
                    g.draw(b);
                }
                g.setColor(dc);
                clipDraw(g, new Area(left) {{ add(new Area(right)); }}, () -> line(g, -60, hem - 8, 60, hem - 8));
                button(g, p, -8, 175); button(g, p, -8, 205); button(g, p, -8, 235);
            }
            default -> { button(g, p, -12, 222); button(g, p, -12, 266); lineM(g, -42, 300, -28, 300); }
        }
    }

    static void drawBag(Graphics2D g, Outfit o, Color skin) {
        if (!o.has(Cat.BAG)) return;
        Piece p = o.get(Cat.BAG);
        switch (o.style(Cat.BAG)) {
            case "Crossbody" -> {
                g.setColor(edge(p.c1));
                g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                line(g, -36, 123, 40, 258);
                g.setColor(p.c1);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                line(g, -36, 123, 40, 258);
                cloth(g, new RoundRectangle2D.Double(30, 256, 30, 25, 7, 7), p);
                Shape flap = new RoundRectangle2D.Double(30, 256, 30, 13, 7, 7);
                cloth(g, flap, p);
                g.setColor(new Color(0, 0, 0, 25));
                g.fill(flap);
                g.setColor(new Color(0xC9A24B));
                g.fill(new Ellipse2D.Double(43, 266, 4, 4));
            }
            case "Tote" -> {
                g.setColor(edge(p.c1));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(quad(50, 254, 58, 224, 70, 254));
                g.draw(quad(56, 254, 66, 228, 78, 254));
                cloth(g, poly(L(p(44, 252), p(82, 252), p(87, 322), p(39, 322))), p);
            }
            case "Clutch" -> {
                cloth(g, new RoundRectangle2D.Double(50, 299, 36, 19, 4, 4), p);
                Shape flap = poly(L(p(50, 300), p(86, 300), p(68, 311)));
                cloth(g, flap, p);
                g.setColor(new Color(0xC9A24B));
                g.fill(new Ellipse2D.Double(66, 308.5, 4, 4));
                Shape hand = mirror(handShape());
                g.setColor(skin);
                g.fill(hand);
                g.setColor(mix(skin, Color.BLACK, 0.32));
                g.setStroke(new BasicStroke(1f));
                g.draw(hand);
            }
            default -> { }
        }
    }

    static void drawHat(Graphics2D g, Outfit o) {
        if (!o.has(Cat.HAT)) return;
        Piece p = o.get(Cat.HAT);
        Piece band = new Piece(1, p.c2, p.c2, Print.SOLID);
        switch (o.style(Cat.HAT)) {
            case "Beret" -> {
                Shape b = AffineTransform.getRotateInstance(Math.toRadians(-9), 2, 47)
                        .createTransformedShape(new Ellipse2D.Double(-31, 35, 64, 24));
                cloth(g, b, p);
                g.setColor(edge(p.c1));
                g.fill(new Rectangle2D.Double(1, 31, 3, 5));
            }
            case "Sun hat" -> {
                Shape brim = new Ellipse2D.Double(-63, 40, 126, 19);
                Shape crown = new RoundRectangle2D.Double(-23, 17, 46, 33, 24, 24);
                cloth(g, brim, p);
                cloth(g, crown, p);
                clipDraw(g, crown, () -> cloth(g, new Rectangle2D.Double(-24, 38, 48, 6), band));
                Area front = new Area(brim);
                front.intersect(new Area(new Rectangle2D.Double(-70, 49.5, 140, 20)));
                cloth(g, front, p);
            }
            case "Fedora" -> {
                Shape brim = new Ellipse2D.Double(-41, 43, 82, 13);
                Path2D crown = new Path2D.Double();
                crown.moveTo(-21, 49); crown.lineTo(-19, 27); crown.quadTo(-10, 22, 0, 27); crown.quadTo(10, 22, 19, 27);
                crown.lineTo(21, 49); crown.closePath();
                cloth(g, brim, p);
                cloth(g, crown, p);
                clipDraw(g, crown, () -> cloth(g, new Rectangle2D.Double(-24, 41, 48, 7), band));
                Area front = new Area(brim);
                front.intersect(new Area(new Rectangle2D.Double(-50, 49.5, 100, 20)));
                cloth(g, front, p);
            }
            default -> { // Beanie
                Area a = new Area(new Ellipse2D.Double(-25.5, 33, 51, 50));
                a.intersect(new Area(new Rectangle2D.Double(-40, 0, 80, 58)));
                cloth(g, a, p);
                Shape cuff = new RoundRectangle2D.Double(-26, 52, 52, 11, 6, 6);
                cloth(g, cuff, p);
                g.setColor(detail(p));
                g.setStroke(new BasicStroke(0.8f));
                for (double x = -23; x <= 23; x += 3.5) line(g, x, 54, x, 61);
                Shape pom = new Ellipse2D.Double(-7, 22, 14, 14);
                cloth(g, pom, band);
            }
        }
    }

    static void drawSunglasses(Graphics2D g) {
        Color lens = new Color(30, 30, 34, 235);
        for (int sx = -1; sx <= 1; sx += 2) {
            Shape l = new RoundRectangle2D.Double(sx < 0 ? -17.5 : 2.5, 64.5, 15, 10.5, 7, 7);
            g.setColor(lens);
            g.fill(l);
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(0.9f));
            line(g, sx < 0 ? -14 : 6, 67, sx < 0 ? -10 : 10, 66);
        }
        g.setColor(new Color(30, 30, 34));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(quad(-2.5, 67.5, 0, 65.5, 2.5, 67.5));
        lineM(g, -17.5, 67.5, -21.5, 66.5);
    }

    // =====================================================================
    //  Style advisor
    // =====================================================================

    record Tone(Color c, boolean neutral) {}
    record Advice(int score, List<Tone> tones, List<String> notes) {}

    static boolean isNeutral(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        if (hsb[1] < 0.16f || hsb[2] < 0.17f) return true;
        NamedColor n = nearest(c);
        return n.neutral() && dist(c, n.color()) < 45;
    }

    static float hue(Color c) { return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0] * 360f; }
    static float hueDist(float a, float b) { float d = Math.abs(a - b) % 360; return d > 180 ? 360 - d : d; }

    static List<Cat> visible(Outfit o) {
        List<Cat> v = new ArrayList<>();
        if (o.has(Cat.DRESS)) v.add(Cat.DRESS);
        else { if (o.has(Cat.TOP)) v.add(Cat.TOP); if (o.has(Cat.BOTTOM)) v.add(Cat.BOTTOM); }
        for (Cat c : new Cat[]{Cat.OUTER, Cat.SHOES, Cat.BAG, Cat.HAT}) if (o.has(c)) v.add(c);
        return v;
    }

    static void addTone(List<Tone> tones, Color c, boolean forceNeutral) {
        for (Tone t : tones) if (dist(t.c(), c) < 30) return;
        tones.add(new Tone(c, forceNeutral || isNeutral(c)));
    }

    static Advice advise(Outfit o) {
        List<String> notes = new ArrayList<>();
        List<Tone> tones = new ArrayList<>();
        List<Cat> vis = visible(o);
        if (vis.isEmpty()) {
            notes.add("Your model is waiting! Choose some pieces on the left, or try <b>Style me</b>.");
            return new Advice(0, tones, notes);
        }
        List<Piece> printed = new ArrayList<>();
        boolean leopard = false;
        for (Cat c : vis) {
            Piece p = o.get(c);
            addTone(tones, p.c1, p.print == Print.DENIM);
            if (p.print != Print.SOLID && p.print != Print.DENIM && p.print != Print.KNIT) {
                printed.add(p);
                addTone(tones, p.c2, false);
                if (p.print == Print.LEOPARD) leopard = true;
            }
        }
        if (o.belt && o.has(Cat.SHOES)) addTone(tones, o.get(Cat.SHOES).c1, false);

        int score = 6;
        List<Color> accents = new ArrayList<>();
        for (Tone t : tones) if (!t.neutral()) accents.add(t.c());
        List<Float> fam = new ArrayList<>();
        for (Color c : accents) {
            float h = hue(c);
            boolean found = false;
            for (float f : fam) if (hueDist(h, f) < 24) { found = true; break; }
            if (!found) fam.add(h);
        }
        boolean allAccent = accents.size() == tones.size();
        switch (fam.size()) {
            case 0 -> {
                score += 1;
                notes.add("<b>All neutrals.</b> Quiet, polished and easy to wear." + (printed.isEmpty()
                        ? " A print or a texture would stop it feeling flat." : " The print adds just enough interest."));
            }
            case 1 -> {
                score += 2;
                if (allAccent || accents.size() > 1)
                    notes.add("<b>Tonal dressing.</b> Shades of one colour head to toe lengthen the silhouette — very elegant.");
                else
                    notes.add("<b>One accent colour</b> (" + colourName(accents.get(0)) + ") against neutrals — the stylist's favourite formula.");
            }
            case 2 -> {
                float d = hueDist(fam.get(0), fam.get(1));
                if (d < 50) { score += 2; notes.add("<b>Analogous colours.</b> Your two accents sit side by side on the colour wheel, so they feel calm and harmonious."); }
                else if (d >= 150) { score += 2; notes.add("<b>Complementary colours.</b> Opposites on the colour wheel — bold and energetic. Let one dominate and use the other as a pop."); }
                else { score += 1; notes.add("<b>Colour blocking.</b> Two distinct accents. Keep everything else neutral so they can shine."); }
            }
            case 3 -> {
                boolean triad = true;
                for (int i = 0; i < 3; i++) for (int j = i + 1; j < 3; j++) {
                    float d = hueDist(fam.get(i), fam.get(j));
                    if (d < 90 || d > 150) triad = false;
                }
                if (triad) { score += 1; notes.add("<b>Triadic palette.</b> Three colours evenly spaced around the wheel — playful and balanced."); }
                else notes.add("<b>Three accent colours.</b> Brave! Ground them with neutral shoes and bag.");
            }
            default -> { score -= 1; notes.add("<b>Lots of colour</b> (" + fam.size() + " colour families). Try the rule of three: no more than three colours in one look."); }
        }

        if (printed.size() == 1) {
            score += 1;
            notes.add("<b>One print as the star.</b> Let the " + printed.get(0).print.label.toLowerCase() + " take centre stage.");
        } else if (printed.size() == 2) {
            Piece a = printed.get(0), b = printed.get(1);
            boolean shared = dist(a.c1, b.c1) < 40 || dist(a.c1, b.c2) < 40 || dist(a.c2, b.c1) < 40 || dist(a.c2, b.c2) < 40;
            if (shared) { score += 1; notes.add("<b>Print mixing, done right.</b> Your two prints share a colour, which ties them together."); }
            else notes.add("<b>Print mixing.</b> It works best when the prints share a colour and differ in scale — one big, one small.");
        } else if (printed.size() >= 3) {
            score -= 1;
            notes.add("<b>That's a lot of pattern.</b> Calm things down by swapping one piece for a solid.");
        }
        if (leopard) notes.add("Leopard behaves like a neutral — it goes with almost everything.");

        if (o.has(Cat.SHOES) && o.has(Cat.BAG) && dist(o.get(Cat.SHOES).c1, o.get(Cat.BAG).c1) < 35)
            notes.add("Shoes and bag match — polished and a little retro. Contrast them for a more modern feel.");
        if (o.belt && o.has(Cat.SHOES)) notes.add("The belt picks up the shoe colour — a classic finishing touch.");

        // Signature combinations
        String top = o.has(Cat.DRESS) ? "" : o.style(Cat.TOP), bottom = o.has(Cat.DRESS) ? "" : o.style(Cat.BOTTOM);
        String dress = o.style(Cat.DRESS), outer = o.style(Cat.OUTER), shoes = o.style(Cat.SHOES);
        if (o.has(Cat.TOP) && !o.has(Cat.DRESS) && o.get(Cat.TOP).print == Print.STRIPES && outer.equals("Trench coat")) {
            score += 1; notes.add("Breton stripes and a trench: the Parisian uniform. <i>Très chic.</i>");
        }
        if ((bottom.equals("Mini skirt") || dress.equals("Shift dress")) && shoes.equals("Knee boots")) {
            score += 1; notes.add("Short hem and knee boots — pure sixties mod.");
        }
        if (bottom.equals("Wide-leg trousers") && o.has(Cat.TOP) && !o.tuck && !top.equals("Crop top"))
            notes.add("Tip: tuck the top in (Model page) to define the waist above wide-leg trousers.");
        if (top.equals("Crop top") && o.has(Cat.BOTTOM))
            notes.add("A crop top with a high waist shows just a sliver of skin — nicely balanced proportions.");
        if (shoes.equals("Trainers") && (o.has(Cat.DRESS) || bottom.contains("skirt")))
            notes.add("Trainers with a skirt or dress: effortless off-duty style.");
        if (o.pearls && (shoes.equals("Trainers") || hasDenim(o)))
            notes.add("Pearls with something casual — the high/low mix stylists love.");
        if (dress.equals("Maxi dress") && o.style(Cat.HAT).equals("Sun hat"))
            notes.add("Maxi dress and a sun hat: holiday-ready.");
        boolean black = false, navy = false;
        for (Tone t : tones) { if (dist(t.c(), col("Black")) < 25) black = true; if (dist(t.c(), col("Navy")) < 25) navy = true; }
        if (black && navy) notes.add("Black and navy together — once a fashion 'rule' to avoid, now quietly chic.");

        String season = season(accents);
        if (season != null) notes.add(season);

        return new Advice(Math.max(3, Math.min(10, score)), tones, notes);
    }

    static boolean hasDenim(Outfit o) {
        for (Cat c : visible(o)) if (o.get(c).print == Print.DENIM) return true;
        return false;
    }

    static String season(List<Color> accents) {
        if (accents.isEmpty()) return null;
        double warm = 0, light = 0;
        for (Color c : accents) {
            float h = hue(c);
            if (h < 65 || h >= 330) warm += 1;
            else if (h < 160) warm += h < 100 ? 0.5 : -0.5;
            else warm -= 1;
            int mx = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue())), mn = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
            light += (mx + mn) / 510.0;
        }
        boolean isWarm = warm >= 0, isLight = light / accents.size() > 0.6;
        String name, why;
        if (isWarm && isLight) { name = "Spring"; why = "warm and bright — lovely on peachy, golden colouring"; }
        else if (isWarm) { name = "Autumn"; why = "warm and rich — gorgeous on golden or olive undertones"; }
        else if (isLight) { name = "Summer"; why = "cool and soft — flattering on rosy, pink undertones"; }
        else { name = "Winter"; why = "cool and vivid — striking on high-contrast colouring"; }
        return "<b>Colour season:</b> this palette leans <i>" + name + "</i>, " + why + ".";
    }

    static String adviceHtml(Advice a) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif; font-size:11px; margin:6px;'>");
        sb.append("<div style='font-family:serif; font-size:20px;'><i>Style notes</i></div>");
        if (a.score() > 0) {
            sb.append("<p><b>Style score:</b> <font color='#B8902F'>");
            sb.append("★".repeat(a.score())).append("</font><font color='#CCCCCC'>").append("★".repeat(10 - a.score()));
            sb.append("</font> ").append(a.score()).append("/10</p>");
        }
        if (!a.tones().isEmpty()) {
            sb.append("<p><b>Palette</b></p><table cellspacing='2' cellpadding='1'>");
            int i = 0;
            for (Tone t : a.tones()) {
                if (i % 2 == 0) sb.append("<tr>");
                sb.append("<td bgcolor='").append(hex(t.c())).append("' width='16'>&nbsp;</td><td width='110'>")
                  .append(colourName(t.c())).append(t.neutral() ? " <font color='#999999'>(neutral)</font>" : "").append("</td>");
                if (i % 2 == 1) sb.append("</tr>");
                i++;
            }
            if (i % 2 == 1) sb.append("</tr>");
            sb.append("</table>");
        }
        sb.append("<ul style='margin-left:14px;'>");
        for (String n : a.notes()) sb.append("<li style='margin-bottom:5px;'>").append(n).append("</li>");
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    // =====================================================================
    //  User interface
    // =====================================================================

    /** A grid of clickable colour chips. */
    static final class SwatchGrid extends JPanel {
        private Color selected;
        private final Consumer<Color> onPick;
        private final JPanel grid;

        SwatchGrid(List<NamedColor> colours, int columns, boolean custom, Consumer<Color> onPick) {
            super(new BorderLayout(0, 4));
            setOpaque(false);
            this.onPick = onPick;
            grid = new JPanel(new GridLayout(0, columns, 3, 3));
            grid.setOpaque(false);
            for (NamedColor n : colours) grid.add(new Chip(n));
            add(grid, BorderLayout.CENTER);
            if (custom) {
                JButton b = new JButton("Custom colour…");
                b.addActionListener(e -> {
                    Color c = JColorChooser.showDialog(this, "Choose a colour", selected);
                    if (c != null) pick(c);
                });
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                row.setOpaque(false);
                row.add(b);
                add(row, BorderLayout.SOUTH);
            }
            setAlignmentX(LEFT_ALIGNMENT);
        }

        void setSelected(Color c) { selected = c; grid.repaint(); }
        void pick(Color c) { selected = c; grid.repaint(); onPick.accept(c); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        final class Chip extends JComponent {
            final NamedColor nc;
            Chip(NamedColor nc) {
                this.nc = nc;
                setPreferredSize(new Dimension(26, 26));
                setToolTipText(nc.name());
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) { pick(nc.color()); }
                });
            }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = Math.min(getWidth(), getHeight()) - 5, x = (getWidth() - s) / 2, y = (getHeight() - s) / 2;
                g.setColor(nc.color());
                g.fillRoundRect(x, y, s, s, 8, 8);
                g.setColor(new Color(0, 0, 0, 60));
                g.drawRoundRect(x, y, s, s, 8, 8);
                if (selected != null && selected.getRGB() == nc.color().getRGB()) {
                    g.setStroke(new BasicStroke(2.2f));
                    g.setColor(new Color(200, 40, 110));
                    g.drawRoundRect(x - 1, y - 1, s + 2, s + 2, 10, 10);
                }
                g.dispose();
            }
        }
    }

    private Outfit outfit = defaultOutfit();
    private boolean updating;
    private final List<Outfit> lookbook = new ArrayList<>();
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<PiecePanel> piecePanels = new ArrayList<>();
    private final Random random = new Random();
    private JFrame frame;
    private JPanel canvas, strip;
    private JEditorPane notes;
    private JTextField nameField;
    private JComboBox<String> backdropBox;

    static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Serif", Font.ITALIC, 24));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JComponent section(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(l.getFont().deriveFont(Font.BOLD, 10.5f));
        l.setForeground(new Color(150, 60, 95));
        l.setBorder(new EmptyBorder(12, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JComponent blurb(String text) {
        JLabel l = new JLabel("<html><div style='width:240px;'>" + text + "</div></html>");
        l.setForeground(new Color(95, 85, 90));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static <T extends JComponent> T fullWidth(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    final class PiecePanel extends JPanel {
        final Cat cat;
        final JList<String> list;
        final SwatchGrid main, second;
        final JComboBox<Print> printBox;
        final JLabel hint = new JLabel(" ");

        PiecePanel(Cat cat, String text) {
            this.cat = cat;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(10, 14, 14, 14));
            add(heading(cat.label));
            add(blurb(text));
            hint.setForeground(new Color(180, 50, 95));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            add(hint);

            add(section("Style"));
            list = new JList<>(cat.styles);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(Math.min(cat.styles.length, 9));
            list.addListSelectionListener(e -> {
                if (updating || e.getValueIsAdjusting() || list.getSelectedIndex() < 0) return;
                outfit.get(cat).style = list.getSelectedIndex();
                changed();
            });
            add(fullWidth(new JScrollPane(list)));

            add(section("Colour"));
            main = new SwatchGrid(PALETTE, 8, true, c -> { if (!updating) { outfit.get(cat).c1 = c; changed(); } });
            add(main);

            add(section("Print / fabric"));
            printBox = new JComboBox<>(Print.values());
            printBox.addActionListener(e -> {
                if (updating) return;
                outfit.get(cat).print = (Print) printBox.getSelectedItem();
                changed();
            });
            add(fullWidth(printBox));

            String second2 = switch (cat) {
                case SHOES -> "Second colour (prints, trainer soles)";
                case HAT -> "Second colour (prints, hat band, pompom)";
                default -> "Second colour (for prints)";
            };
            add(section(second2));
            second = new SwatchGrid(PALETTE, 8, true, c -> { if (!updating) { outfit.get(cat).c2 = c; changed(); } });
            add(second);
            add(Box.createVerticalGlue());
            refreshers.add(this::refresh);
        }

        void refresh() {
            Piece p = outfit.get(cat);
            list.setSelectedIndex(p.style);
            list.ensureIndexIsVisible(p.style);
            main.setSelected(p.c1);
            second.setSelected(p.c2);
            printBox.setSelectedItem(p.print);
            updateHint();
        }

        void updateHint() {
            boolean hidden = (cat == Cat.TOP || cat == Cat.BOTTOM) && outfit.has(Cat.DRESS);
            hint.setText(hidden ? "<html><div style='width:240px;'>Hidden while a dress is chosen — set Dress to None to wear separates.</div></html>" : " ");
        }
    }

    JPanel buildModelPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 14, 14, 14));
        panel.add(heading("Model"));
        panel.add(blurb("Your model's look, plus the little finishing touches that pull an outfit together."));

        panel.add(section("Skin tone"));
        SwatchGrid skin = new SwatchGrid(SKIN, 6, false, c -> {
            if (updating) return;
            for (int i = 0; i < SKIN.size(); i++) if (SKIN.get(i).color().equals(c)) outfit.skin = i;
            changed();
        });
        panel.add(skin);

        panel.add(section("Hair style"));
        JList<String> hair = new JList<>(HAIR_STYLES);
        hair.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hair.setVisibleRowCount(HAIR_STYLES.length);
        hair.addListSelectionListener(e -> {
            if (updating || e.getValueIsAdjusting() || hair.getSelectedIndex() < 0) return;
            outfit.hair = hair.getSelectedIndex();
            changed();
        });
        panel.add(fullWidth(new JScrollPane(hair)));

        panel.add(section("Hair colour"));
        SwatchGrid hairCol = new SwatchGrid(HAIR_COLOURS, 8, true, c -> { if (!updating) { outfit.hairColor = c; changed(); } });
        panel.add(hairCol);

        panel.add(section("Finishing touches"));
        JCheckBox tuck = new JCheckBox("Tuck the top in");
        JCheckBox belt = new JCheckBox("Belt (always matches the shoes)");
        JCheckBox glasses = new JCheckBox("Sunglasses");
        JCheckBox pearls = new JCheckBox("String of pearls");
        for (JCheckBox b : new JCheckBox[]{tuck, belt, glasses, pearls}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setOpaque(false);
            panel.add(b);
        }
        tuck.addActionListener(e -> { if (!updating) { outfit.tuck = tuck.isSelected(); changed(); } });
        belt.addActionListener(e -> { if (!updating) { outfit.belt = belt.isSelected(); changed(); } });
        glasses.addActionListener(e -> { if (!updating) { outfit.sunglasses = glasses.isSelected(); changed(); } });
        pearls.addActionListener(e -> { if (!updating) { outfit.pearls = pearls.isSelected(); changed(); } });
        panel.add(Box.createVerticalGlue());

        refreshers.add(() -> {
            skin.setSelected(SKIN.get(outfit.skin).color());
            hair.setSelectedIndex(outfit.hair);
            hairCol.setSelected(outfit.hairColor);
            tuck.setSelected(outfit.tuck);
            belt.setSelected(outfit.belt);
            glasses.setSelected(outfit.sunglasses);
            pearls.setSelected(outfit.pearls);
        });
        return panel;
    }

    JComponent buildEditor() {
        Map<Cat, String> blurbs = Map.of(
                Cat.TOP, "Tees, blouses and knits. Tuck it in from the Model page.",
                Cat.BOTTOM, "Trousers, jeans and skirts — the foundation of the silhouette.",
                Cat.DRESS, "A dress replaces the top and bottoms. Choose None to go back to separates.",
                Cat.OUTER, "The third piece. A jacket, coat or cardigan makes an outfit look finished.",
                Cat.SHOES, "Shoes set the mood. The belt on the Model page always matches them.",
                Cat.BAG, "Match it to your shoes for polish, or make it the pop of colour.",
                Cat.HAT, "The final flourish. A hat's second colour is its band or pompom.");
        CardLayout cards = new CardLayout();
        JPanel cardPanel = new JPanel(cards);
        List<String> names = new ArrayList<>();
        for (Cat c : Cat.values()) {
            PiecePanel pp = new PiecePanel(c, blurbs.get(c));
            piecePanels.add(pp);
            cardPanel.add(pp, c.name());
            names.add(c.label);
        }
        cardPanel.add(buildModelPanel(), "MODEL");
        names.add("Model");

        JList<String> catList = new JList<>(names.toArray(new String[0]));
        catList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        catList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, sel, foc);
                c.setBorder(new EmptyBorder(9, 12, 9, 12));
                c.setFont(new Font("Serif", Font.PLAIN, 16));
                return c;
            }
        });
        catList.addListSelectionListener(e -> {
            int i = catList.getSelectedIndex();
            if (i < 0) return;
            cards.show(cardPanel, i < Cat.values().length ? Cat.values()[i].name() : "MODEL");
        });
        catList.setSelectedIndex(0);

        JScrollPane cardScroll = new JScrollPane(cardPanel);
        cardScroll.setBorder(null);
        cardScroll.setPreferredSize(new Dimension(360, 100));
        cardScroll.getVerticalScrollBar().setUnitIncrement(16);
        cardScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollPane listScroll = new JScrollPane(catList);
        listScroll.setPreferredSize(new Dimension(118, 100));
        JPanel west = new JPanel(new BorderLayout());
        west.add(listScroll, BorderLayout.WEST);
        west.add(cardScroll, BorderLayout.CENTER);
        return west;
    }

    JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.add(new JLabel("Look name:"));
        nameField = new JTextField(outfit.name, 14);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            void update() { if (!updating) { outfit.name = nameField.getText(); canvas.repaint(); } }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
        bar.add(nameField);

        bar.add(Box.createHorizontalStrut(8));
        JComboBox<String> presets = new JComboBox<>(PRESETS);
        bar.add(new JLabel("Style:"));
        bar.add(presets);
        JButton dressMe = new JButton("Dress me");
        dressMe.addActionListener(e -> { applyPreset(outfit, (String) presets.getSelectedItem()); refreshAll(); });
        bar.add(dressMe);
        JButton surprise = new JButton("Surprise me!");
        surprise.addActionListener(e -> { surprise(outfit, random); refreshAll(); });
        bar.add(surprise);

        bar.add(Box.createHorizontalStrut(8));
        bar.add(new JLabel("Backdrop:"));
        backdropBox = new JComboBox<>(BACKDROPS);
        backdropBox.addActionListener(e -> { if (!updating) { outfit.backdrop = backdropBox.getSelectedIndex(); changed(); } });
        bar.add(backdropBox);

        bar.add(Box.createHorizontalStrut(8));
        JButton save = new JButton("Save to lookbook");
        save.addActionListener(e -> { lookbook.add(outfit.copy()); rebuildStrip(); });
        bar.add(save);
        JButton export = new JButton("Export picture…");
        export.addActionListener(e -> exportPng());
        bar.add(export);
        JButton about = new JButton("About");
        about.addActionListener(e -> showAbout());
        bar.add(about);
        return bar;
    }

    JComponent buildNotes() {
        notes = new JEditorPane("text/html", "");
        notes.setEditable(false);
        notes.setBackground(new Color(252, 249, 246));
        JScrollPane sp = new JScrollPane(notes);
        sp.setPreferredSize(new Dimension(290, 100));
        sp.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(225, 215, 210)));
        return sp;
    }

    JComponent buildLookbook() {
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 215, 210)));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JLabel title = new JLabel("Lookbook");
        title.setFont(new Font("Serif", Font.ITALIC, 18));
        head.add(title);
        JButton open = new JButton("Open…"), saveAs = new JButton("Save…"), clear = new JButton("Clear");
        open.addActionListener(e -> openLookbook());
        saveAs.addActionListener(e -> saveLookbook());
        clear.addActionListener(e -> {
            if (lookbook.isEmpty()) return;
            if (JOptionPane.showConfirmDialog(frame, "Remove every look from the lookbook?", "Clear lookbook",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) { lookbook.clear(); rebuildStrip(); }
        });
        head.add(open); head.add(saveAs); head.add(clear);
        JLabel tip = new JLabel("Click a look to wear it again; right-click to delete.");
        tip.setForeground(Color.GRAY);
        head.add(tip);
        south.add(head, BorderLayout.NORTH);
        strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JScrollPane sp = new JScrollPane(strip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(100, 158));
        sp.setBorder(null);
        south.add(sp, BorderLayout.CENTER);
        rebuildStrip();
        return south;
    }

    static BufferedImage thumbnail(Outfit o, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        render(g, o, w, h, false);
        g.dispose();
        return img;
    }

    void rebuildStrip() {
        strip.removeAll();
        if (lookbook.isEmpty()) {
            JLabel empty = new JLabel("Saved looks appear here — press “Save to lookbook”.");
            empty.setForeground(Color.GRAY);
            empty.setBorder(new EmptyBorder(40, 10, 0, 0));
            strip.add(empty);
        }
        for (Outfit look : lookbook) {
            String label = look.name.length() > 16 ? look.name.substring(0, 15) + "…" : look.name;
            JButton b = new JButton(label, new ImageIcon(thumbnail(look, 80, 108)));
            b.setVerticalTextPosition(SwingConstants.BOTTOM);
            b.setHorizontalTextPosition(SwingConstants.CENTER);
            b.setToolTipText(look.name);
            b.addActionListener(e -> { outfit = look.copy(); refreshAll(); });
            JPopupMenu menu = new JPopupMenu();
            JMenuItem del = new JMenuItem("Delete “" + look.name + "”");
            del.addActionListener(e -> { lookbook.remove(look); rebuildStrip(); });
            menu.add(del);
            b.setComponentPopupMenu(menu);
            strip.add(b);
        }
        strip.revalidate();
        strip.repaint();
    }

    void changed() {
        canvas.repaint();
        for (PiecePanel pp : piecePanels) pp.updateHint();
        notes.setText(adviceHtml(advise(outfit)));
        notes.setCaretPosition(0);
    }

    void refreshAll() {
        updating = true;
        for (Runnable r : refreshers) r.run();
        nameField.setText(outfit.name);
        backdropBox.setSelectedIndex(outfit.backdrop);
        updating = false;
        changed();
    }

    static String safeName(String s) {
        String n = s.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return n.isEmpty() ? "look" : n;
    }

    void exportPng() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(safeName(outfit.name) + ".png"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getParentFile(), f.getName() + ".png");
        try {
            ImageIO.write(exportImage(outfit), "png", f);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Couldn't save the picture:\n" + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    static BufferedImage exportImage(Outfit o) {
        int w = 1000, h = 1400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        render(g, o, w, h, true);
        Advice a = advise(o);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(o.backdrop == 1 ? new Color(230, 225, 220) : new Color(70, 60, 64));
        String foot = (a.score() > 0 ? "Style score " + a.score() + "/10  ·  " : "") + "Made with Fashion Studio";
        g.drawString(foot, w - g.getFontMetrics().stringWidth(foot) - 30, h - 28);
        g.dispose();
        return img;
    }

    void saveLookbook() {
        if (lookbook.isEmpty()) { JOptionPane.showMessageDialog(frame, "The lookbook is empty — save a look first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("My lookbook.lookbook"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().endsWith(".lookbook")) f = new File(f.getParentFile(), f.getName() + ".lookbook");
        List<String> lines = new ArrayList<>();
        lines.add("# Fashion Studio lookbook " + VERSION);
        for (Outfit o : lookbook) lines.add(o.toLine());
        try {
            Files.write(f.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Couldn't save the lookbook:\n" + ex.getMessage(), "Save", JOptionPane.ERROR_MESSAGE);
        }
    }

    void openLookbook() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            int added = 0;
            for (String line : Files.readAllLines(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                lookbook.add(Outfit.parse(line));
                added++;
            }
            rebuildStrip();
            if (added == 0) JOptionPane.showMessageDialog(frame, "No looks were found in that file.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Couldn't open the lookbook:\n" + ex.getMessage(), "Open", JOptionPane.ERROR_MESSAGE);
        }
    }

    void showAbout() {
        String html = "<html><div style='width:320px;'>"
                + "<div style='font-family:serif; font-size:22px;'><i>Fashion Studio</i></div>"
                + "<p>Version " + VERSION + "</p><br>"
                + "<p>A design studio for fashion lovers. Dress the model, play with colour and print, "
                + "get styling notes and build a lookbook of your favourite outfits.</p><br>"
                + "<p>Copyright Mario Gianota 2026. All rights reserved.</p><br>"
                + "<p>Feature requests, bugs and suggestions:<br><b>mariogianota@protonmail.com</b></p>"
                + "</div></html>";
        JOptionPane.showMessageDialog(frame, html, "About Fashion Studio", JOptionPane.PLAIN_MESSAGE);
    }

    void build() {
        frame = new JFrame("Fashion Studio");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                render(g2, outfit, getWidth(), getHeight(), true);
                g2.dispose();
            }
        };
        canvas.setPreferredSize(new Dimension(500, 600));
        JComponent toolbar = buildToolbar();
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(buildEditor(), BorderLayout.WEST);
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(buildNotes(), BorderLayout.EAST);
        frame.add(buildLookbook(), BorderLayout.SOUTH);
        frame.pack();
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setLocationRelativeTo(null);
        refreshAll();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Fashion Studio");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            new FashionStudio().build();
        });
    }
}
