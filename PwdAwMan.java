import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
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

    private static void parseCSV(String csv) {
        for (String s : csv.split("\n|\r\n")) {
            PwdAwMan.table.add(splitWithQuotes(s, ','));
        }
    }

    private static void setPwd(AES256TextEncryptor enc, String filename) {
        Console console = System.console();
        String fmt = "Password for ' " + filename + "':";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
    }

    private static void printHeaders() {
        System.out.println("ID\tPLACE\t\tTYPE\tURL\t\tUSERNAME");
    }

    private static String fmtRow(List<String> row, int rowNum) {
        StringBuilder sb = new StringBuilder(80);
        sb.append(rowNum);
        sb.append('\t');
        sb.append(row.get(PLACE));
        sb.append('\t');
        sb.append(row.get(TYPE));
        sb.append('\t');
        sb.append(row.get(URL));
        sb.append('\t');
        sb.append(row.get(USERNAME));
        return sb.toString();
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            try {
                List<String> cmd = splitWithQuotes(sc.nextLine(), ' ');
                System.out.println(":" + cmd.toString());
                if (cmd.get(0).equals("list")) {
                    printHeaders();
                    for (int i = 0; i < table.size(); i++) {
                        System.out.println(fmtRow(table.get(i), i));
                    }
                } else if (cmd.get(0).equals("replace")) {
                    
                } else if (cmd.get(0).equals("show")) {

                } else if (cmd.get(0).equals("copy")) {
                    
                } else if (cmd.get(0).equals("add")) {
                    //id place type url username pwd note
                    
                }
            } catch (Exception e) {
                System.out.println("Command syntax:\n"
                    + "list\n"
                    + "replace <id> TYPE <replacement>\n"
                    + "show <id> TYPE\n"
                    + "copy <id> TYPE\n"
                    + "add <place> <type> <url> <username> <password> <note>\n"
                    + "Where TYPE is one of: <all|place|type|url|username|password|note>\n"
                    + "Omiting arguments from add will keep those fields blank.\n"
                    + "If the 'all' option is selected, then <replacement> has the same\n"
                    + "syntax as add."
                );
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
