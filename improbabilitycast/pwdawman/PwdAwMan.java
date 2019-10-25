package improbabilitycast.pwdawman;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
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
    private static boolean isEncrypted = true;
    private static List<List<String>> dataTable = new LinkedList<List<String>>();
    private static AES256TextEncryptor enc = new AES256TextEncryptor();

    private PwdAwMan() {}

    private static class TableSort implements Comparator<List<String>> {
        @Override
        public int compare(List<String> o1, List<String> o2) {
            int result = 0;
            int i = 0;
            while (o1.size() > i && o2.size() > i && result == 0) {
                result = o1.get(i).compareToIgnoreCase(o2.get(i));
                i++;
            }
            return result;
        }
    }

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
        isModified = ret;
        return ret;
    }

    // return true if save unsuccessful
    private static boolean askSave(Scanner in, String filename, String data) {
        boolean ret = false;
        System.out.print("Do you wish to save your changes? [Y/n] ");
        String line = in.nextLine();

        if (line.equals("y") || line.equals("Y")) {
            if (!isEncrypted) {
                System.out.print("Do you wish to encrypt the save file? [Y/n] ");
                line = in.nextLine();
                if (line.equals("y") || line.equals("Y")) {
                    System.out.print("Enter new filename: ");
                    filename = in.nextLine();
                    setPwdAskTwice(enc);
                    isEncrypted = true;
                }
            }
            if (isEncrypted) {
                data = enc.encrypt(data);
            }
            ret = save(filename, data);
        }
        return ret;
    }

    private static void tryAgainSave(Scanner in, String data) {
        String filename;
        do {
            System.err.println("Couldn't save the file.");
            System.out.print("New file name (enter nothing to not save): ");
            filename = in.nextLine();
        } while (filename.length() > 0 && save(filename, data));
        isModified = (filename.length() == 0);
    }

    /*
    * Password setting methods.
    */

    private static void setPwdAskOnce(AES256TextEncryptor e) {
        Console console = System.console();
        String fmt = "Enter password: ";
        String pwd = new String(console.readPassword(fmt));
        e.setPassword(pwd);
    }

    private static void setPwdAskTwice(AES256TextEncryptor e) {
        Console console = System.console();
        int counter = 0;
        
        while (counter < 3) {
            String fmt = "Enter password: ";
            String pwd = new String(console.readPassword(fmt));
            String fmt2 = "Re-enter password: ";
            String pwd2 = new String(console.readPassword(fmt2));

            if (pwd.equals(pwd2)) {
                e.setPassword(pwd);
                return;
            } else {
                counter++;
                System.err.println("Passwords do not match.");
            }
        }
        String pwd = "Lasagna" + (int) Math.floor(Math.random() * 1000);
        e.setPassword(pwd);
        System.out.println("Fine. I'll do it for you. Your password is: " + pwd);
    }

    /*
    * Command handlers.
    */

    private static void binarySearchInsert(List<List<String>> table, List<String> line) {
        TableSort ts = new TableSort();
        int start = 0;
        int end = table.size();
        int idx;

        do {
            idx = (end + start) / 2;
            int comp = ts.compare(line, table.get(idx));
            if (comp > 0) {
                start = idx;
            } else if (comp < 0) {
                end = idx;
            } else {
                break;
            }
        } while (end - start > 1);
        table.add(end, line);
    }

    private static String add(List<String> cmd) {
        // cmd should have at most DisplayUtil.getNumCols() elements
        // since there are DisplayUtil.getNumCols() - 1 editable columns,
        // and one non-editable one. cmd starts with "add", which acts
        // as a placeholder for the id value.
        cmd.set(0, String.valueOf(dataTable.size()));
        if (cmd.size() <= DisplayUtil.getNumCols()) {
            // if not enough args, pad it with empty values
            while (cmd.size() < DisplayUtil.getNumCols()) {
                cmd.add("");
            }
            cmd.remove(0);
            binarySearchInsert(dataTable, cmd);
            DisplayUtil.updatePaddingIDCol(String.valueOf(dataTable.size()));
            DisplayUtil.updatePaddingRow(cmd);
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

    private static String export(List<String> cmd) {
        if (cmd.size() < 2) {
            return "export: invalid command syntax";
        }

        String csv = ParseUtil.toCSV(dataTable);
        String filename;

        if (cmd.size() > 2) {
            if (cmd.get(1).equals("-e")) {
                isEncrypted = true;
                setPwdAskTwice(enc);
            } else {
                isEncrypted = false;
                System.out.println("export: Warn: Saving file unencrypted.");
            }
            filename = cmd.get(2);
        } else {
            filename = cmd.get(1);
        }
        
        if (isEncrypted) {
            csv = enc.encrypt(csv);
        }
        if (save(filename, csv)) {
            tryAgainSave(new Scanner(System.in), csv);
        }

        return "";
    }

    private static String find(List<String> cmd) {
        if (cmd.size() != 2) {
            return "find: invalid command syntax";
        }

        String term = cmd.get(1);
        Deque<Integer> results = new ArrayDeque<>();
        for (int i = 0; i < dataTable.size(); i++) {
            if (dataTable.get(i).toString().contains(term)) {
                results.push(i);
            }
        }

        System.out.println("Search results for: " + term);
        DisplayUtil.printHeaders();
        while (results.size() > 0) {
            DisplayUtil.printRow(dataTable, results.pop());
        }
        return "";
    }

    private static String promptHelp() {
        return "Command syntax:\n"
            + "COL_NAME is one of: <place|type|url|username|password|note>\n\n"
            + "add <place> <type> <url> <username> <password> <note>\n"
            + "\tOmiting arguments from add will keep those fields blank.\n"
            + "copy <id> COL_NAME\n"
            + "export [-e] <filename>\n"
            + "\tSaves everything to a file. If the -e is supplied, then the file\n"
            + "\twill be encrypted.\n"
            + "find <term>\n"
            + "help - prints this\n"
            + "quit - quits, you may be asked if you want to save changes\n"
            + "remove <id>\n"
            + "replace <id> COL_NAME <replacement>\n"
            + "show <id> COL_NAME\n"
            + "\tIf none/incorrent arguments are supplied to show, it will show everything.\n";
    }

    // this method exists so that the "invaild command" msg doesn't print
    private static String dummyQuit() {
        return "";
    }

    private static String remove(List<String> cmd, int id) {
        if (id < 0 || id >= cmd.size()) {
            return "remove: invalid command sytax";
        } else {
            cmd.remove(id);
            return "";
        }
    }

    // case where id < 0 not checked
    private static String replace(List<String> cmd, int id, int idx) {
        // replace <id> COL_NAME <replacement>
        if (idx >= 0 && 3 < cmd.size()) {
            String replacement = cmd.get(3);
            // +1 to idx because disply has the ID column as 0
            // but dataTable has PLACE column as 0
            DisplayUtil.updatePaddingItem(replacement, idx + 1);
            dataTable.get(id).set(idx, cmd.get(3));
            isModified = true;
            return "";
        } else {
            return "replace: invalid command syntax";
        }
    }

    private static String show(List<String> cmd, int id, int idx) {
        if (id < 0 || id >= dataTable.size()) {
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
                    + "\nProbably invalid command syntax, or you loaded a csv file"
                    + " without enough columns (7).\n"
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
        cmdTable.put("export", (a, b, c) -> export(a));
        cmdTable.put("find", (a, b, c) -> find(a));
        cmdTable.put("help", (a, b, c) -> promptHelp());
        cmdTable.put("quit", (a, b, c) -> dummyQuit());
        cmdTable.put("remove", (a, b, c) -> remove(a, b));
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

    private static void cleanup(Scanner in, String filename) {
        String data = ParseUtil.toCSV(dataTable);
        if (isModified && askSave(in, filename, data)) {
            // something went wrong
            if (isEncrypted) {
                data = enc.encrypt(data);
            }
            tryAgainSave(in, data);
        }
    }

    private static void run(Scanner in, String data) {
        Map<String, Action> cmdTable = new HashMap<>();

        ParseUtil.parseCSV(dataTable, data);
        DisplayUtil.updatePaddingTable(dataTable);
        dataTable.sort(new TableSort());

        initCmdTable(cmdTable);
        prompt(in, cmdTable);
    }

    public static void main(final String[] args) {
        Scanner in = new Scanner(System.in);
        String filename;
        // if only a filename is provided, then file is encrypted
        isEncrypted = (args.length == 1);

        if (args.length > 0) {
            // filename is always last
            filename = args[args.length - 1];
        } else {
            printHelp();
            return;
        }

        String data = load(filename);
        if (data == null) {
            System.err.println("Could not load file: '" + filename + "'");
            return;
        }

        if (isEncrypted) {
            try {
                setPwdAskOnce(enc);
                data = enc.decrypt(data);
            } catch (EncryptionOperationNotPossibleException e) {
                System.err.println("Wrong password?");
                return;
            }
        }

        run(in, data);
        cleanup(in, filename);
    }
}
