# Command Line Interpreter Simulator (CLI)

A custom Command Line Interpreter (CLI) built in Java, inspired by Unix/Linux shell functionality. This project supports essential command-line operations with built-in error handling and includes automated testing via JUnit to ensure command reliability.

## Features

### 1. **Command Execution**

- **Directory & File Management**:
    - Commands include `pwd`, `cd`, `ls` (with `a` for hidden files and `r` for reverse order), `mkdir`, `rmdir`, `touch`, `mv`, `rm` (`-r` flag for recursive deletion) , and `cat`.
- **Redirection & Piping**:
    - Supports output redirection (`>` for overwrite, `>>` for appending).
    - Enables command piping (`|`) to chain outputs between commands.

### 2. **Internal Commands**

- `exit`: Closes the CLI.
- `help`: Displays available commands and their usage details.

### 3. **Error Handling**

- Gracefully handles invalid commands and parameters, displaying appropriate error messages without terminating the session.

## Project Structure

- **Source Code**: The core functionality is contained.
- **JUnit Tests**: unit tests for each command, validating correct functionality and edge cases.

## Testing with JUnit

JUnit ensures that each command behaves as expected:

- **Annotations**: Uses `@Test` for marking test cases.
- **Assertions**: Functions like `assertEquals()` and `assertTrue()` verify command outputs.

### Prerequisites

- **Java**: Java installed on your machine.
- **JUnit**: Set up JUnit for testing.

## Usage Examples

### Basic Operations

```bash
 Directory navigation
$ pwd
/home/user
$ cd documents
$ mkdir new_folder

# File operations
$ touch file.txt
$ cat > file.txt
Hello World!
exit
$ cat file.txt
Hello World!

```

### Advanced Features

```bash
# Using pipes and redirection
$ ls -a | >> files.txt
$ cat files.txt
.
..
file.txt
files.txt
new_folder

```

## Error Handling Examples

```bash
# Invalid command
$ invalid_command
Unknown command: invalid_command. Type 'help' for a list of commands.

# Directory not found
$ cd nonexistent
Directory not found: nonexistent

# File operation errors
$ rm nonexistent.txt
Failed to remove file: nonexistent.txt

```

## Additional Notes

- **No `exec` Use**: Commands are implemented without `exec`.
- **Collaborative Project**: Designed and implemented by a group of four developers.
