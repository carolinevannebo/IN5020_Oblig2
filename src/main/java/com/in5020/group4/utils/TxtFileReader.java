package com.in5020.group4.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TxtFileReader {
    private final File file;
    private static final List<String> queries = new ArrayList<>();

    public TxtFileReader(File file) {
        this.file = file;
    }

    public List<String> getQueries() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.file));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                queries.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queries;
    }
}
