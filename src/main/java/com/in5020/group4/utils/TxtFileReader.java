package com.in5020.group4.utils;

import com.in5020.group4.models.Query;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtFileReader {
    private static final File file = new File(System.getProperty("user.dir")+"/src/main/java/com/in5020/group4/utils/input.txt");
    private static final List<String> queries = new ArrayList<>();

    public static List<String> getQueries() {
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
        //readInputFile();
        //return queries;
    }

    /*private static void readInputFile() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            Pattern pattern = Pattern.compile("^(\\S+)(?:\\s+(.*))?$");
            String line = "";

            while ((line = bufferedReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    // group line
                    String methodName = matcher.group(1);
                    String paramsString = matcher.group(2) != null ? matcher.group(2).trim() : ""; // capture params // todo: rename

                    ArrayList<Integer> args = new ArrayList<>();
                    // parser interprets params
                    String[] params = paramsString.split(" ");

                    // check that methods needing params has them
                    boolean needsParams = methodName.equals("deposit") ||
                            methodName.equals("addInterest") ||
                            methodName.equals("checkTxStatus") ||
                            methodName.equals("sleep");

                    if (needsParams) {
                        if (params.length == 0) continue;
                        List<Integer> numericParts = new ArrayList<>();

                        // ensure correct type
                        for (String param : params) {
                            if (isNumeric(param)) {
                                numericParts.add(Integer.parseInt(param));
                            } else {
                                System.out.println("Param for input is not integer");
                            }
                        }

                        // ensure not empty
                        if (!numericParts.isEmpty()) args.addAll(numericParts);
                        if (args.isEmpty()) continue;
                    }

                    queries.add(new Query(methodName, args));
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }*/
}
