package com.mediocrity.entity;

import lombok.Data;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Data
public class SinkRule {
    private String sinkName;
    private String sinkDesc;
    private String severityLevel;
    private ArrayList<String> sinks;

    public SinkRule(String sinkName, String sinkDesc, String severityLevel, ArrayList<String> sinks){
        this.sinkName = sinkName;
        this.sinkDesc = sinkDesc;
        this.severityLevel = severityLevel;
        this.sinks = sinks;
    }

    public String toString(){
        return sinkName + " - " + sinkDesc + " - " + severityLevel + " : " + sinks;
    }

}
