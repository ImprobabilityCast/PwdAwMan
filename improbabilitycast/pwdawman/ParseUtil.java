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
            if (line.charAt(end) == '"'
                    && end - 1 >= 0 && line.charAt(end - 1) != '\\') {
                quotes++;
            }
            end++;
        }

        return line.substring(start, end);
    }

    public static List<String> splitWithQuotes(String line, char sep) {
        List<String> list  = new ArrayList<>();
        int pos = 0;
        while (pos <= line.length()) {
            String token = getNextToken(line, sep, pos);
            // +1 to skip sep char
            pos += token.length() + 1;
            list.add(token);
        }
        return list;
    }

    private static String csvUnEscape(String s) {
        StringBuilder sb = new StringBuilder(s);
        boolean slash = false;

        if (sb.length() > 0 && sb.charAt(0) == '"'
                && sb.charAt(sb.length() - 1) == '"') {
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(0);
        }

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (slash && c == '"') {
                i--;
                sb.deleteCharAt(i);
            }
            slash = (c == '\\');
        }
        return sb.toString();
    }

    private static void csvUnEscapeRow(List<String> row) {
        for (int i = 0; i < row.size(); i++) {
            row.set(i, csvUnEscape(row.get(i)));
        }
    }

    public static void parseCSV(List<List<String>> table, String csv) {
        table.clear();
        for (String s : csv.split("\n|\r\n")) {
            List<String> row = splitWithQuotes(s, ',');
            csvUnEscapeRow(row);
            row.add(0, String.valueOf(table.size()));
            DisplayUtil.updatePadding(row);
            row.remove(0);
            table.add(row);
        }
    }

    private static String csvEscape(String s) {
        // add 4 so sb will probably not have to realloc
        StringBuilder sb = new StringBuilder(s.length() + 4);
        sb.append('\"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append('\"');

        if (s.contains(",")) {
            return sb.toString();
        } else {
            return sb.substring(1, sb.length() - 1);
        }
    }

    // every list in the list must have size > 0
    public static String toCSV(List<List<String>> table) {
        // assuming about 100 chars per line
        StringBuilder sb = new StringBuilder(table.size() * 100);

        for (int i = 0; i < table.size(); i++) {
            List<String> line = table.get(i);
            String escaped = csvEscape(line.get(0));
            sb.append(escaped);
            for (int j = 1; j < line.size(); j++) {
                sb.append(',');
                escaped = csvEscape(line.get(j));
                sb.append(escaped);
            }
            sb.append('\n');
        }

        return sb.toString();
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