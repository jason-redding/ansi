package com.witcraft.ansi;

import com.witcraft.ansi.AnsiSequence.Style;
import org.junit.jupiter.api.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class AnsiTest {
    private static final Pattern PATTERN_ANSI_CONTROL_SEQUENCE = Pattern.compile("(\\x1B\\[)([\\x30-\\x3F]*)([\\x20-\\x2F]*)([\\x40-\\x7E])");

    private Ansi ansi;

    @BeforeAll
    static void setup() {
    }

    @AfterAll
    static void done() {
    }

    @BeforeEach
    void init() {
        ansi = new Ansi(false);
    }

    @AfterEach
    void tearDown() {
    }

    void analyzeControlSequence(String sequence, Pattern expectedParameter) {
        analyzeControlSequence(sequence, expectedParameter, null);
    }

    void analyzeControlSequence(String sequence, Pattern expectedParameter, String expectedIntermediate) {
        analyzeControlSequence(sequence, expectedParameter, expectedIntermediate, "m");
    }

    void analyzeControlSequence(String sequence, Pattern expectedParameter, String expectedIntermediate, String expectedTerminator) {
        Matcher matcher = PATTERN_ANSI_CONTROL_SEQUENCE.matcher(sequence);
        assertTrue(matcher.matches(), "Control Sequence doesn't match expected format: " + PATTERN_ANSI_CONTROL_SEQUENCE.pattern());

        String csiStart = matcher.group(1);
        String csiParameter = matcher.group(2);
        String csiIntermediate = matcher.group(3);
        String csiTerminator = matcher.group(4);

        if (expectedIntermediate == null) {
            expectedIntermediate = "";
        }

        assertEquals(AnsiSequence.CSI_START, csiStart, "Control Sequence Header field doesn't match expected value");
        Matcher resetMatcher = expectedParameter.matcher(csiParameter);
        assertTrue(resetMatcher.matches(), "Control Sequence Parameter field doesn't match expected value");
        assertEquals(expectedIntermediate, csiIntermediate, "Control Sequence Intermediate field doesn't match expected value");
        assertEquals(expectedTerminator, csiTerminator, "Control Sequence Terminator field doesn't match expected value");
    }

    @TestFactory
    @DisplayName("Test encoding of all values for enum Style")
    public Stream<DynamicNode> testAllEnumItems() {
        return Stream.of(Style.values())
            .sorted((left, right) -> {
                if (left.expectsArgument() != right.expectsArgument()) {
                    if (left.expectsArgument()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                return String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name());
            })
            .map((style) -> {
                if (style.expectsArgument()) {
                    return dynamicContainer("Test encoding of Style." + style.name() + " with integer arguments [0-10000]", IntStream.rangeClosed(0, 10000).mapToObj((i) -> {
                    //return dynamicContainer("Test encoding of Style." + style.name() + " with integer arguments [0x0-0xFFFFFF]", IntStream.rangeClosed(0, 0xffffff).mapToObj((i) -> {
                        final String ansiSequence = String.valueOf(new Ansi(false).set(style, i));
                        final String styleSequence = style.getSequence(i);
                        return dynamicTest("Test encoding of Style." + style.name() + " with argument: " + i, () -> {
                            assertEquals(AnsiSequence.CSI_START + styleSequence + AnsiSequence.CSI_END, ansiSequence, "AnsiSequence improperly constructed!");
                        });
                    }));
                } else {
                    return dynamicTest("Test encoding of Style." + style.name(), () -> {
                        final String ansiSequence = String.valueOf(new Ansi(false).set(style));
                        final String styleSequence = style.getSequence();
                        assertEquals(AnsiSequence.CSI_START + styleSequence + AnsiSequence.CSI_END, ansiSequence);
                    });
                }
            });
    }

    @Test
    void testSequenceGrouping() {
        ansi.setGrouped(true);
        ansi.reset().bold().red();
        String value = String.valueOf(ansi);

        analyzeControlSequence(value, Pattern.compile("^1;31$"));
    }
}
