package raft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileArray {
    private File file;
    private List<List<Integer>> lines;
    private boolean isModified;

    public FileArray(String filename) throws IOException {
        file = new File(filename);
        if (!file.exists()) {
            file.createNewFile();
        }
        lines = new ArrayList<>();
        readFile();
        isModified = false;
    }

    public void readFile() throws IOException {
        isModified = false;
        lines = new ArrayList<>();
        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                List<Integer> numbers = parseLine(line);
                lines.add(numbers);
            }
        }
    }

    private List<Integer> parseLine(String line) {
        String[] parts = line.split(" ");
        List<Integer> numbers = new ArrayList<>();
        for (String part : parts) {
            numbers.add(Integer.parseInt(part));
        }
        return numbers;
    }

    public List<Integer> get(int index) {
        if(lines.isEmpty() || index >= lines.size() || index < 0) {
           return new ArrayList<>();
        }
        return lines.get(index);
    }

    public void set(int index, List<Integer> value) {
        lines.set(index, value);
        isModified = true;
    }

    public void add(List<Integer> value) {
        lines.add(value);
        isModified = true;
    }

    public int size() {
        return lines.size();
    }

    public void truncate(int index) {
        if(index >= 0 && index < lines.size()) {
            lines.subList(index, lines.size()).clear();
            isModified = true;
        }
    }

    public void clearFileContents() throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            // Empty the file by overwriting it with an empty string
            fileWriter.write("");
        }
    }

    public void close() throws IOException {
        if (isModified) {
            clearFileContents();
            try (FileWriter fileWriter = new FileWriter(file);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                for (List<Integer> line : lines) {
                    String textLine = line.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(" "));
                    bufferedWriter.write(textLine);
                    bufferedWriter.newLine();
                }
            }
            isModified = false;
        }
    }
}
