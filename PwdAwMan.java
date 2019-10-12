import java.io.Console;
import java.io.File;
import java.io.FileReader;
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
    private static final int NUM_COLS = 5;
    private static SortedMap<String, String[]> table
            = new TreeMap<String, String[]>();

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

    private static String getLastCell(String line, int subStrLen) {
        int i = subStrLen - 1;
        char c = line.charAt(i);
        int quotes = 0;

        while ((c != ',' || quotes % 2 != 0) && i - 1 > 0) {
            if (c == '"') {
                quotes++;
            }
            i--;
            c = line.charAt(i);
        }
        return line.substring(i + 1, subStrLen);
    }

    private static void parseCSV(String csv) {
        for (String s : csv.split("\n|\r\n")) {
            String note = getLastCell(s, s.length());
            // -1 to skip commas
            int end = s.length() - (note.length() + 1);
            String pwd = getLastCell(s, end);
            end -= (pwd.length() + 1);
            String[] val = {pwd, note};
            table.put(s.substring(0, end - 1), val);
        }
    }

    private static void setPwd(AES256TextEncryptor enc, String filename) {
        Console console = System.console();
        String fmt = "Password for ' " + filename + "':";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                String[] cmd = sc.nextLine().split(" ");
                if (cmd[0].equals("list")) {
                    for (String key : PwdAwMan.table.keySet()) {
                        System.out.println(key);
                    }
                } else if (cmd[0].equals("replace")) {
                    
                } else if (cmd[0].equals("show")) {

                } else if (cmd[0].equals("copy")) {
                    
                } else if (cmd[0].equals("add")) {
                    //place type url username pwd note
                    
                }
            } catch (Exception e) {

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
