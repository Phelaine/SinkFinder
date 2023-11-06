package com.mediocrity.entity;

import lombok.Data;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Data
public class Rules {
    private int depth;
    private ArrayList<String> pathExclusions;
    private ArrayList<String> jarNameInclusions;
    private ArrayList<String> jarNameExclusions;
    private ArrayList<String> classInclusions;
    private ArrayList<String> classExclusions;
    private ArrayList<SinkRule> sinkRules;

    public String toString(){
        return "遍历深度为：" + depth + "  路径黑名单：" + pathExclusions + "  jar包白名单：" + jarNameInclusions + "  jar包黑名单：" + jarNameExclusions + "  class白名单：" + classInclusions + "  class黑名单：" + classExclusions + "  sink规则条数：" + sinkRules.size();
    }
}
