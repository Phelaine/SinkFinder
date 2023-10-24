package com.mediocrity.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;

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

    public SinkResult(int count, String sinkName, String severityLevel, ArrayList<String> result) {
        this.invokeLength = count;
        this.sinkCata = sinkName;
        this.sinkLevel = severityLevel;
        this.invokeDetail = result;
    }
}
