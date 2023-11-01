package com.mediocrity.util;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/15
 */
public class RuleUtil {

    /**
     * 根据规则判断是否排除
     *
     * @param excludeKey The String to check if is excluded.
     * @param rules
     * @return True or False.
     */
    public static boolean isExcluded(String excludeKey, ArrayList<String> rules) {

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

        for (String inclusion : rules) {
            if (includeKey.contains(inclusion) || inclusion.equals("*")) {
                return true;
            }
        }

        return false;
    }

}
