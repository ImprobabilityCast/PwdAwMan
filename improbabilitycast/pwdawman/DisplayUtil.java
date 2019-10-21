package improbabilitycast.pwdawman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayUtil {

    // should be 7 columns on normal use
    private static final List<String> padArr = new ArrayList<> (7);
    static {
        while (padArr.size() < 7) {
            padArr.add("");
        }
    }

    private static final int WIDTH = 80;

    private DisplayUtil() {}

    private static String fmtRow(List<String> row) {
        StringBuilder sb = new StringBuilder(WIDTH);
        for (int i = 0; i < row.size(); i++) {
            String item = row.get(i);
            sb.append(item);
            sb.append(padArr.get(i).substring(item.length()));
        }
        return sb.toString();
    }

    public static int getNumCols() {
        return padArr.size();
    }

    public static void printHeaders() {
        List<String> list =
            List.of("ID", "PLACE", "TYPE", "URL", "USERNAME");
        updatePaddingIDCol(list.get(0));
        updatePaddingRow(list.subList(1, list.size()));
        String s = fmtRow(list);
        System.out.println(s);
    }

    public static void printRow(List<List<String>> table, int id) {
        List<String> list = table.get(id);
        list.add(0, String.valueOf(id));
        // -2 to skip pwd and note at end
        int subtract = (list.size() >= 2) ? 2 : 0;
        String row = fmtRow(list.subList(0, list.size() - subtract));
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

    public static void updatePaddingIDCol(String id) {
        updatePaddingItem(id, 0);
    }

    // assumes that col < PAD_COLS
    public static void updatePaddingItem(String item, int col) {
        int itemLen = item.length();
        while (col >= padArr.size()) {
            padArr.add(" ");
        }
        if (itemLen >= padArr.get(col).length()) {
            padArr.set(col, makePaddingBigger(padArr.get(col), itemLen + 1));
        }
    }

    // assumes list does not contain ids
    public static void updatePaddingRow(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            updatePaddingItem(list.get(i), i + 1);
        }
    }
    
    public static void updatePaddingTable(List<List<String>> table) {
        updatePaddingIDCol(String.valueOf(table.size()));
        for (List<String> row : table) {
            DisplayUtil.updatePaddingRow(row);
        }
    }
}
