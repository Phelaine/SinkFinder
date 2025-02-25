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
//    private Boolean controllerFilter;

    public String toString(){
        return "递归查找深度：" + depth + " \npath黑名单：" + pathExclusions + " \njar包白名单：" + jarNameInclusions + " \njar包黑名单：" + jarNameExclusions + " \nclass白名单：" + classInclusions + " \nclass黑名单：" + classExclusions + " \nsink规则：" + sinkRules.toString();
//        + " \n是否对Controller进行过滤：" + controllerFilter;
    }
}
