package com.witcraft.ansi;

import java.util.*;
import java.util.function.Function;

import static com.witcraft.ansi.AnsiSequence.Style.*;

public interface AnsiSequence<T extends AnsiSequence<T>> {
    String CSI_START = "\033[";
    String CSI_END = "m";

    Function<Object, String> HANDLER_INDEX = (color) -> {
        if (color instanceof Number) {
            int colorIndex = ((Number)color).intValue();
            if (colorIndex >= 0 && colorIndex <= 255) {
                return ";5;" + colorIndex;
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
        } else if (color instanceof Number[]) {
            Number[] colors = (Number[])color;
            if (colors.length >= 3) {
                int colorDepth = colors.length;
                red = colors[colorDepth - 3].floatValue();
                green = colors[colorDepth - 2].floatValue();
                blue = colors[colorDepth - 1].floatValue();
            }
        }

        if (red > 0 && green > 0 && blue > 0) {
            return ";2;" + red + ';' + green + ';' + blue;
        }

        return null;
    };

    static int rgb2int(int red, int green, int blue) {
        int rgb = red;
        rgb = (rgb << 8) + green;
        rgb = (rgb << 8) + blue;
        return rgb;
    }

    /**
     * Returns a map of arguments and their values.
     * @return a map of arguments and their values
     */
    Map<Style, Object> getArgumentMap();

    /**
     * Returns the styles currently applied to this AnsiSequence.
     * @return the styles currently applied to this AnsiSequence
     */
    EnumSet<Style> getStyles();

    /**
     * Returns whether this AnsiSequence groups multiple styles into a single Control Sequence structure, or wraps a Control Sequence structure around each style.
     * @return true if this AnsiSequence groups multiple styles into a single Control Sequence structure, false otherwise.
     */
    boolean isGrouped();

    /**
     * Sets whether this AnsiSequence will group multiple styles into a single Control Sequence structure, or wrap a Control Sequence structure around each style.
     * @param group boolean indicating whether to group multiple style into a single Control Sequence structure
     * @return this AnsiSequence for chaining
     */
    T setGrouped(boolean group);

    @SuppressWarnings({"unchecked", "unused"})
    default T addStyle(Style... styles) {
        if (styles.length > 0) {
            getStyles().addAll(Arrays.asList(styles));
        }
        return (T)this;
    }

    @SuppressWarnings({"unchecked", "unused"})
    default T addStyles(Collection<Style> styles) {
        if (styles != null && !styles.isEmpty()) {
            getStyles().addAll(styles);
        }
        return (T)this;
    }

    @SuppressWarnings("unused")
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

        return CSI_START + String.join((grouped ? ";" : (CSI_END + CSI_START)), resultList) + CSI_END;
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

    @SuppressWarnings({"unchecked", "unused"})
    default T reset(Style style) {
        reset();
        getStyles().add(style);
        return (T)this;
    }

    @SuppressWarnings("unused")
    default T reset(Style style, Object arg) {
        reset();
        return styleWithArgument(style, arg);
    }

    @SuppressWarnings("unused")
    default T color(int red, int green, int blue) {
        return color(rgb2int(red, green, blue));
    }

    @SuppressWarnings({"unchecked", "unused"})
    default T noColor() {
        getStyles().remove(COLOR);
        return (T)this;
    }

    default T color(int color) {
        return styleWithArgument(COLOR, color);
    }

    @SuppressWarnings("unused")
    default T bgColor(int red, int green, int blue) {
        return bgColor(rgb2int(red, green, blue));
    }

    default T bgColor(int color) {
        return styleWithArgument(COLOR, color);
    }

    default T colorIndex(int color) {
        return styleWithArgument(COLOR_INDEX, color);
    }

    @SuppressWarnings("unused")
    default T bgColorIndex(int color) {
        return styleWithArgument(BACKGROUND_COLOR_INDEX, color);
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    default boolean has(Style... styles) {
        return has(Arrays.asList(styles));
    }

    default boolean has(Collection<Style> styles) {
        return getStyles().containsAll(styles);
    }

    @SuppressWarnings("unused")
    default boolean has(EnumSet<Style> styles) {
        return getStyles().containsAll(styles);
    }

    @SuppressWarnings("unused")
    default boolean is(Style... styles) {
        return is(Arrays.asList(styles));
    }

    default boolean is(Collection<Style> styles) {
        return getStyles().containsAll(styles);
    }

    @SuppressWarnings("unused")
    default boolean is(EnumSet<Style> styles) {
        return getStyles().containsAll(styles);
    }

    @SuppressWarnings("unused")
    default T highIntensity() {
        return and(HIGH_INTENSITY);
    }

    default T bold() {
        return and(BOLD);
    }

    @SuppressWarnings("unused")
    default T lowIntensity() {
        return and(LOW_INTENSITY);
    }

    @SuppressWarnings("unused")
    default T light() {
        return and(LIGHT);
    }

    @SuppressWarnings("unused")
    default T italic() {
        return and(ITALIC);
    }

    @SuppressWarnings("unused")
    default T underline() {
        return and(UNDERLINE);
    }

    @SuppressWarnings("unused")
    default T blink() {
        return and(BLINK);
    }

    @SuppressWarnings("unused")
    default T rapidBlink() {
        return and(RAPID_BLINK);
    }

    @SuppressWarnings("unused")
    default T invert() {
        return and(REVERSE);
    }

    @SuppressWarnings("unused")
    default T reverse() {
        return and(REVERSE);
    }

    @SuppressWarnings("unused")
    default T invisibleText() {
        return and(INVISIBLE_TEXT);
    }

    @SuppressWarnings("unused")
    default T strike() {
        return and(STRIKE);
    }

    @SuppressWarnings("unused")
    default T doubleUnderline() {
        return and(DOUBLE_UNDERLINE);
    }

    @SuppressWarnings("unused")
    default T normalIntensity() {
        return and(NORMAL_INTENSITY);
    }

    @SuppressWarnings("unused")
    default T black() {
        return and(BLACK);
    }

    @SuppressWarnings("unused")
    default T red() {
        return and(RED);
    }

    @SuppressWarnings("unused")
    default T green() {
        return and(GREEN);
    }

    @SuppressWarnings("unused")
    default T yellow() {
        return and(YELLOW);
    }

    @SuppressWarnings("unused")
    default T blue() {
        return and(BLUE);
    }

    @SuppressWarnings("unused")
    default T magenta() {
        return and(MAGENTA);
    }

    @SuppressWarnings("unused")
    default T cyan() {
        return and(CYAN);
    }

    @SuppressWarnings("unused")
    default T white() {
        return and(WHITE);
    }

    @SuppressWarnings("unused")
    default T bgBlack() {
        return and(BACKGROUND_BLACK);
    }

    @SuppressWarnings("unused")
    default T bgRed() {
        return and(BACKGROUND_RED);
    }

    @SuppressWarnings("unused")
    default T bgGreen() {
        return and(BACKGROUND_GREEN);
    }

    @SuppressWarnings("unused")
    default T bgYellow() {
        return and(BACKGROUND_YELLOW);
    }

    @SuppressWarnings("unused")
    default T bgBlue() {
        return and(BACKGROUND_BLUE);
    }

    @SuppressWarnings("unused")
    default T bgMagenta() {
        return and(BACKGROUND_MAGENTA);
    }

    @SuppressWarnings("unused")
    default T bgCyan() {
        return and(BACKGROUND_CYAN);
    }

    @SuppressWarnings("unused")
    default T bgWhite() {
        return and(BACKGROUND_WHITE);
    }

    @SuppressWarnings("unused")
    default T brightBlack() {
        return and(BRIGHT_BLACK);
    }

    @SuppressWarnings("unused")
    default T brightRed() {
        return and(BRIGHT_RED);
    }

    @SuppressWarnings("unused")
    default T brightGreen() {
        return and(BRIGHT_GREEN);
    }

    @SuppressWarnings("unused")
    default T brightYellow() {
        return and(BRIGHT_YELLOW);
    }

    @SuppressWarnings("unused")
    default T brightBlue() {
        return and(BRIGHT_BLUE);
    }

    @SuppressWarnings("unused")
    default T brightMagenta() {
        return and(BRIGHT_MAGENTA);
    }

    @SuppressWarnings("unused")
    default T brightCyan() {
        return and(BRIGHT_CYAN);
    }

    @SuppressWarnings("unused")
    default T brightWhite() {
        return and(BRIGHT_WHITE);
    }

    @SuppressWarnings("unused")
    default T bgBrightBlack() {
        return and(BACKGROUND_BRIGHT_BLACK);
    }

    @SuppressWarnings("unused")
    default T bgBrightRed() {
        return and(BACKGROUND_BRIGHT_RED);
    }

    @SuppressWarnings("unused")
    default T bgBrightGreen() {
        return and(BACKGROUND_BRIGHT_GREEN);
    }

    @SuppressWarnings("unused")
    default T bgBrightYellow() {
        return and(BACKGROUND_BRIGHT_YELLOW);
    }

    @SuppressWarnings("unused")
    default T bgBrightBlue() {
        return and(BACKGROUND_BRIGHT_BLUE);
    }

    @SuppressWarnings("unused")
    default T bgBrightMagenta() {
        return and(BACKGROUND_BRIGHT_MAGENTA);
    }

    @SuppressWarnings("unused")
    default T bgBrightCyan() {
        return and(BACKGROUND_BRIGHT_CYAN);
    }

    @SuppressWarnings("unused")
    default T bgBrightWhite() {
        return and(BACKGROUND_BRIGHT_WHITE);
    }

    @SuppressWarnings("unused")
    default String wrap(String input) {
        String sequence = getSequence();
        String reset = Style.RESET.toString();
        return getSequence() + input + reset;
    }

    enum Style {
        /**
         * RESET
         */
        RESET(0),

        /**
         * HIGH_INTENSITY
         */
        HIGH_INTENSITY(1),

        /**
         * BOLD
         */
        BOLD(1),

        /**
         * LOW_INTENSITY
         */
        LOW_INTENSITY(2),

        /**
         * LIGHT
         */
        LIGHT(2),

        /**
         * ITALIC
         */
        ITALIC(3),

        /**
         * UNDERLINE
         */
        UNDERLINE(4),

        /**
         * BLINK
         */
        BLINK(5),

        /**
         * RAPID_BLINK
         */
        RAPID_BLINK(6),

        /**
         * REVERSE
         */
        REVERSE(7),

        /**
         * INVISIBLE_TEXT
         */
        INVISIBLE_TEXT(8),

        /**
         * STRIKE
         */
        STRIKE(9),

        /**
         * DOUBLE_UNDERLINE
         */
        DOUBLE_UNDERLINE(21),

        /**
         * NORMAL_INTENSITY
         */
        NORMAL_INTENSITY(22),

        /**
         * BLACK
         */
        BLACK(30),

        /**
         * RED
         */
        RED(31),

        /**
         * GREEN
         */
        GREEN(32),

        /**
         * YELLOW
         */
        YELLOW(33),

        /**
         * BLUE
         */
        BLUE(34),

        /**
         * MAGENTA
         */
        MAGENTA(35),

        /**
         * CYAN
         */
        CYAN(36),

        /**
         * WHITE
         */
        WHITE(37),

        /**
         * COLOR_INDEX
         */
        COLOR_INDEX(38, HANDLER_INDEX),

        /**
         * COLOR
         */
        COLOR(38, HANDLER_COLOR),

        /**
         * BACKGROUND_BLACK
         */
        BACKGROUND_BLACK(40),

        /**
         * BACKGROUND_RED
         */
        BACKGROUND_RED(41),

        /**
         * BACKGROUND_GREEN
         */
        BACKGROUND_GREEN(42),

        /**
         * BACKGROUND_YELLOW
         */
        BACKGROUND_YELLOW(43),

        /**
         * BACKGROUND_BLUE
         */
        BACKGROUND_BLUE(44),

        /**
         * BACKGROUND_MAGENTA
         */
        BACKGROUND_MAGENTA(45),

        /**
         * BACKGROUND_CYAN
         */
        BACKGROUND_CYAN(46),

        /**
         * BACKGROUND_WHITE
         */
        BACKGROUND_WHITE(47),

        /**
         * BACKGROUND_COLOR_INDEX
         */
        BACKGROUND_COLOR_INDEX(48, HANDLER_INDEX),

        /**
         * BACKGROUND_COLOR
         */
        BACKGROUND_COLOR(48, HANDLER_COLOR),

        /**
         * BRIGHT_BLACK
         */
        BRIGHT_BLACK(90),

        /**
         * BRIGHT_RED
         */
        BRIGHT_RED(91),

        /**
         * BRIGHT_GREEN
         */
        BRIGHT_GREEN(92),

        /**
         * BRIGHT_YELLOW
         */
        BRIGHT_YELLOW(93),

        /**
         * BRIGHT_BLUE
         */
        BRIGHT_BLUE(94),

        /**
         * BRIGHT_MAGENTA
         */
        BRIGHT_MAGENTA(95),

        /**
         * BRIGHT_CYAN
         */
        BRIGHT_CYAN(96),

        /**
         * BRIGHT_WHITE
         */
        BRIGHT_WHITE(97),

        /**
         * BACKGROUND_BRIGHT_BLACK
         */
        BACKGROUND_BRIGHT_BLACK(100),

        /**
         * BACKGROUND_BRIGHT_RED
         */
        BACKGROUND_BRIGHT_RED(101),

        /**
         * BACKGROUND_BRIGHT_GREEN
         */
        BACKGROUND_BRIGHT_GREEN(102),

        /**
         * BACKGROUND_BRIGHT_YELLOW
         */
        BACKGROUND_BRIGHT_YELLOW(103),

        /**
         * BACKGROUND_BRIGHT_BLUE
         */
        BACKGROUND_BRIGHT_BLUE(104),

        /**
         * BACKGROUND_BRIGHT_MAGENTA
         */
        BACKGROUND_BRIGHT_MAGENTA(105),

        /**
         * BACKGROUND_BRIGHT_CYAN
         */
        BACKGROUND_BRIGHT_CYAN(106),

        /**
         * BACKGROUND_BRIGHT_WHITE
         */
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
            return CSI_START + getSequence() + CSI_END;
        }
    }
}
