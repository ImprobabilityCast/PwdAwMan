package improbabilitycast.pwdawman;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.jasypt.util.text.TextEncryptor;
import org.jasypt.util.text.AES256TextEncryptor;

public class PwdAwMan {

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

    private static void setPwd(AES256TextEncryptor enc, String filename) {
        Console console = System.console();
        String fmt = "Password for ' " + filename + "':";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
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

    private static void show(List<String> cmd) {
        int id = Integer.parseInt(cmd.get(1));
        int idx = (2 < cmd.size()) ? ParseUtil.parseColName(cmd.get(2)) : -1;
        if (idx == -1) {
            DisplayUtil.printRow(table, id);
        } else {
            System.out.println(table.get(id).get(idx));
        }
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            try {
                List<String> cmd = ParseUtil.splitWithQuotes(sc.nextLine(), ' ');
                if (cmd.get(0).equals("list")) {
                    DisplayUtil.printTable(table);
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

        ParseUtil.parseCSV(table, data);
        prompt();
    }
}
