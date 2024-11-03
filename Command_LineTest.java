import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

class Command_LineTest {
    private Command_Line cli;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private File tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempPath) {
        tempDir = tempPath.toFile();
        System.setOut(new PrintStream(outputStream));
        cli = new Command_Line();
        // Set current directory to temp directory for testing
        cli.setCurrentDirectory(tempDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testMkdir() throws IOException {
        // Test single directory creation
        simulateCommand("mkdir testDir");
        assertTrue(new File(tempDir, "testDir").exists());
        assertTrue(new File(tempDir, "testDir").isDirectory());

        // Test multiple directory creation
        simulateCommand("mkdir dir1 dir2 dir3");
        assertTrue(new File(tempDir, "dir1").exists());
        assertTrue(new File(tempDir, "dir2").exists());
        assertTrue(new File(tempDir, "dir3").exists());
    }

    @Test
    void testTouch() throws IOException {
        // Test single file creation
        simulateCommand("touch test.txt");
        assertTrue(new File(tempDir, "test.txt").exists());

        // Test multiple file creation
        simulateCommand("touch file1.txt file2.txt file3.txt");
        assertTrue(new File(tempDir, "file1.txt").exists());
        assertTrue(new File(tempDir, "file2.txt").exists());
        assertTrue(new File(tempDir, "file3.txt").exists());
    }

    @Test
    void testLs() throws IOException {
        // Create test files and directories
        new File(tempDir, "file1.txt").createNewFile();
        new File(tempDir, "file2.txt").createNewFile();
        new File(tempDir, "testDir").mkdir();

        // Test basic ls
        simulateCommand("ls");
        String output = outputStream.toString();
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("testDir"));

        // Test ls with hidden files
        new File(tempDir, ".hidden").createNewFile();
        outputStream.reset();
        simulateCommand("ls -a");
        output = outputStream.toString();
        assertTrue(output.contains(".hidden"));
    }

    @Test
    void testCat() throws IOException {
        // Test file creation and content writing
        String testContent = "Hello\nWorld\n";
        Files.write(tempDir.toPath().resolve("test.txt"), testContent.getBytes());

        // Test cat file
        simulateCommand("cat test.txt");
        assertEquals(testContent, outputStream.toString());
    }

    @Test
    void testCp() throws IOException {
        // Create source file
        String testContent = "Test content";
        Files.write(tempDir.toPath().resolve("source.txt"), testContent.getBytes());

        // Test file copy
        simulateCommand("cp source.txt dest.txt");
        assertTrue(new File(tempDir, "dest.txt").exists());
        assertEquals(testContent,
                new String(Files.readAllBytes(tempDir.toPath().resolve("dest.txt"))));

        // Test copy to directory
        new File(tempDir, "testDir").mkdir();
        simulateCommand("cp source.txt testDir");
        assertTrue(new File(tempDir, "testDir/source.txt").exists());
    }

    @Test
    void testMv() throws IOException {
        // Create source file
        String testContent = "Test content";
        Files.write(tempDir.toPath().resolve("source.txt"), testContent.getBytes());

        // Test file move
        simulateCommand("mv source.txt dest.txt");
        assertTrue(new File(tempDir, "dest.txt").exists());
        assertFalse(new File(tempDir, "source.txt").exists());
        assertEquals(testContent,
                new String(Files.readAllBytes(tempDir.toPath().resolve("dest.txt"))));

        // Test move to directory
        new File(tempDir, "testDir").mkdir();
        simulateCommand("mv dest.txt testDir");
        assertTrue(new File(tempDir, "testDir/dest.txt").exists());
        assertFalse(new File(tempDir, "dest.txt").exists());
    }

    @Test
    void testRm() throws IOException {
        // Create test files
        new File(tempDir, "file1.txt").createNewFile();
        new File(tempDir, "file2.txt").createNewFile();

        // Test single file removal
        simulateCommand("rm file1.txt");
        assertFalse(new File(tempDir, "file1.txt").exists());
        assertTrue(new File(tempDir, "file2.txt").exists());

        // Test recursive directory removal
        new File(tempDir, "testDir").mkdir();
        new File(tempDir, "testDir/nested").mkdir();
        new File(tempDir, "testDir/nested/file.txt").createNewFile();

        simulateCommand("rm -r testDir");
        assertFalse(new File(tempDir, "testDir").exists());
    }

    @Test
    void testRmdir() throws IOException {
        // Create test directories
        new File(tempDir, "emptyDir").mkdir();
        new File(tempDir, "nonEmptyDir").mkdir();
        new File(tempDir, "nonEmptyDir/file.txt").createNewFile();

        // Test empty directory removal
        simulateCommand("rmdir emptyDir");
        assertFalse(new File(tempDir, "emptyDir").exists());

        // Test non-empty directory removal (should fail)
        simulateCommand("rmdir nonEmptyDir");
        assertTrue(new File(tempDir, "nonEmptyDir").exists());
    }

    @Test
    void testCd() throws IOException {
        // Create test directory structure
        new File(tempDir, "testDir").mkdir();
        new File(tempDir, "testDir/nestedDir").mkdir();

        // Test changing to subdirectory
        simulateCommand("cd testDir");
        assertEquals(
                new File(tempDir, "testDir").getAbsolutePath(),
                cli.getCurrentDirectory()
        );

        // Test changing to parent directory
        simulateCommand("cd ..");
        assertEquals(tempDir.getAbsolutePath(), cli.getCurrentDirectory());

        // Test changing to current directory
        simulateCommand("cd .");
        assertEquals(tempDir.getAbsolutePath(), cli.getCurrentDirectory());
    }

    @Test
    void testPipingWithLS() throws IOException {
        // Ensure there are files to list (create one if necessary)
        File fileToEnsureListing = new File(tempDir, "fileForListing.txt");
        if (!fileToEnsureListing.exists()) {
            fileToEnsureListing.createNewFile();
        }

        // Test piping 'ls' to a file using '>>' for append
        simulateCommand("ls >> lsOutput.txt");
        File lsOutputFile = new File(tempDir, "lsOutput.txt");
        assertTrue(lsOutputFile.exists());
        String lsOutput = new String(Files.readAllBytes(lsOutputFile.toPath()));
        assertTrue(lsOutput.contains(fileToEnsureListing.getName())); // Check if the file is listed
    }

    @Test
    void testPwd() throws IOException {
        simulateCommand("pwd");
        String output = outputStream.toString();
        assertTrue(output.contains(tempDir.getAbsolutePath()));
    }

    @Test
    void testHelp() throws IOException {
        simulateCommand("help");
        String output = outputStream.toString();
        assertTrue(output.contains("Available commands:"));
        assertTrue(output.contains("mv <source> <destination> - Moves a file or directory to a new location."));
        // Add more assertions for other commands if needed
    }


    @Test
    void testPipingWithCat() throws IOException {
        // Create test file
        Files.write(tempDir.toPath().resolve("test.txt"), "Hello\nWorld\n".getBytes());

        // Test simple pipe
        simulateCommand("cat test.txt | cat");
        String output = outputStream.toString();
        assertEquals("Hello\nWorld\n", output);
    }

    @Test
    void testInvalidCommands() throws IOException {
        // Test unknown command
        simulateCommand("unknownCommand");
        String output = outputStream.toString();
        assertTrue(output.contains("Unknown command: unknownCommand"));

        // Test command with insufficient arguments
        simulateCommand("cp");
        output = outputStream.toString();
        assertTrue(output.contains("Usage: cp <source_file(s)> <destination>"));

        // Test command with too many arguments
        simulateCommand("cp source.txt dest.txt extraArg");
        output = outputStream.toString();
        // Depending on implementation, this might not produce an error, but it's a good practice to test
    }

    @Test
    void testCatMultipleFiles() throws IOException {
        // Create test files
        Files.write(tempDir.toPath().resolve("file1.txt"), "Content 1".getBytes());
        Files.write(tempDir.toPath().resolve("file2.txt"), "Content 2".getBytes());

        // Test cat with multiple files
        simulateCommand("cat file1.txt file2.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Content 1"));
        assertTrue(output.contains("Content 2"));
    }

    @Test
    void testCatNonExistentFile() throws IOException {
        // Test cat with non-existent file
        simulateCommand("cat nonExistentFile.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("File not found: nonExistentFile.txt"));
    }

    @Test
    void testCpNonExistentSource() throws IOException {
        // Test cp with non-existent source file
        simulateCommand("cp nonExistentSource.txt dest.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Source file does not exist: nonExistentSource.txt"));
    }

    @Test
    void testMvNonExistentSource() throws IOException {
        // Test mv with non-existent source file
        simulateCommand("mv nonExistentSource.txt dest.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Source does not exist: nonExistentSource.txt"));
    }

    @Test
    void testRmNonExistentFile() throws IOException {
        // Test rm with non-existent file
        simulateCommand("rm nonExistentFile.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Failed to remove file: nonExistentFile.txt"));
    }

    @Test
    void testRmdirNonEmptyDirectory() throws IOException {
        // Create a non-empty directory
        new File(tempDir, "nonEmptyDir").mkdir();
        new File(tempDir, "nonEmptyDir/file.txt").createNewFile();

        // Test rmdir with non-empty directory
        simulateCommand("rmdir nonEmptyDir");
        String output = outputStream.toString();
        assertTrue(output.contains("nonEmptyDir Directory is not empty.\n"));
    }

    @Test
    void testRmdirNonExistentDirectory() throws IOException {
        // Test rmdir with non-existent directory
        simulateCommand("rmdir nonExistentDir");
        String output = outputStream.toString();
        assertTrue(output.contains("nonExistentDir directory not found.\n"));
    }

    @Test
    void testCdNonExistentDirectory() throws IOException {
        // Test cd with non-existent directory
        simulateCommand("cd nonExistentDir");
        String output = outputStream.toString();
        assertTrue(output.contains("Directory not found: nonExistentDir"));
    }

    @Test
    void testCdRelativeToRoot() throws IOException {
        // Test cd with relative path to root (../)
        new File(tempDir, "testDir").mkdir();
        cli.setCurrentDirectory(new File(tempDir, "testDir").getAbsolutePath());
        simulateCommand("cd ..");
        assertEquals(tempDir.getAbsolutePath(), cli.getCurrentDirectory());
    }



    @Test
    void testRmWithRecursiveOption() throws IOException {
        // Create test files and directories
        new File(tempDir, "file1.txt").createNewFile();
        new File(tempDir, "file2.txt").createNewFile();
        new File(tempDir, "testDir").mkdir();

        // Test rm command with recursive option
        simulateCommand("rm -r testDir");
        assertFalse(new File(tempDir, "testDir").exists());
    }

    @Test
    void testRmdirWithRecursiveOption() throws IOException {
        // Create test files and directories
        new File(tempDir, "file1.txt").createNewFile();
        new File(tempDir, "file2.txt").createNewFile();
        new File(tempDir, "testDir").mkdir();

        // Test rmdir command with recursive option
        simulateCommand("rmdir -r testDir");
        assertFalse(new File(tempDir, "testDir").exists());
    }

    @Test
    void testCdWithAbsolutePath() {
        // Create test files and directories
        new File(tempDir, "testDir").mkdir();

        // Test cd command with absolute path
        simulateCommand("cd /absolute/path");
        assertEquals(tempDir.getAbsolutePath(), cli.getCurrentDirectory());
    }

    @Test
    void testCdWithRelativePath() {
        // Create test files and directories
        new File(tempDir, "testDir").mkdir();

        // Test cd command with relative path
        simulateCommand("cd testDir");
        assertEquals(new File(tempDir, "testDir").getAbsolutePath(), cli.getCurrentDirectory());
    }

    @Test
    void testCdWithInvalidPath() {
        // Test cd command with an invalid path
        simulateCommand("cd invalidPath");
        String output = outputStream.toString();
        assertTrue(output.contains("Directory not found: invalidPath"));
    }

    @Test
    void testCdWithNonExistentDirectory() {
        // Test cd command with a non-existent directory
        simulateCommand("cd nonExistentDir");
        String output = outputStream.toString();
        assertTrue(output.contains("Directory not found: nonExistentDir"));
    }

    @Test
    void testCpWithNonExistentSourceFile() {
        // Test cp command with a non-existent source file
        simulateCommand("cp nonExistentSource.txt dest.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Source file does not exist: nonExistentSource.txt"));
    }

    @Test
    void testMvWithNonExistentSourceFile() {
        // Test mv command with a non-existent source file
        simulateCommand("mv nonExistentSource.txt dest.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Source does not exist: nonExistentSource.txt"));
    }


    @Test
    void testCatWithNonExistentFile() {
        // Test cat command with a non-existent file
        simulateCommand("cat nonExistentFile.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("File not found: nonExistentFile.txt"));
    }

    @Test
    void testPwd_AfterNavigation() throws IOException {
        // Test pwd after navigating through directories
        simulateCommand("mkdir testDir");
        simulateCommand("cd testDir");
        simulateCommand("pwd");
        String output = outputStream.toString();
        assertTrue(output.contains(new File(tempDir, "testDir").getAbsolutePath()));
    }

    @Test
    void testCd_DeepDirectoryStructure() throws IOException {
        // Test cd with deeper directory structures
        simulateCommand("mkdir dir1");
        simulateCommand("cd dir1");
        simulateCommand("mkdir dir2");
        simulateCommand("cd dir2");
        simulateCommand("mkdir dir3");
        simulateCommand("cd dir3");
        assertEquals(new File(tempDir, "dir1/dir2/dir3").getAbsolutePath(), cli.getCurrentDirectory());
    }


    @Test
    void testLs_Recursive() throws IOException {
        // Assuming -r is for recursive listing
        simulateCommand("mkdir dir1");
        simulateCommand("touch dir1/file.txt");
        simulateCommand("ls -r");
        String output = outputStream.toString();
        assertTrue(output.contains("file.txt")); // Adjust based on your ls -r output format
    }


    @Test
    void testRmdir_DirectoryWithNameSpace() throws IOException {
        // Test rmdir with a directory that has a space
        simulateCommand("mkdir 'Directory With Space'");
        simulateCommand("rmdir 'Directory With Space'");
        assertFalse(new File(tempDir, "Directory With Space").exists());
    }


    @Test
    void testCat_MultipleFiles_Output() throws IOException {
        // Test cat with multiple files and output
        Files.write(tempDir.toPath().resolve("file1.txt"), "Content 1".getBytes());
        Files.write(tempDir.toPath().resolve("file2.txt"), "Content 2".getBytes());
        simulateCommand("cat file1.txt file2.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("Content 1"));
        assertTrue(output.contains("Content 2"));
    }

    @Test
    void testCat_NonExistentFile_ErrorMessage() throws IOException {
        // Test cat with a non-existent file and error message
        simulateCommand("cat nonExistentFile.txt");
        String output = outputStream.toString();
        assertTrue(output.contains("File not found: nonExistentFile.txt"));
    }

    @Test
    void testRedirect_OutputToFile() throws IOException {
        // Test > redirecting output to a file
        simulateCommand("ls > outputFile.txt");
        File outputFile = new File(tempDir, "outputFile.txt");
        assertTrue(outputFile.exists());
    }

    @Test
    void testAppend_OutputToFile() throws IOException {
        // Test >> appending output to a file
        Files.write(tempDir.toPath().resolve("appendFile.txt"), "Initial Content".getBytes());
        simulateCommand("ls >> appendFile.txt");
        String output = new String(Files.readAllBytes(tempDir.toPath().resolve("appendFile.txt")));
        assertTrue(output.contains("Initial Content")); // And optionally, the ls output
    }

    @Test
    void testMultiplePipe() throws IOException {
        // Create a sample file and directory in tempDir to list
        File sampleFile = new File(tempDir, "sampleFile.txt");
        sampleFile.createNewFile();
        File sampleDir = new File(tempDir, "sampleDir");
        sampleDir.mkdir();

        // Run the command `ls | >> output.txt`
        simulateCommand("ls | >> output.txt");

        // Verify output file was created
        File outputFile = new File(tempDir, "output.txt");
        assertTrue(outputFile.exists(), "Output file was not created.");

        // Verify contents of output.txt
        String outputContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(outputContent.contains("sampleFile.txt"), "Output does not contain the expected file.");
        assertTrue(outputContent.contains("sampleDir"), "Output does not contain the expected directory.");
    }


    private void simulateCommand(String command) {
        // Create a new scanner with the command and process it
        Scanner mockScanner = new Scanner(command + "\nexit\n");
        cli.setScanner(mockScanner);
        cli.processInput(command);
    }
}