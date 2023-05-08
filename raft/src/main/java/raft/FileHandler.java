package raft;

import java.io.*;

public class FileHandler {
    private File file;
    private FileWriter fileWriter;
    private FileReader fileReader;
    private BufferedReader bufferedReader;

    public FileHandler(String filename) throws IOException {
        // Create and/or open the file
        file = new File(filename);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public void writeToFile(String content) throws IOException {
        // Open the FileWriter for the file
        fileWriter = new FileWriter(file, true);

        // Write content to the file
        fileWriter.write(content);
        fileWriter.flush();
    }

    public String readFromFile() throws IOException {
        // Open the FileReader and BufferedReader for the file
        fileReader = new FileReader(file);
        bufferedReader = new BufferedReader(fileReader);

        // Read and return the content of the file
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

    public void close() throws IOException {
        // Close the file resources
        if (fileWriter != null) {
            fileWriter.close();
        }
        if (bufferedReader != null) {
            bufferedReader.close();
        }
        if (fileReader != null) {
            fileReader.close();
        }
    }
}