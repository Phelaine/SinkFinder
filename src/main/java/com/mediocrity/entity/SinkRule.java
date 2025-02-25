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
    private String severityLevel;
    private ArrayList<String> sinks;

    public SinkRule(){}

    public SinkRule(String sinkName, String severityLevel, ArrayList<String> sinks){
        this.sinkName = sinkName;
        this.severityLevel = severityLevel;
        this.sinks = sinks;
    }

    public String toString(){
        return sinkName + " - " + severityLevel + " : " + sinks;
    }

}
