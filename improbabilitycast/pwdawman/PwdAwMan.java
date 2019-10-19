package improbabilitycast.pwdawman;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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

    private static final int VERSION_MAJOR = 0;
    private static final int VERSION_MINOR = 1;
    
    private static boolean isModified = false;
    private static boolean isUnencrypted = false;
    private static List<List<String>> dataTable = new LinkedList<List<String>>();

    private PwdAwMan() {}

    /*
    * File I/O.
    */

    // if loading the file fails, this returns null
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
        FileWriter fw = null;
        try {
            // save in tmp file first to ensure something gets saved
            File tmpFile = new File(filename + ".tmp");
            File outFile = new File(filename);
            fw = new FileWriter(tmpFile);
            fw.write(data);
            fw.close();
            if (outFile.exists()) {
                outFile.delete();
            }
            ret = !tmpFile.renameTo(outFile);
        } catch (Exception e1) {
            ret = true;
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (Exception e2) {
                // Yup. Do nothing.
            }
        }
        return ret;
    }

    // return false if save unsuccessful
    private static boolean askSave(Scanner in, String filename,
            String data, AES256TextEncryptor enc) {
        boolean ret = false;
        System.out.print("Do you wish to save your changes? [Y/n] ");
        String line = in.nextLine();

        if (line.equals("y") || line.equals("Y")) {
            if (isUnencrypted) {
                System.out.print("Do you wish to encrypt the save file? [Y/n] ");
                line = in.nextLine();
                if (line.equals("y") || line.equals("Y")) {
                    System.out.print("Enter new filename: ");
                    filename = in.nextLine();
                    setPwdAskTwice(enc);
                    data = enc.encrypt(data);
                }
            } else {
                data = enc.encrypt(data);
            }
            ret = save(filename, data);
        }
        return ret;
        // TODO: if save unsuccessful, but user said to encrypt,
        // and the file gets saved in tryAgainSave, it will not
        // be encrypted
    }

    private static void tryAgainSave(Scanner in, String data) {
        String filename;
        do {
            System.err.println("Couldn't save the file.");
            System.out.print("New file name (enter nothing to not save): ");
            filename = in.nextLine();
        } while (filename.length() > 0 && save(filename, data));
    }

    /*
    * Password setting methods.
    */

    private static void setPwdAskOnce(AES256TextEncryptor enc) {
        Console console = System.console();
        String fmt = "Enter password: ";
        String pwd = new String(console.readPassword(fmt));
        enc.setPassword(pwd);
    }

    private static void setPwdAskTwice(AES256TextEncryptor enc) {
        Console console = System.console();
        
        while (true) {
            String fmt = "Enter password: ";
            String pwd = new String(console.readPassword(fmt));
            String fmt2 = "Re-enter password: ";
            String pwd2 = new String(console.readPassword(fmt2));

            if (pwd.equals(pwd2)) {
                enc.setPassword(pwd);
                return;
            } else {
                System.err.println("Passwords do not match.");
            }
        }
        // TODO: allow user to change their mind, e.g. not encrypt
        // after saying 'yes, encrypt it'
    }

    /*
    * Command handlers.
    */

    private static String add(List<String> cmd) {
        // cmd should have at most DisplayUtil.PAD_COLS elements
        // since there are DisplayUtil.PAD_COLS - 1 editable columns,
        // and one non-editable one. cmd starts with "add", which acts
        // as a placeholder for the id value.
        cmd.set(0, String.valueOf(dataTable.size()));
        if (cmd.size() <= DisplayUtil.PAD_COLS) {
            // if not enough args, pad it with empty values
            while (cmd.size() < DisplayUtil.PAD_COLS) {
                cmd.add("");
            }
            DisplayUtil.updatePadding(cmd);
            cmd.remove(0);
            dataTable.add(cmd);
            isModified = true;
            return "";
        } else {
            return "add: too many arguments";
        }
    }

    private static String copy(List<String> cmd, int id, int idx) {
        String data = dataTable.get(id).get(idx);
        StringSelection sData = new StringSelection(data);
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(sData, sData);
        return "";
    }

    private static String export() {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter new filename: ");
        String filename = in.nextLine();

        String csv = ParseUtil.toCSV(dataTable);
        if (save(filename, csv)) {
            tryAgainSave(in, csv);
        }

        return "";
    }

    private static String promptHelp() {
        return "Command syntax:\n"
            + "show <id> COL_NAME\n"
            + "replace <id> COL_NAME <replacement>\n"
            + "copy <id> COL_NAME\n"
            + "export - save as a plain text csv file\n"
            + "add <place> <type> <url> <username> <password> <note>\n"
            + "Where COL_NAME is one of: <all|place|type|url|username|password|note>\n"
            + "Omiting arguments from add will keep those fields blank.\n"
            + "If the 'all' option is selected, then <replacement> has the same\n"
            + "syntax as add.\n"
            + "If no arguments are supplied to show, it will show all";
    }

    // this method exists so that the "invaild command" msg doesn't print
    private static String dummyQuit() {
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

    private static String show(List<String> cmd, int id, int idx) {
        if (id < 0 || id > dataTable.size()) {
            DisplayUtil.printTable(dataTable);
        } else if (idx < 0) {
            DisplayUtil.printRow(dataTable, id);
        } else {
            System.out.println(dataTable.get(id).get(idx));
        }
        return "";
    }

    private static String processCmd(List<String> cmd, Map<String, Action> cmdTable) {
        String msg;
        String key = cmd.get(0);

        int id = (1 < cmd.size()) ? ParseUtil.parsePositiveInt(cmd.get(1)) : -1;
        int idx = (2 < cmd.size()) ? ParseUtil.parseColName(cmd.get(2)) : -1;

        Action fn = cmdTable.get(key);
        if (fn == null) {
            msg = "unrecognized command: " + key + "\n"
                + "try 'help' to see a list of available commands.";
        } else {
            msg = fn.process(cmd, id, idx);
        }
        return msg;
    }

    private static void prompt(Scanner in, Map<String, Action> cmdTable) {
        String key = "";
        do {
            System.out.print("> ");
            String errMsg = "";
            try {
                List<String> cmd = ParseUtil.splitWithQuotes(in.nextLine(), ' ');
                key = cmd.get(0);
                errMsg = processCmd(cmd, cmdTable);
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

    /*
     * Helper methods for main.
     */

    private static void initCmdTable(Map<String, Action> cmdTable) {
        cmdTable.put("add", (a, b, c) -> add(a));
        cmdTable.put("copy", (a, b, c) -> copy(a, b, c));
        cmdTable.put("export", (a, b, c) -> export());
        cmdTable.put("help", (a, b, c) -> promptHelp());
        cmdTable.put("quit", (a, b, c) -> dummyQuit());
        cmdTable.put("replace", (a, b, c) -> replace(a, b, c));
        cmdTable.put("show", (a, b, c) -> show(a, b, c));
    }

    private static void printHelp() {
        System.out.println("PwdAwMan v" + VERSION_MAJOR + "."
            + VERSION_MINOR + " - A simple password manager.\n\n"
            + "USAGE: <this> [OPTION] FILE\n"
            + "OPTION:\n"
            + "\t-p\tread file as a plain text CSV file\n"
            + "FILE: a CSV file, possibly encrypted\n"
        );
    }

    public static void main(final String[] args) {
        Map<String, Action> cmdTable = new HashMap<>();
        AES256TextEncryptor enc = new AES256TextEncryptor();
        Scanner in = new Scanner(System.in);
        String data;
        String filename;

        if (args.length == 1) {
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
        initCmdTable(cmdTable);
        prompt(in, cmdTable);
        data = ParseUtil.toCSV(dataTable);
        
        if (isModified && askSave(in, filename, data, enc)) {
            // something went wrong
            tryAgainSave(in, data);
        }
    }
}
