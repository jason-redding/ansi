package com.witcraft.ansi;

import java.util.*;
import java.util.function.Function;

import static com.witcraft.ansi.AnsiSequence.Style.*;

public interface AnsiSequence<T extends AnsiSequence> {
    String CSI_START = "\033[";
    String CSI_END = "m";

    Function<Object, String> HANDLER_INDEX = (color) -> {
        if (color instanceof Number) {
            int colorIndex = ((Number)color).intValue();
            if (colorIndex >= 0 && colorIndex <= 255) {
                return new StringBuilder(6)
                    .append(";5;")
                    .append(colorIndex)
                    .toString();
            }
        }
        return null;
    };

    Function<Object, String> HANDLER_COLOR = (color) -> {
        float red = -1;
        float green = -1;
        float blue = -1;

        if (color instanceof Number) {
            int colorInt = ((Number)color).intValue();
            if (colorInt >= 0) {
                colorInt = (0xff000000 | colorInt);
                red = (colorInt >> 16) & 0xFF;
                green = (colorInt >> 8) & 0xFF;
                blue = (colorInt) & 0xFF;
            }
        } else if (color instanceof Number[] colors) {
            if (colors.length >= 3) {
                int colorDepth = colors.length;
                red = colors[colorDepth - 3].floatValue();
                green = colors[colorDepth - 2].floatValue();
                blue = colors[colorDepth - 1].floatValue();
            }
        }

        if (red > 0 && green > 0 && blue > 0) {
            return new StringBuilder(14)
                .append(";2;")
                .append(red)
                .append(';')
                .append(green)
                .append(';')
                .append(blue)
                .toString();
        }

        return null;
    };

    static int rgb2int(int red, int green, int blue) {
        int rgb = red;
        rgb = (rgb << 8) + green;
        rgb = (rgb << 8) + blue;
        return rgb;
    }

    EnumSet<Style> getStyles();

    Map<Style, Object> getArgumentMap();

    boolean isGrouped();
    T setGrouped(boolean group);

    @SuppressWarnings({"unchecked"})
    default T addStyle(Style... styles) {
        if (styles.length > 0) {
            getStyles().addAll(Arrays.asList(styles));
        }
        return (T)this;
    }

    @SuppressWarnings({"unchecked"})
    default T addStyles(Collection<Style> styles) {
        if (styles != null && !styles.isEmpty()) {
            getStyles().addAll(styles);
        }
        return (T)this;
    }

    default T removeStyle(Style... styles) {
        return removeStyles(Arrays.asList(styles));
    }

    @SuppressWarnings({"unchecked"})
    default T removeStyles(Collection<Style> styles) {
        if (styles != null) {
            getStyles().removeAll(styles);
        }
        return (T)this;
    }

    default String getSequence() {
        return getSequence(isGrouped());
    }

    default String getSequence(boolean grouped) {
        Map<Style, Object> argumentMap = getArgumentMap();
        List<String> resultList = new ArrayList<>();
        for (Style style : getStyles()) {
            Object arg = argumentMap.get(style);
            resultList.add(style.getSequence(arg));
        }
        int itemCount = resultList.size();
        int itemSize = 1;
        int bufferSize =  (itemCount * itemSize) + ((itemCount - 1) * (grouped ? 1 : 3)) + 3;

        return new StringBuilder(bufferSize)
            .append(CSI_START)
            .append(String.join((grouped ? ";" : (CSI_END + CSI_START)), resultList))
            .append(CSI_END)
            .toString();
    }

    @SuppressWarnings({"unchecked"})
    default T styleWithArgument(Style style, Object arg) {
        getStyles().add(style);
        getArgumentMap().put(style, arg);
        return (T)this;
    }

    @SuppressWarnings({"unchecked"})
    default T set(Style... styles) {
        reset();
        getStyles().addAll(Arrays.asList(styles));
        return (T)this;
    }

    default T set(Style style, Object arg) {
        reset();
        return styleWithArgument(style, arg);
    }

    @SuppressWarnings({"unchecked"})
    default T reset() {
        getStyles().clear();
        getArgumentMap().clear();
        return (T)this;
    }

    @SuppressWarnings({"unchecked"})
    default T reset(Style style) {
        reset();
        getStyles().add(style);
        return (T)this;
    }

    default T reset(Style style, Object arg) {
        reset();
        return styleWithArgument(style, arg);
    }

    default T color(int red, int green, int blue) { return color(rgb2int(red, green, blue)); }

    @SuppressWarnings({"unchecked"})
    default T noColor() {
        getStyles().remove(COLOR);
        return (T)this;
    }

    default T color(int color) { return styleWithArgument(COLOR, color); }

    default T bgColor(int red, int green, int blue) { return bgColor(rgb2int(red, green, blue)); }

    default T bgColor(int color) { return styleWithArgument(COLOR, color); }

    default T colorIndex(int color) { return styleWithArgument(COLOR_INDEX, color); }

    default T bgColorIndex(int color) { return styleWithArgument(BACKGROUND_COLOR_INDEX, color); }

    default T and(Style style, Object arg) {
        return styleWithArgument(style, arg);
    }

    default T and(Style... styles) {
        return and(Arrays.asList(styles));
    }

    @SuppressWarnings({"unchecked"})
    default T and(Collection<Style> styles) {
        EnumSet<Style> styleSet = getStyles();
        Map<Style, Object> styleArgumentMap = getArgumentMap();
        styleSet.addAll(styles);
        for (Style style : styles) {
            styleArgumentMap.remove(style);
        }
        return (T)this;
    }

    default T not(Style... styles) {
        return not(Arrays.asList(styles));
    }

    @SuppressWarnings({"unchecked"})
    default T not(Collection<Style> styles) {
        EnumSet<Style> styleSet = getStyles();
        Map<Style, Object> styleArgumentMap = getArgumentMap();
        styleSet.removeAll(styles);
        for (Style style : styleSet) {
            styleArgumentMap.remove(style);
        }
        return (T)this;
    }

    default boolean has(Style... styles) {
        return has(Arrays.asList(styles));
    }

    default boolean has(Collection<Style> styles) {
        return getStyles().containsAll(styles);
    }

    default boolean has(EnumSet<Style> styles) {
        return getStyles().containsAll(styles);
    }

    default boolean is(Style... styles) {
        return is(Arrays.asList(styles));
    }

    default boolean is(Collection<Style> styles) {
        return getStyles().containsAll(styles);
    }

    default boolean is(EnumSet<Style> styles) {
        return getStyles().containsAll(styles);
    }

    default T highIntensity() {
        return and(HIGH_INTENSITY);
    }
    default T bold() {
        return and(BOLD);
    }
    default T lowIntensity() {
        return and(LOW_INTENSITY);
    }
    default T light() {
        return and(LIGHT);
    }

    default T italic() {
        return and(ITALIC);
    }
    default T underline() {
        return and(UNDERLINE);
    }
    default T blink() {
        return and(BLINK);
    }
    default T rapidBlink() {
        return and(RAPID_BLINK);
    }
    default T invert() {
        return and(REVERSE);
    }
    default T reverse() {
        return and(REVERSE);
    }
    default T invisibleText() {
        return and(INVISIBLE_TEXT);
    }

    default T strike() {
        return and(STRIKE);
    }

    default T doubleUnderline() {
        return and(DOUBLE_UNDERLINE);
    }

    default T normalIntensity() {
        return and(NORMAL_INTENSITY);
    }

    default T black() {
        return and(BLACK);
    }
    default T red() {
        return and(RED);
    }
    default T green() {
        return and(GREEN);
    }
    default T yellow() {
        return and(YELLOW);
    }
    default T blue() {
        return and(BLUE);
    }
    default T magenta() {
        return and(MAGENTA);
    }
    default T cyan() {
        return and(CYAN);
    }
    default T white() {
        return and(WHITE);
    }

    default T bgBlack() {
        return and(BACKGROUND_BLACK);
    }
    default T bgRed() {
        return and(BACKGROUND_RED);
    }
    default T bgGreen() {
        return and(BACKGROUND_GREEN);
    }
    default T bgYellow() {
        return and(BACKGROUND_YELLOW);
    }
    default T bgBlue() {
        return and(BACKGROUND_BLUE);
    }
    default T bgMagenta() {
        return and(BACKGROUND_MAGENTA);
    }
    default T bgCyan() {
        return and(BACKGROUND_CYAN);
    }
    default T bgWhite() {
        return and(BACKGROUND_WHITE);
    }

    default T brightBlack() {
        return and(BRIGHT_BLACK);
    }
    default T brightRed() {
        return and(BRIGHT_RED);
    }
    default T brightGreen() {
        return and(BRIGHT_GREEN);
    }
    default T brightYellow() {
        return and(BRIGHT_YELLOW);
    }
    default T brightBlue() {
        return and(BRIGHT_BLUE);
    }
    default T brightMagenta() {
        return and(BRIGHT_MAGENTA);
    }
    default T brightCyan() {
        return and(BRIGHT_CYAN);
    }
    default T brightWhite() {
        return and(BRIGHT_WHITE);
    }

    default T bgBrightBlack() {
        return and(BACKGROUND_BRIGHT_BLACK);
    }
    default T bgBrightRed() {
        return and(BACKGROUND_BRIGHT_RED);
    }
    default T bgBrightGreen() {
        return and(BACKGROUND_BRIGHT_GREEN);
    }
    default T bgBrightYellow() {
        return and(BACKGROUND_BRIGHT_YELLOW);
    }
    default T bgBrightBlue() {
        return and(BACKGROUND_BRIGHT_BLUE);
    }
    default T bgBrightMagenta() {
        return and(BACKGROUND_BRIGHT_MAGENTA);
    }
    default T bgBrightCyan() {
        return and(BACKGROUND_BRIGHT_CYAN);
    }
    default T bgBrightWhite() {
        return and(BACKGROUND_BRIGHT_WHITE);
    }

    default String wrap(String input) {
        String sequence = getSequence();
        String reset = Style.RESET.toString();
        int bufferSize = (sequence.length() + input.length() + reset.length());
        return new StringBuilder(bufferSize)
            .append(getSequence())
            .append(input)
            .append(reset)
            .toString();
    }

    enum Style {
        RESET(0),

        HIGH_INTENSITY(1),
        BOLD(1),
        LOW_INTENSITY(2),
        LIGHT(2),

        ITALIC(3),
        UNDERLINE(4),
        BLINK(5),
        RAPID_BLINK(6),
        REVERSE(7),
        INVISIBLE_TEXT(8),

        STRIKE(9),

        DOUBLE_UNDERLINE(21),

        NORMAL_INTENSITY(22),

        BLACK(30),
        RED(31),
        GREEN(32),
        YELLOW(33),
        BLUE(34),
        MAGENTA(35),
        CYAN(36),
        WHITE(37),
        COLOR_INDEX(38, HANDLER_INDEX),
        COLOR(38, HANDLER_COLOR),

        BACKGROUND_BLACK(40),
        BACKGROUND_RED(41),
        BACKGROUND_GREEN(42),
        BACKGROUND_YELLOW(43),
        BACKGROUND_BLUE(44),
        BACKGROUND_MAGENTA(45),
        BACKGROUND_CYAN(46),
        BACKGROUND_WHITE(47),
        BACKGROUND_COLOR_INDEX(48, HANDLER_INDEX),
        BACKGROUND_COLOR(48, HANDLER_COLOR),

        BRIGHT_BLACK(90),
        BRIGHT_RED(91),
        BRIGHT_GREEN(92),
        BRIGHT_YELLOW(93),
        BRIGHT_BLUE(94),
        BRIGHT_MAGENTA(95),
        BRIGHT_CYAN(96),
        BRIGHT_WHITE(97),

        BACKGROUND_BRIGHT_BLACK(100),
        BACKGROUND_BRIGHT_RED(101),
        BACKGROUND_BRIGHT_GREEN(102),
        BACKGROUND_BRIGHT_YELLOW(103),
        BACKGROUND_BRIGHT_BLUE(104),
        BACKGROUND_BRIGHT_MAGENTA(105),
        BACKGROUND_BRIGHT_CYAN(106),
        BACKGROUND_BRIGHT_WHITE(107);

        final int code;

        private final Function<Object, String> argumentHandler;

        Style(int code) {
            this(code, null);
        }

        Style(int code, Function<Object, String> argumentHandler) {
            this.code = code;
            this.argumentHandler = argumentHandler;
        }

        final int getCode() {
            return code;
        }

        final boolean expectsArgument() {
            return (argumentHandler != null);
        }

        final String getSequence() {
            return getSequence(null);
        }

        final String getSequence(Object argument) {
            boolean expectsArgument = expectsArgument();
            //if (expectsArgument && argument == null) {
            //    throw new IllegalArgumentException("SGR sequence requires a non-null argument.");
            //}
            int bufferSize = 3;
            if (expectsArgument && argument != null) {
                bufferSize += 14;
            }
            StringBuilder result = new StringBuilder(bufferSize);
            result.append(getCode());
            if (expectsArgument) {
                String argResult = argumentHandler.apply(argument);
                if (argResult != null) {
                    result.append(argResult);
                }
            }
            return result.toString();
        }

        @Override
        public String toString() {
            return new StringBuilder(6)
                .append(CSI_START)
                .append(getSequence())
                .append(CSI_END)
                .toString();
        }
    }
}
