package com.mediocrity.entity;

import lombok.Data;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Data
public class Rules {
    private ArrayList<String> pathExclusions;
    private ArrayList<String> jarNameInclusions;
    private ArrayList<String> jarNameExclusions;
    private ArrayList<String> classInclusions;
    private ArrayList<String> classExclusions;
    private ArrayList<SinkRule> sinkRules;
}
