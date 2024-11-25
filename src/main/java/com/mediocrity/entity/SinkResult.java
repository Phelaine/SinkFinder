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
        int j = 0;
        if (isPrint) {
            header = new String[]{"层数", "路径[Class:Method]", "Jar/War/ZIP文件路径"};
            rows = new String[invokeLength + 1][3];
        }
        else {
            header = new String[]{"层数", "路径[Class:Method]", "Method Signature", "Jar/War/ZIP文件路径"};
            rows = new String[invokeLength + 1][4];
        }
        for (int i = invokeLength+1; i > 0 ; i--) {
            String vul = invokeDetail.get(i-1);
            rows[j][0] = String.valueOf(j+1);

            if ( vul.indexOf('(') == -1 ) {
                rows[j][1] = vul;
                j++;
                continue;
            }

            rows[j][1] = vul.substring(vul.indexOf('#') + 1, vul.indexOf('('));
            if (!isPrint) {
                rows[j][2] = vul.substring(vul.indexOf('('));
                rows[j][3] = vul.substring(0, vul.indexOf('#'));
            }
            else
                rows[j][2] = vul.substring(0, vul.indexOf('#'));

            j++;
        }
        return sinkCata + " - " + sinkLevel + " - " + invokeDetail.get(0) +  "\n" + new TextTable(header, rows);
    }
}
