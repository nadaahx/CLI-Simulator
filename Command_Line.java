import java.io.*;
import java.util.*;

public class Command_Line {
    private static String currentDirectory;
    private Scanner scanner;
    private boolean isRunning;

    public Command_Line() {
        currentDirectory = System.getProperty("user.dir");
        scanner = new Scanner(System.in);
        isRunning = true;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String directory) {
        currentDirectory = directory;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }


    public void start() {
        while (isRunning) {
            System.out.print(currentDirectory + "$ ");
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                processInput(input);
            }
        }
    }

    public void processInput(String input) {
        if (input.startsWith("cat >")) {
            // Handle cat > and cat >> commands
            boolean append = input.startsWith("cat >>");
            String filename = input.substring(append ? 6 : 5).trim();
            handleCatRedirection(filename, append);
            return;
        }

        String[] pipedCommands = input.split("\\|");
        String result = null;

        for (int i = 0; i < pipedCommands.length; i++) {
            String currentCommand = pipedCommands[i].trim();

            // Handle redirection at the end of the pipe chain
            if (i == pipedCommands.length - 1 && (currentCommand.contains(">") || currentCommand.equals(">"))) {
                String[] redirectParts = currentCommand.split(">");
                String file = redirectParts[redirectParts.length - 1].trim();

                // If there's a command before the redirection, process it
                if (redirectParts[0].trim().length() > 0) {
                    result = processCommand(redirectParts[0].trim(), result);
                }

                // Handle the redirection
                boolean append = currentCommand.contains(">>");
                if (append) {
                    appendToFile(file, result != null ? result : "");
                } else {
                    writeToFile(file, result != null ? result : "");
                }
                return;
            }

            result = processCommand(currentCommand, result);

            // Only print output if it's the last command and not being redirected
            if (i == pipedCommands.length - 1 && result != null && !result.isEmpty()) {
                System.out.print(result);
            }
        }
    }

    void handleCatRedirection(String filename, boolean append) {
        System.out.println("Enter text (type 'exit' on a new line to finish):");
        StringBuilder content = new StringBuilder();
        Scanner inputScanner = new Scanner(System.in);

        while (true) {
            String line = inputScanner.nextLine();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            content.append(line).append("\n");
        }

        if (append) {
            appendToFile(filename, content.toString());
        } else {
            writeToFile(filename, content.toString());
        }
    }

    private String processCommand(String command, String input) {
        String[] cmdParts = command.split("\\s+");
        StringBuilder output = new StringBuilder();

        switch (cmdParts[0].toLowerCase()) {
            case "exit":
                isRunning = false;
                System.out.println("Exiting...");
                return null;

            case "rmdir":
                if (cmdParts.length > 1) {
                    for (int i = 1; i < cmdParts.length; i++) {
                        output.append(removeDirectory(cmdParts[i]));
                    }
                } else {
                    output.append("Usage: rmdir <directory_name1> [<directory_name2> ...]\n");
                }
                break;

            case "ls":
                output.append(listDirectory(command.contains(" -a"), command.contains(" -r")));
                break;

            case "cp":
                if (cmdParts.length == 3) {
                    output.append(copyFileOrToDirectory(cmdParts[1], cmdParts[2]));
                } else if (cmdParts.length > 3) {
                    // Multiple files to directory case
                    String destDir = cmdParts[cmdParts.length - 1];
                    File dest = new File(currentDirectory, destDir);

                    if (!dest.exists()) {
                        output.append("Destination directory does not exist: ").append(destDir).append("\n");
                    } else if (!dest.isDirectory()) {
                        output.append("Destination must be a directory when copying multiple files\n");
                    } else {
                        // Copy each file to the destination directory
                        for (int i = 1; i < cmdParts.length - 1; i++) {
                            output.append(copyFileOrToDirectory(cmdParts[i], destDir));
                        }
                    }
                } else {
                    output.append("Usage: cp <source_file(s)> <destination>\n");
                }
                break;

            case "pwd":
                output.append(currentDirectory).append("\n");
                break;

            case "mkdir":
                if (cmdParts.length > 1) {
                    for (int i = 1; i < cmdParts.length; i++) {
                        output.append(createDirectory(cmdParts[i]));
                    }
                } else {
                    output.append("Usage: mkdir <directory_name1> [<directory_name2> ...]\n");
                }
                break;

            case "rm":
                if (cmdParts.length > 1) {
                    if (cmdParts[1].equals("-r")) {
                        for (int i = 2; i < cmdParts.length; i++) {
                            output.append(removeDirectoryRecursive(cmdParts[i]));
                        }
                    } else {
                        for (int i = 1; i < cmdParts.length; i++) {
                            output.append(removeFile(cmdParts[i]));
                        }
                    }
                } else {
                    output.append("Usage: rm [-r] <file/directory>\n");
                }
                break;

            case ">":
                if (cmdParts.length == 3) {
                    output.append(writeToFile(cmdParts[1], cmdParts[2]));
                } else {
                    output.append("Usage: > <file_name> <text_to_write>\n");
                }
                break;


            case "cat":
                if (cmdParts.length == 1) {
                    if (input != null) {
                        // If there's piped input, display it directly
                        return input;
                    } else {
                        readFromUserInput();
                    }
                } else {
                    for (int i = 1; i < cmdParts.length; i++) {
                        output.append(displayFileContents(cmdParts[i]));
                    }
                }
                break;


            case ">>":
                if (cmdParts.length >= 2) {
                    String fileName = cmdParts[1];
                    if (input != null) {
                        // Handle piped input
                        output.append(appendToFile(fileName, input));
                    } else if (cmdParts.length >= 3) {
                        // Handle direct input
                        String content = String.join(" ", Arrays.copyOfRange(cmdParts, 2, cmdParts.length));
                        output.append(appendToFile(fileName, content));
                    }
                } else {
                    output.append("Usage: >> <file_name> [<text_to_append>]\n");
                }
                break;

            case "cd":
                if (cmdParts.length == 2) {
                    changeDirectory(cmdParts[1]);
                } else {
                    System.out.println("Usage: cd <directory>");
                }
                break;

            case "mv":
                if (cmdParts.length >= 3) {
                    String destPath = cmdParts[cmdParts.length - 1];
                    File dest = new File(currentDirectory, destPath);

                    if (cmdParts.length > 3 && !dest.isDirectory()) {
                        output.append("Destination must be a directory when moving multiple files\n");
                    } else {
                        for (int i = 1; i < cmdParts.length - 1; i++) {
                            output.append(moveFileOrDirectory(cmdParts[i], destPath));
                        }
                    }
                } else {
                    output.append("Usage: mv <source(s)> <destination>\n");
                }
                break;

            case "touch":
                if (cmdParts.length > 1) {
                    for (int i = 1; i < cmdParts.length; i++) {
                        output.append(createFile(cmdParts[i]));
                    }
                } else {
                    output.append("Usage: touch <filename> [<filename2> ...]\n");
                }
                break;


            case "help":
                output.append("Available commands:\n");
                output.append("mv <source> <destination> - Moves a file or directory to a new location.\n");
                output.append("cd <directory>           - Changes the current directory.\n");
                output.append("touch <filename>         - Creates a new empty file.\n");
                output.append("rmdir <directory_name>   - Removes an empty directory.\n");
                output.append("ls [-a] [-r]             - Lists files in the current directory.\n");
                output.append("cp <source_file> <destination_file>  - Copies a file.\n");
                output.append("cat <file_name>          - Displays contents of a file.\n");
                output.append(">> <file_name> <text>    - Appends text to a file.\n");
                output.append("rm <file_name>           - Removes a file.\n");
                output.append("mkdir <directory_name>    - Creates a new directory.\n");
                output.append("pwd                      - Prints the current working directory.\n");
                output.append("> <file_name> <text>     - Redirects output to a file (overwrites).\n");
                output.append("| <command1> | <command2> - Pipes the output of command1 into command2.\n");
                output.append("exit                     - Exits the command line.\n");

                break;

            default:
                output.append("Unknown command: ").append(cmdParts[0]).append(". Type 'help' for a list of commands.\n");
                return output.toString();
        }

        return output.toString();
    }

    private String createDirectory(String dirName) {
        File dir = new File(currentDirectory, dirName);
        return dir.mkdir() ? "Directory created: " + dirName + "\n" : "Failed to create directory: " + dirName + "\n";
    }

    private String removeFile(String fileName) {
        File file = new File(currentDirectory, fileName);
        return (file.exists() && file.delete()) ? "File removed: " + fileName + "\n" : "Failed to remove file: " + fileName + "\n";
    }

    private String writeToFile(String fileName, String content) {
        File file = new File(currentDirectory, fileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            writer.print(content);
            return "Content written to " + fileName + "\n";
        } catch (IOException e) {
            return "Failed to write to file: " + e.getMessage() + "\n";
        }
    }

    private String removeDirectoryRecursive(String dirPath) {
        File dir = new File(currentDirectory, dirPath);
        if (!dir.exists()) {
            return "Directory not found: " + dirPath + "\n";
        }
        return deleteRecursive(dir) ? "Removed directory and its contents: " + dirPath + "\n"
                : "Failed to remove directory: " + dirPath + "\n";
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursive(f);
                }
            }
        }
        return file.delete();
    }

    private String removeDirectory(String dirName) {
        File dir = new File(currentDirectory, dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            return dirName + " directory not found.\n";
        }

        String[] files = dir.list();
        if (files == null || files.length == 0) {
            if (dir.delete()) {
                return dirName + " Directory removed.\n";
            } else {
                return "Error removing directory: " + dirName + "\n";
            }
        } else {
            return dirName + " Directory is not empty.\n";
        }
    }

    private String listDirectory(boolean showHidden, boolean reverseOrder) {
        StringBuilder output = new StringBuilder();
        File dir = new File(currentDirectory);
        File[] files = dir.listFiles();

        if (files == null) {
            return "Cannot access directory: " + currentDirectory + "\n";
        }

        List<File> fileList = Arrays.asList(files);
        if (reverseOrder) {
            fileList.sort(Comparator.comparing(File::getName).reversed());
        } else {
            fileList.sort(Comparator.comparing(File::getName));
        }

        for (File file : fileList) {
            // Skip hidden files if not showing hidden
            if (!showHidden && file.getName().startsWith(".")) {
                continue;
            }

            // Check if it's a directory
            if (file.isDirectory()) {
                output.append(file.getName()).append("\n");
            }
            // Print .txt files without any tags, just the name
            else if (!file.getName().contains(".")) {
                output.append(file.getName()).append(".txt\n");
            }
            // For all other files, print their names normally
            else {
                output.append(file.getName()).append("\n");
            }
        }

        return output.toString();
    }

    private String copyFile(String sourceFile, String destinationFile) {
        File src = new File(currentDirectory, sourceFile);
        File dest = new File(currentDirectory, destinationFile);

        if (!src.exists()) return "Source file not found: " + sourceFile + "\n";

        try (FileInputStream fis = new FileInputStream(src); FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return "File copied from " + sourceFile + " to " + destinationFile + "\n";
        } catch (IOException e) {
            return "Error during file copy: " + e.getMessage() + "\n";
        }
    }

    private String copyFileToDirectory(String sourceFile, File destinationDirectory) {
        return copyFile(sourceFile, new File(destinationDirectory, new File(sourceFile).getName()).getPath());
    }

    private String displayFileContents(String fileName) {
        StringBuilder output = new StringBuilder();
        File file = new File(currentDirectory, fileName);
        if (!file.exists()) {
            return "File not found: " + fileName + "\n";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage() + "\n";
        }
        return output.toString();
    }

    private void readFromUserInput() {
        System.out.println("Enter text (type 'exit' to finish input):");
        StringBuilder input = new StringBuilder();
        Scanner userScanner = new Scanner(System.in);

        String line;
        while (true) {
            line = userScanner.nextLine();
            if (line.equalsIgnoreCase("exit") ) {
                break;
            }
            input.append(line).append(System.lineSeparator());
        }

        System.out.println("You entered:");
        System.out.println(input.toString());
    }
    private String appendToFile(String fileName, String content) {
        File file = new File(currentDirectory, fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.print(content);
                return "Content appended to " + fileName + "\n";
            }
        } catch (IOException e) {
            return "Failed to append to file: " + e.getMessage() + "\n";
        }
    }

    private String moveFile(String sourcePath, String destinationPath) {
        File src = new File(currentDirectory, sourcePath);
        File dest = new File(currentDirectory, destinationPath);

        if (!src.exists()) {
            return "Source does not exist: " + sourcePath + "\n";
        }

        // If destination is a directory, move into it
        if (dest.isDirectory()) {
            dest = new File(dest, src.getName());
        }

        // Create parent directories if they don't exist
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }

        try {
            if (src.renameTo(dest)) {
                return "Successfully moved " + sourcePath + " to " + destinationPath + "\n";
            } else {
                // If simple rename fails, try copy and delete
                if (src.isDirectory()) {
                    copyDirectory(src, dest);
                } else {
                    copyFile(sourcePath, destinationPath);
                }
                deleteRecursive(src);
                return "Successfully moved " + sourcePath + " to " + destinationPath + "\n";
            }
        } catch (IOException e) {
            return "Failed to move: " + e.getMessage() + "\n";
        }
    }

    private void copyDirectory(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.mkdir();
        }
        File[] files = src.listFiles();
        if (files != null) {
            for (File file : files) {
                File newDest = new File(dest, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, newDest);
                } else {
                    copyFile(file.getPath(), newDest.getPath());
                }
            }
        }
    }

    private String createFile(String filename) {
        File file = new File(currentDirectory, filename);
        try {
            return file.createNewFile() ? "File created: " + filename + "\n" : "File already exists: " + filename + "\n";
        } catch (IOException e) {
            return "Failed to create file: " + filename + "\n";
        }
    }

    private void changeDirectory(String path) {
        File newDir = new File(currentDirectory, path);
        if(path.equals(".")){
            return;
        }
        if (path.equals("..")) {
            currentDirectory = new File(currentDirectory).getParent();
        } else if (newDir.isDirectory()) {
            currentDirectory = newDir.getAbsolutePath();
        }
        else {
            System.out.println("Directory not found: " + path);
        }
    }


    private String copyFileOrToDirectory(String sourcePath, String destPath) {
        File sourceFile = new File(currentDirectory, sourcePath);
        File destFile = new File(currentDirectory, destPath);

        if (!sourceFile.exists()) {
            return "Source file does not exist: " + sourcePath + "\n";
        }

        try {
            if (destFile.isDirectory()) {
                // If destination is a directory, create a new file inside it with the source file's name
                destFile = new File(destFile, sourceFile.getName());
            }

            // Ensure parent directories exist
            if (destFile.getParentFile() != null) {
                destFile.getParentFile().mkdirs();
            }

            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
            return "Successfully copied " + sourcePath + " to " + destPath + "\n";
        } catch (IOException e) {
            return "Failed to copy file: " + e.getMessage() + "\n";
        }
    }

    private String moveFileOrDirectory(String sourcePath, String destPath) {
        File sourceFile = new File(currentDirectory, sourcePath);
        File destFile = new File(currentDirectory, destPath);

        if (!sourceFile.exists()) {
            return "Source does not exist: " + sourcePath + "\n";
        }

        try {
            if (destFile.isDirectory()) {
                // If destination is a directory, move the file into it
                destFile = new File(destFile, sourceFile.getName());
            }

            // Ensure parent directories exist
            if (destFile.getParentFile() != null) {
                destFile.getParentFile().mkdirs();
            }

            if (sourceFile.renameTo(destFile)) {
                return "Successfully moved " + sourcePath + " to " + destPath + "\n";
            } else {
                // If rename fails, try copy and delete
                copyFileOrToDirectory(sourcePath, destPath);
                if (sourceFile.delete()) {
                    return "Successfully moved " + sourcePath + " to " + destPath + "\n";
                } else {
                    return "Copied file but failed to remove source: " + sourcePath + "\n";
                }
            }
        } catch (Exception e) {
            return "Failed to move file: " + e.getMessage() + "\n";
        }
    }

    public static void main(String[] args) {
        Command_Line cli = new Command_Line();
        cli.start();
    }
}
