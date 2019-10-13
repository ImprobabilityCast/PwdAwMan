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

    private static boolean isModified = false;

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
            + "replace <id> COL_NAME <replacement>\n"
            + "show <id> COL_NAME\n"
            + "copy <id> COL_NAME\n"
            + "add <place> <type> <url> <username> <password> <note>\n"
            + "Where COL_NAME is one of: <all|place|type|url|username|password|note>\n"
            + "Omiting arguments from add will keep those fields blank.\n"
            + "If the 'all' option is selected, then <replacement> has the same\n"
            + "syntax as add.\n"
            + "If no arguments are supplied to show, it will show all"
        );
    }

    private static void show(List<String> cmd, int id, int idx) {
        if (id < 0) {
            DisplayUtil.printTable(table);
        } else if (idx < 0) {
            DisplayUtil.printRow(table, id);
        } else {
            System.out.println(table.get(id).get(idx));
        }
    }

    // case where id < 0 not checked
    private static String replace(List<String> cmd, int id, int idx) {
        // replace <id> COL_NAME <replacement>
        if (idx >= 0 && 3 < cmd.size()) {
            String replacement = cmd.get(3);
            // +1 to idx because disply has the ID column as 0
            // but table has PLACE column as 0
            DisplayUtil.updatePadding(replacement, idx + 1);
            table.get(id).set(idx, cmd.get(3));
            return "";
        } else {
            return "replace: invalid command syntax";
        }
    }

    private static String add(List<String> cmd) {
        // remove "add" from the start of the list
        cmd.remove(0);
        
        final int numCols = 6;
        if (cmd.size() <= numCols) {
            while (cmd.size() < numCols) {
                cmd.add("");
            }
            table.add(cmd);
            return "";
        } else {
            return "add: too many arguments";
        }
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String errMsg = "";
            try {
                List<String> cmd = ParseUtil.splitWithQuotes(sc.nextLine(), ' ');
                int id = (1 < cmd.size()) ? ParseUtil.parsePositiveInt(cmd.get(1)) : -1;
                int idx = (2 < cmd.size()) ? ParseUtil.parseColName(cmd.get(2)) : -1;

                if (cmd.get(0).equals("show")) {
                    show(cmd, id, idx);
                } else if (cmd.get(0).equals("replace")) {
                    errMsg = replace(cmd, id, idx);
                } else if (cmd.get(0).equals("copy")) {
                    
                } else if (cmd.get(0).equals("add")) {
                    errMsg = add(cmd);
                } else if (cmd.get(0).equals("exit")) {
                    break;
                } else if (cmd.get(0).equals("help")) {
                    printSyntax();
                } else {
                    errMsg = "unrecognized command: " + cmd.get(0) + "\n"
                        + "try 'help' to see a list of available commands.";
                }
            } catch (Exception e) {
                errMsg = e.getMessage();
            }
            if (errMsg.length() > 0) {
                System.err.println(errMsg);
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
