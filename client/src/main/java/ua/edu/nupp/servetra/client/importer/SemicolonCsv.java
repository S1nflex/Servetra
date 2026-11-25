package ua.edu.nupp.servetra.client.importer;

import java.util.ArrayList;
import java.util.List;

// [ETAP 4] Semicolon-separated UTF-8 row parser with quoted-field support.
public final class SemicolonCsv {
    private SemicolonCsv() {
    }

    public static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                boolean escapedQuote = quoted
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"';
                if (escapedQuote) {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ';' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted field");
        }
        values.add(current.toString().trim());
        return values;
    }
}

