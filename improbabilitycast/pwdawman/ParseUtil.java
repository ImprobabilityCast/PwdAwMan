package improbabilitycast.pwdawman;

import java.util.ArrayList;
import java.util.List;

public class ParseUtil {
    private ParseUtil() {}

    private static String getNextToken(String line, char sep, int start) {
        int end = start;
        int quotes = 0;

         while (end < line.length()
                && (line.charAt(end) != sep || quotes % 2 != 0)) {
            if (line.charAt(end) == '"') {
                quotes++;
            }
            end++;
        }

        return line.substring(start, end);
    }

    public static List<String> splitWithQuotes(String line, char sep) {
        List<String> list  = new ArrayList<>();
        int pos = 0;
        while (pos < line.length()) {
            String token = getNextToken(line, sep, pos);
            // +1 to skip sep char
            pos += token.length() + 1;
            list.add(token);
        }
        return list;
    }

    public static void parseCSV(List<List<String>> table, String csv) {
        table.clear();
        for (String s : csv.split("\n|\r\n")) {
            List<String> row = splitWithQuotes(s, ',');
            row.add(0, String.valueOf(table.size()));
            DisplayUtil.updatePadding(row);
            row.remove(0);
            table.add(row);
        }
    }

    public static int parseColName(String col) {
        String[] cols = new String[] {
            "place", "type", "url", "username", "password", "note"
        };
        // array is small enough that a linear search is good enough
        int idx = 0;
        while (idx < cols.length && !cols[idx].equals(col)) {
            idx++;
        }
        return (idx < cols.length) ? idx : -1;
    }

    // returns < 0 if string s cannot be parsed as a positive int
    public static int parsePositiveInt(String s) {
        int i;
        try {
            i = Integer.parseInt(s);
        } catch (Exception e) {
            i = -1;
        }
        return i;
    }

}