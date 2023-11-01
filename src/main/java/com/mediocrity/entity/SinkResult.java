package com.mediocrity.entity;

import lombok.Data;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/24
 */

@Data
public class SinkResult{
    public int invokeLength;
    public String sinkCata;
    public String sinkLevel;
    public ArrayList<String> invokeDetail;
    private String format = "|%1$-5s|%2$-10s|%3$-6s|%4$-1s|";

    public SinkResult(int count, String sinkName, String severityLevel, ArrayList<String> result) {
        this.invokeLength = count;
        this.sinkCata = sinkName;
        this.sinkLevel = severityLevel;
        this.invokeDetail = result;
    }

    public String toString(){
        return String.format(format, invokeLength, sinkCata, sinkLevel,
                String.join("\t", invokeDetail));
    }
}
