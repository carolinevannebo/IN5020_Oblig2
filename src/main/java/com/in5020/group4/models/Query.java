package com.in5020.group4.models;

import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.ArrayList;

@Component
public class Query implements Serializable {
    public String methodName;
    public ArrayList<Integer> params;

    public Query(String methodName, ArrayList<Integer> params) {
        this.methodName = methodName;
        this.params = params;
    }

    public void printQuery() {
        System.out.println("Method: " + methodName + " Params: " + params.toString());
    }
}
