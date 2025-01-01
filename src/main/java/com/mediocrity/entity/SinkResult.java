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
    public String sinkCata;
    public String sinkDesc;
    public String sinkLevel;
    public Boolean isFilter;
    public ArrayList<String> invokeDetail;
//    private String format = "|%1$-5s|%2$-10s|%3$-6s|%4$-1s|";

    public SinkResult(String sinkName, String sinkDesc, String severityLevel, ArrayList<String> result) {
        this.sinkCata = sinkName;
        this.sinkDesc = sinkDesc;
        this.sinkLevel = severityLevel;
        this.invokeDetail = result;
    }

    public SinkResult(Boolean isFilter, ArrayList<String> result) {
        this.isFilter = isFilter;
        this.invokeDetail = result;
    }

    public String toString(boolean isPrint){
        int invokeLength = this.getInvokeDetail().size();
        String[] header;
        String[][] rows;
        int j = 0;
        if (isPrint) {
            header = new String[]{"层数", "方法调用路径[Class:Method]", "Jar/War/ZIP文件路径"};
            rows = new String[invokeLength][3];
        }
        else {
            header = new String[]{"层数", "方法调用路径[Class:Method]", "Method Signature", "Jar/War/ZIP文件路径"};
            rows = new String[invokeLength][4];
        }
        for (int i = invokeLength - 1; i >= 0 ; i--) {
            String vul = invokeDetail.get(i);
            rows[j][0] = String.valueOf(j+1);

            if ( vul.indexOf('(') == -1 ) {
                rows[j][1] = vul;
                j++;
                continue;
            }

            rows[j][1] = vul.substring(vul.indexOf('#') + 1, vul.indexOf('('));
            if (!isPrint) {
                rows[j][2] = vul.substring(vul.indexOf('('));
                if ( vul.indexOf('#') != -1 )
                    rows[j][3] = vul.substring(0, vul.indexOf('#'));
            }
            else
                if (vul.indexOf('#') != -1)
                    rows[j][2] = vul.substring(0, vul.indexOf('#'));

            j++;
        }
        return sinkCata + " - " + sinkLevel + " - " + invokeDetail.get(0) +  "\n" + new TextTable(header, rows);
    }

    public java.lang.String toString(Boolean isLLmInput, Boolean llm){
        int invokeLength = this.getInvokeDetail().size();
        StringBuilder header = new StringBuilder();
        int j = 0;

        if (isLLmInput){
            header.append("|层数|方法调用路径[Class:Method(Method Signature)]|Jar/War/ZIP文件路径|\n");
        }else {
            header.append("|层数|方法调用路径[Class:Method]|Method Signature|Jar/War/ZIP文件路径|\n");
            header.append("|:--|:--|:--|:--|\n");
        }

        for (int i = invokeLength - 1 ; i >= 0 ; i--) {
            String vul = invokeDetail.get(i);
            header.append("|第").append(j + 1).append("层");

            if ( vul.indexOf('(') == -1 ) {
                header.append("|").append(vul).append("|");
                j++;
                continue;
            }

            if (!isLLmInput) {
                header.append("|").append(vul, vul.indexOf('#') + 1, vul.indexOf('('));

                header.append("|").append(vul.substring(vul.indexOf('(')));
                if (vul.indexOf('#') != -1)
                    header.append("|").append(vul, 0, vul.indexOf('#')).append("|\n");
                else header.append("|\n");
            }else{
                header.append("|").append(vul, vul.indexOf('#') + 1, vul.indexOf(')')+1);
                if (vul.indexOf('#') != -1)
                    header.append("|").append(vul, 0, vul.indexOf('#')).append("|\n");
                else header.append("|\n");
            }

            j++;
        }

        return "## " + sinkCata + " - " + sinkDesc + " - " + sinkLevel + " - " + invokeDetail.get(0) +  "\n\n" + header +"\n";
    }
}
