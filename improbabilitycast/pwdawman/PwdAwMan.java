package improbabilitycast.pwdawman;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.jasypt.util.text.TextEncryptor;
import org.jasypt.util.text.AES256TextEncryptor;

public class PwdAwMan {

    @FunctionalInterface
    interface Action {
        public String process(List<String> cmd, int id, int idx);
    }

    private static boolean isModified = false;
    private static boolean isUnencrypted = false;
    private static List<List<String>> dataTable = new LinkedList<List<String>>();
    private static Map<String, Action> cmdTable = new HashMap<>();

    static {
        cmdTable.put("show", (a, b, c) -> show(a, b, c));
        cmdTable.put("replace", (a, b, c) -> replace(a, b, c));
        //cmdTable.add("copy", copy);
        cmdTable.put("add", (a, b, c) -> add(a, b, c));
        cmdTable.put("help", (a, b, c) -> getHelp(a, b, c));
        cmdTable.put("quit", (a, b, c) -> dummyQuit(a, b, c));
    }

    private PwdAwMan() {}

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
            ret = null;
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (Exception e2) {
                // Yup. Do nothing.
            }
        }
        return ret;
    }

    // returns true on error
    private static boolean save(String filename, String data) {
        boolean ret = false;
        FileWriter fr = null;
        try {
            File f = new File(filename);
            fw = new FileWriter(f);
            fr.write(data);
            fr.close();
        } catch (Exception e1) {
            ret = true;
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (Exception e2) {
                // Yup. Do nothing.
            }
        }
        return ret;
    }

    private static void setPwdAskOnce(AES256TextEncryptor enc) {
        Console console = System.console();
        String fmt = "Password: ";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
    }

    private static void setPwdAskTwice(AES256TextEncryptor enc) {
        Console console = System.console();
        
        while (true) {
            String fmt = "Password: ";
            String pwd = new String(console.readPassword(fmt));
            String fmt2 = "Re-enter password: ";
            String pwd2 = new String(console.readPassword(fmt));

            if (pwd.equals(pwd2)) {
                enc.setPassword(pwd);
                return;
            } else {
                System.err.println("Passwords do not match.");
            }
        }
    }

    // all parameters are ignored
    private static String getHelp(List<String> cmd, int id, int idx) {
        return "Command syntax:\n"
            + "replace <id> COL_NAME <replacement>\n"
            + "show <id> COL_NAME\n"
            + "copy <id> COL_NAME\n"
            + "add <place> <type> <url> <username> <password> <note>\n"
            + "Where COL_NAME is one of: <all|place|type|url|username|password|note>\n"
            + "Omiting arguments from add will keep those fields blank.\n"
            + "If the 'all' option is selected, then <replacement> has the same\n"
            + "syntax as add.\n"
            + "If no arguments are supplied to show, it will show all";
    }

    // once again, all parameters are ignored
    private static String dummyQuit(List<String> cmd, int id, int idx) {
        return "";
    }

    // return false if save unsuccessful
    private static boolean askSave(String filename, AES256TextEncryptor enc) {
        boolean ret = false;
        System.out.print("Do you wish to save your changes? [Y/n] ");
        Scanner in = Scanner(System.in);
        String line = in.nextLine();

        if (line.equals("y") || line.equals("Y")) {
            String data = ParseUtil.toCSV(dataTable);
            if (isUnencrypted) {
                System.out.print("Do you wish to encrypt the save file? [Y/n] ");
                line = in.nextLine();
                if (line.equals("y") || line.equals("Y")) {
                    setPwdAskTwice(enc);
                    data = enc.encrypt(data);
                }
            } else {
                data = enc.encrypt(data);
            }
            ret = save(filename, data);
        }
        return ret;
    }

    private static String show(List<String> cmd, int id, int idx) {
        if (id < 0) {
            DisplayUtil.printTable(dataTable);
        } else if (idx < 0) {
            DisplayUtil.printRow(dataTable, id);
        } else {
            System.out.println(dataTable.get(id).get(idx));
        }
        return "";
    }

    // case where id < 0 not checked
    private static String replace(List<String> cmd, int id, int idx) {
        // replace <id> COL_NAME <replacement>
        if (idx >= 0 && 3 < cmd.size()) {
            String replacement = cmd.get(3);
            // +1 to idx because disply has the ID column as 0
            // but dataTable has PLACE column as 0
            DisplayUtil.updatePadding(replacement, idx + 1);
            dataTable.get(id).set(idx, cmd.get(3));
            isModified = true;
            return "";
        } else {
            return "replace: invalid command syntax";
        }
    }

    // id & idx not used
    private static String add(List<String> cmd, int id, int idx) {
        // remove "add" from the start of the list
        cmd.remove(0);
        
        final int numCols = 6;
        if (cmd.size() <= numCols) {
            while (cmd.size() < numCols) {
                cmd.add("");
            }
            dataTable.add(cmd);
            isModified = true;
            return "";
        } else {
            return "add: too many arguments";
        }
    }

    private static void prompt() {
        Scanner sc = new Scanner(System.in);
        String key = "";
        do {
            System.out.print("> ");
            String errMsg = "";
            try {
                List<String> cmd = ParseUtil.splitWithQuotes(sc.nextLine(), ' ');
                key = cmd.get(0);
                int id = (1 < cmd.size()) ? ParseUtil.parsePositiveInt(cmd.get(1)) : -1;
                int idx = (2 < cmd.size()) ? ParseUtil.parseColName(cmd.get(2)) : -1;

                Action fn = cmdTable.get(key);
                if (fn == null) {
                    errMsg = "unrecognized command: " + key + "\n"
                        + "try 'help' to see a list of available commands.";
                } else {
                    errMsg = fn.process(cmd, id, idx);
                }
            } catch (Exception e) {
                errMsg = "Oops: " + e.getMessage()
                    + "\nProbably invalid command syntax. "
                    + "Try 'help' to see a list of available commands.";
            }
            if (errMsg.length() > 0) {
                System.err.println(errMsg);
            }
        } while (!key.equals("quit"));
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
            setPwdAskOnce(enc);
            data = enc.decrypt(data);
        } else if (args.length == 2 && args[0].equals("-p")) {
            isUnencrypted = true;
            filename = args[1];
            data = load(filename);
        } else {
            printHelp();
            return;
        }

        ParseUtil.parseCSV(dataTable, data);
        prompt();
        
        if (isModified && askSave(filename)) {
            // something went wrong
        }
    }
}
