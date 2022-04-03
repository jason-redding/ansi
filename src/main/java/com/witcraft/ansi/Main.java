package com.witcraft.ansi;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        Ansi styler = new Ansi(true);

        final BigDecimal[] length = {new BigDecimal(0)};
        //PrintStream out = System.out;
        PrintWriter out = new PrintWriter(new FilterWriter(new PrintWriter(System.out)) {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                super.write(cbuf, off, len);
                length[0] = length[0].add(BigDecimal.valueOf(len));
            }

            @Override
            public void write(String str, int off, int len) throws IOException {
                super.write(str, off, len);
                length[0] = length[0].add(BigDecimal.valueOf(len));
            }

            @Override
            public void write(int c) throws IOException {
                super.write(c);
                length[0] = length[0].add(BigDecimal.ONE);
            }
        });

        for (int i = 0; i < 16; i++) {
            int colorIndex;
            for (int j = 0; j < 16; j++) {
                colorIndex = (i * 16 + j);
                String text = String.valueOf(colorIndex);
                for (int p = text.length(); p <= 4; p++) {
                    text = " " + text;
                }
                styler.reset().colorIndex(colorIndex);
                //out.print(styler.wrap(text));
                out.print(styler);
                out.print(text);

            }
            out.println();
        }

        styler.reset().bgBrightRed().colorIndex(255);
        out.println();
        out.println("[" + styler.wrap("FAILED") + "]");
        styler.reset().bgGreen().colorIndex(255);
        out.println("[" + styler.wrap(" PASS ") + "]");
        styler.reset().bgBlack().colorIndex(255);
        out.println("[" + styler.wrap("  OK  ") + "]");
        out.flush();

        System.out.println("Bytes transferred: " + length[0].longValue());
    }
}
