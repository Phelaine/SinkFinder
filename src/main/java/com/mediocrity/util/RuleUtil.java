package com.mediocrity.util;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/15
 */
public class RuleUtil {

//    public static Boolean allInclude;

    /**
     * 根据规则判断是否排除
     *
     * @param excludeKey The String to check if is excluded.
     * @param rules
     * @return True or False.
     */
    public static boolean isExcluded(String excludeKey, ArrayList<String> rules) {

        // org/springframework/boot 转换为 org.springframework.boot
//        excludeKey = excludeKey.replaceAll("/", "\\.");

        for (String exclusion : rules) {
            if (excludeKey.contains(exclusion)) {
                return true;
            }
        }

        return false;
    }


    /**
     * 根据规则判断是否包含
     *
     * @param includeKey The String to check if is included.
     * @param rules
     * @return True or False.
     */
    public static boolean isIncluded(String includeKey, ArrayList<String> rules) {

//        allInclude = false;
        // org/springframework/boot 转换为 org.springframework.boot
//        includeKey = includeKey.replaceAll("/", "\\.");

        for (String inclusion : rules) {
//            if (inclusion.equals("*")){
//                allInclude = true;
//                return true;
//            } else
            if (includeKey.contains(inclusion) || inclusion.equals("*")) {
                return true;
            }
        }

        return false;
    }

}
