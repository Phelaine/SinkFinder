package com.mediocrity.util;

import java.util.Arrays;

/**
 * @Project: sinkfinder
 * @ClassName: TextTable
 * @Author: TheKingOfDuck
 * @Date:2023/10/11 5:53 下午
 * Github:https://github.com/TheKingOfDuck
 * When I wirting my code, only God and I know what it does. After a while, only God knows.
 */
public class TextTable {

    public final static String SEP = "\n";

    private String[] header;

    private String[][] rows;

    public TextTable(String[] header, String[][] rows) {
        this.header = header;
        this.rows = rows;
    }

    public String[] getHeader() {
        return header;
    }

    public void setHeader(String[] header) {
        this.header = header;
    }

    public String[][] getRows() {
        return rows;
    }

    public void setRows(String[][] rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();

        // calc length of every column
        int[] lens = calcLenOfColumn(header, rows);

        // build split line
        String splitLine = buildSplitLine(lens);

        // print split line
        append(buff, splitLine);

        // print header
        append(buff, buildArray(lens, header));

        // print split line
        append(buff, splitLine);

        for (String[] row : rows) {
            append(buff, buildArray(lens, row));
            append(buff, splitLine);
        }
        return buff.toString();
    }

    private void append(StringBuffer buff, String line) {
        buff.append(line).append(SEP);
    }

    private String buildSplitLine(int[] lens) {
        StringBuilder buff = new StringBuilder();
        buff.append("+");
        for (int i = 0; i < lens.length; i++) {
            buff.append(buildStrWithFixCharAndLength('-', lens[i] + 2));
            buff.append("+");
        }
        return buff.toString();
    }

    private String buildArray(int[] lens, String[] row) {
        StringBuffer buff = new StringBuffer();
        buff.append("|");
        for (int i = 0; i < header.length; i++) {
            String s = null;
            if (i < row.length) {
                s = row[i];
            }
            buff.append(" ");
            buff.append(polishing(s, lens[i]));
            buff.append(" |");
        }
        return buff.toString();
    }


    private int[] calcLenOfColumn(String[] header, String[][] rows) {
        int[] lens = new int[header.length];
        Arrays.fill(lens, 0);
        for (int i = 0; i < header.length; i++) {
            lens[i] = Math.max(stringLen(header[i]), lens[i]);
        }
        for (String[] row : rows) {
            for (int i = 0; i < header.length; i++) {
                if (i < row.length) {
                    lens[i] = Math.max(stringLen(row[i]), lens[i]);
                }
            }
        }
        return lens;
    }

    /**
     * calc string length
     *
     * @param s
     * @return
     */
    public static int stringLen(String s) {
        if (s == null) {
            return 0;
        }
        int len = 0;
        String ch = "[\u0391-\uFFE5]";
        String ch2 = "[\u00B7]";
        for (int i = 0; i < s.length(); i++) {
            String tmp = s.substring(i, i + 1);
            if (tmp.matches(ch) || tmp.matches(ch2)) {
                len += 2;
            } else {
                len += 1;
            }
        }
        return len;
    }

    private String buildStrWithFixCharAndLength(char c, int len) {
        char[] cs = new char[len];
        Arrays.fill(cs, c);
        return new String(cs);
    }

    /**
     * polish blank after string with max length
     *
     * @param str
     * @param destLength
     * @return
     */
    private String polishing(String str, int destLength) {
        if (str == null) {
            return buildStrWithFixCharAndLength(' ', destLength);
        }
        int len = stringLen(str);
        if (len < destLength) {
            int dif = destLength - len;
            char[] cs = new char[dif];
            Arrays.fill(cs, ' ');
            return str + new String(cs);
        } else if (len == destLength) {
            return str;
        } else {
            StringBuffer sb = new StringBuffer();
            char[] cs = str.toCharArray();
            for (int i = 0; i < cs.length; i++) {
                if (stringLen(sb.toString() + cs[i]) >= destLength - 3) {
                    break;
                }
                sb.append(cs[i]);
            }
            String s = sb + "...";
            len = stringLen(s);
            if (len < destLength) {
                int dif = destLength - len;
                cs = new char[dif];
                Arrays.fill(cs, ' ');
                return s + new String(cs);
            }
            return s;
        }
    }
}
