package com.witcraft.ansi;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Ansi implements AnsiSequence<Ansi> {


    private final EnumSet<Style> styles;
    private final Map<Style, Object> argumentMap;
    private boolean isGrouped;

    /**
     * Creates a new Ansi object with grouped sequences.
     */
    public Ansi() {
        this(true);
    }

    /**
     * Creates a new Ansi object, with grouping indicated by {@code isGroup}.
     *
     * @param isGrouped indicates whether the created Ansi instance will group all of its sequences together
     */
    public Ansi(boolean isGrouped) {
        styles = EnumSet.noneOf(Style.class);
        argumentMap = new HashMap<>();
        this.isGrouped = isGrouped;
    }

    @Override
    public EnumSet<Style> getStyles() { return styles; }

    @Override
    public Map<Style, Object> getArgumentMap() { return argumentMap; }

    @Override
    public boolean isGrouped() { return isGrouped; }

    @Override
    public Ansi setGrouped(boolean group) { isGrouped = group; return this; }

    @Override
    public String toString() { return getSequence(); }
}
