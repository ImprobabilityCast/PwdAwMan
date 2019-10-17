package improbabilitycast.pwdawman;

import java.util.Arrays;
import java.util.List;

public class DisplayUtil {

    private static final String[] padArr = new String[] {
        // padding after:
        // id  place     type     url     username   pwd note
        "   ", "      ", "     ", "    ", "       ", "", ""
    };

    private static final int WIDTH = 80;
    public static final int PAD_COLS = padArr.length;

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
        // -2 to skip pwd and note at end
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

    // assumes that col < PAD_COLS
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
