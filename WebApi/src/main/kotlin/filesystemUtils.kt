import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Safely executes a directory listing command and returns the standard output.
 *
 * @param directory The target directory to list. Defaults to the current working directory.
 * @param timeoutSeconds Maximum time allowed for the process to complete.
 * @return The command output as a String.
 * @throws IOException If process execution fails or returns a non-zero exit code.
 */
fun getDirectoryListing(
    directory: File = File("."),
    timeoutSeconds: Long = 5
): String {
    // Determine the operating system to build the correct command array
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val command = if (isWindows) {
        listOf("cmd.exe", "/c", "dir")
    } else {
        listOf("ls", "-la")
    }

    val processBuilder = ProcessBuilder(command).apply {
        directory(directory)
        // Redirect error stream to standard output stream to capture errors in output
        redirectErrorStream(true)
    }

    val process = processBuilder.start()

    // Safely read output from the input stream
    val output = process.inputStream.bufferedReader().use { it.readText() }

    // Enforce a timeout to prevent process hang/deadlock
    val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
    if (!completed) {
        process.destroyForcibly()
        throw IOException("Command timed out after $timeoutSeconds seconds.")
    }

    if (process.exitValue() != 0) {
        throw IOException("Command failed with exit code ${process.exitValue()}:\n$output")
    }

    return output
}
