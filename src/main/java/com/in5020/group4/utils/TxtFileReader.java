package com.in5020.group4.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TxtFileReader {
    private static final File file = null;
    private static final List<String> queries = new ArrayList<>();

    public static List<String> getQueries(String fileName) {
        File file = new File(System.getProperty("user.dir")+"/src/main/java/com/in5020/group4/utils/"+fileName);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
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
