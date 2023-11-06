package com.mediocrity.entity;

import com.mediocrity.util.TextTable;
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
//    private String format = "|%1$-5s|%2$-10s|%3$-6s|%4$-1s|";

    public SinkResult(int count, String sinkName, String severityLevel, ArrayList<String> result) {
        this.invokeLength = count;
        this.sinkCata = sinkName;
        this.sinkLevel = severityLevel;
        this.invokeDetail = result;
    }

//    public String toString(){
//        return String.format(format, invokeLength, sinkCata, sinkLevel,
//                String.join("\t", invokeDetail));
//    }

    public String toString(boolean isPrint){
        String[] header;
        String[][] rows;
        if (isPrint) {
            header = new String[]{"Depth", "Source", "Jar/Class"};
            rows = new String[getInvokeLength()][3];
        }
        else {
            header = new String[]{"Depth", "Source", "Method Signature", "Jar/Class"};
            rows = new String[getInvokeLength()][4];
        }
        for (int i = 0; i < getInvokeLength(); i++) {
            String vul = getInvokeDetail().get(i+1);
            rows[i][0] = String.valueOf(i+1);
            rows[i][1] = vul.substring(vul.indexOf('#') + 1, vul.indexOf('('));
            if (!isPrint) rows[i][2] = vul.substring(vul.indexOf('('));
            else rows[i][2] = vul.substring(0, vul.indexOf('#'));
            if (!isPrint) rows[i][3] = vul.substring(0, vul.indexOf('#'));
        }
        return new TextTable(header, rows).toString();
    }
}
