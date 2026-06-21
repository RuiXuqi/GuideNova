package guideme.color;

public final class ARGB {
    public static int alpha(int packedColor) {
        return packedColor >>> 24;
    }

    public static int red(int packedColor) {
        return packedColor >> 16 & 0xFF;
    }

    public static int green(int packedColor) {
        return packedColor >> 8 & 0xFF;
    }

    public static int blue(int packedColor) {
        return packedColor & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int multiply(int packedColourOne, int packedColorTwo) {
        return color(
                alpha(packedColourOne) * alpha(packedColorTwo) / 0xFF,
                red(packedColourOne) * red(packedColorTwo) / 0xFF,
                green(packedColourOne) * green(packedColorTwo) / 0xFF,
                blue(packedColourOne) * blue(packedColorTwo) / 0xFF);
    }
}
