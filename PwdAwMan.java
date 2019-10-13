import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jasypt.util.text.TextEncryptor;
import org.jasypt.util.text.AES256TextEncryptor;

public class PwdAwMan {

    private static final int PLACE = 0;
    private static final int TYPE = 1;
    private static final int URL = 2;
    private static final int USERNAME = 3;
    private static final int PASSWORD = 4;
    private static final int NOTE = 5;
    private static final int WIDTH = 80;

    private static String[] padArr = new String[] {
        // username is last so it does not need padding
        // id     place   type     url     
        "   ", "      ", "     ", "    ", ""
    };

    private static List<List<String>> table
            = new LinkedList<List<String>>();

    private PwdAwMan() {
    }

    private static String load(String filename) {
        String ret = null;
        FileReader fr = null;
        try {
            File f = new File(filename);
            fr = new FileReader(f);
            char[] buf = new char[(int) f.length()];
            fr.read(buf);
            ret = new String(buf);
            fr.close();
        } catch (Exception e1) {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (Exception e2) {

            }
        }
        return ret;
    }

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

    private static List<String> splitWithQuotes(String line, char sep) {
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

    private static String makePaddingBigger(String start, int newLength) {
        StringBuilder sb = new StringBuilder(newLength);
        sb.append(start);
        char[] newSpaces = new char[newLength - start.length()];
        Arrays.fill(newSpaces, ' ');
        sb.append(newSpaces);
        return sb.toString();
    }

    private static void updatePadding(List<String> list) {
        for (int i = 1; i < padArr.length; i++) {
            int itemLen = list.get(i).length();
            if (itemLen > padArr[i].length()) {
                padArr[i] = makePaddingBigger(padArr[i], itemLen + 1);
            }
        }
    }

    private static void parseCSV(String csv) {
        for (String s : csv.split("\n|\r\n")) {
            List<String> row = splitWithQuotes(s, ',');
            row.add(0, String.valueOf(table.size()));
            updatePadding(row);
            row.remove(0);
            PwdAwMan.table.add(row);
        }
    }

    private static void setPwd(AES256TextEncryptor enc, String filename) {
        Console console = System.console();
        String fmt = "Password for ' " + filename + "':";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
    }

    private static void printHeaders() {
        List<String> list =
            List.of("ID", "PLACE", "TYPE", "URL", "USERNAME");
        String s = fmtRow(list);
        System.out.println(s);
    }

    private static void printTable() {
        printHeaders();
        for (int i = 0; i < table.size(); i++) {
            printRow(i);
        }
    }

    private static String fmtRow(List<String> row) {
        StringBuilder sb = new StringBuilder(WIDTH);
        for (int i = 0; i < row.size(); i++) {
            String item = row.get(i);
            sb.append(item);
            sb.append(padArr[i].substring(item.length()));
        }
        return sb.toString();
    }

    private static void printSyntax() {
        System.out.println("Command syntax:\n"
            + "list\n"
            + "replace <id> COL_NAME <replacement>\n"
            + "show <id> COL_NAME\n"
            + "copy <id> COL_NAME\n"
            + "add <place> <type> <url> <username> <password> <note>\n"
            + "Where COL_NAME is one of: <all|place|type|url|username|password|note>\n"
            + "Omiting arguments from add will keep those fields blank.\n"
            + "If the 'all' option is selected, then <replacement> has the same\n"
            + "syntax as add."
        );
    }

    private static int parseColName(String col) {
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

    private static void printRow(List<List<String>> table, int id) {
        List<String> list = table.get(id);
        list.add(0, String.valueOf(id));
        String row = fmtRow(list.subList(0, list.size() - 2));
        System.out.println(row);
        list.remove(0);
    }

    private static void show(List<String> cmd) {
        int id = Integer.parseInt(cmd.get(1));
        int idx = (2 < cmd.size()) ? parseColName(cmd.get(2)) : -1;
        if (idx == -1) {
            printRow(id);
        } else {
            System.out.println(table.get(id).get(idx));
        }
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            try {
                List<String> cmd = splitWithQuotes(sc.nextLine(), ' ');
                if (cmd.get(0).equals("list")) {
                    printTable();
                } else if (cmd.get(0).equals("replace")) {
                    
                } else if (cmd.get(0).equals("show")) {
                    show(cmd);
                } else if (cmd.get(0).equals("copy")) {
                    
                } else if (cmd.get(0).equals("add")) {
                    //id place type url username pwd note
                } else {
                    printSyntax();
                }
            } catch (Exception e) {
                // TODO: make this bit more user friendly
                System.err.println(e.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("halp.");
    }

    public static void main(final String[] args) {

        AES256TextEncryptor enc = new AES256TextEncryptor();
        String data;
        String filename;

        if (args.length == 0) {
            printHelp();
            return;
        } else if (args.length == 1) {
            filename = args[0];
            data = load(filename);
            setPwd(enc, filename);
            data = enc.decrypt(data);
        } else if (args.length == 2 && args[0].equals("-p")) {
            filename = args[1];
            data = load(filename);
        } else {
            printHelp();
            return;
        }

        parseCSV(data);
        prompt();
    }
}
