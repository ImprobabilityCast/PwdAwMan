package improbabilitycast.pwdawman;

import java.util.Arrays;
import java.util.List;

public class DisplayUtil {

    private static final int WIDTH = 80;

    private static String[] padArr = new String[] {
        // username is last so it does not need padding
        // id     place   type     url     
        "   ", "      ", "     ", "    ", ""
    };

    private DisplayUtil() {}

    private static String fmtRow(List<String> row) {
        StringBuilder sb = new StringBuilder(WIDTH);
        for (int i = 0; i < row.size(); i++) {
            String item = row.get(i);
            sb.append(item);
            sb.append(padArr[i].substring(item.length()));
        }
        return sb.toString();
    }

    public static void printHeaders() {
        List<String> list =
            List.of("ID", "PLACE", "TYPE", "URL", "USERNAME");
        String s = fmtRow(list);
        System.out.println(s);
    }

    public static void printRow(List<List<String>> table, int id) {
        List<String> list = table.get(id);
        list.add(0, String.valueOf(id));
        String row = fmtRow(list.subList(0, list.size() - 2));
        System.out.println(row);
        list.remove(0);
    }

    public static void printTable(List<List<String>> table) {
        printHeaders();
        for (int i = 0; i < table.size(); i++) {
            printRow(table, i);
        }
    }

    private static String makePaddingBigger(String start, int newLength) {
        StringBuilder sb = new StringBuilder(newLength);
        sb.append(start);
        char[] newSpaces = new char[newLength - start.length()];
        Arrays.fill(newSpaces, ' ');
        sb.append(newSpaces);
        return sb.toString();
    }

    public static void updatePadding(String item, int col) {
        int itemLen = item.length();
        if (itemLen > padArr[col].length()) {
            padArr[col] = makePaddingBigger(padArr[col], itemLen + 1);
        }
    }

    public static void updatePadding(List<String> list) {
        for (int i = 0; i < padArr.length; i++) {
            updatePadding(list.get(i), i);
        }
    }
}
