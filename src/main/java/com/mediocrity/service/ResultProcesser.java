package com.mediocrity.service;

import com.mediocrity.entity.SinkResult;
import com.mediocrity.entity.SinkRule;
import com.mediocrity.util.FileUtil;
import com.mediocrity.util.ThreadType;
import com.mediocrity.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.mediocrity.SinkFinder.*;

public class ResultProcesser {
    private static final Logger logger = LoggerFactory.getLogger(ResultProcesser.class);

    public static Future<Boolean> resultProcessTask;
    public static List<Future<Boolean>> resultProcessTasks = new ArrayList<>();
    public static String filterResultLog;
    public static String otherResultLog;
    public static String llmResultLog;

    public static final HashSet<SinkResult> otherResult = new HashSet<>();
    public static final HashSet<SinkResult> filterResult = new HashSet<>();
    public static final HashSet<SinkResult> llmResult = new HashSet<>();

    private static final Date day = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");

    static {
        if (CUSTOM_SINK_RULE.isEmpty()) {
            String[] f = TARGET_PATH.split("/");
            if (f.length == 1)
                f = TARGET_PATH.split("\\\\");
            String s = f[f.length-1].replace(".", "_").replace(":","") + ".log";
            otherResultLog = "logs" + File.separator + sdf.format(day) + "_" + "OtherRisk_" + s;
            filterResultLog = "logs" + File.separator + sdf.format(day) + "_" + "Risk_" + s;
            if (LLM_ENABLE) llmResultLog = "logs" + File.separator + sdf.format(day) + "_" + "HighLLMRisk_" + s;
        } else{
            String[] f = CUSTOM_SINK_RULE.split(":")[0].split("\\.");
            String s = f[f.length-1] + "_" + CUSTOM_SINK_RULE.split(":")[1].replace(".", "_").replace("(","_").replace(")","_").replace(";","_") + ".log";
            otherResultLog = "logs" + File.separator + sdf.format(day) + "_" + "OtherRisk_" + s;
            filterResultLog = "logs" + File.separator + sdf.format(day) + "_" + "Risk_" + s;
            if (LLM_ENABLE) llmResultLog = "logs" + File.separator + sdf.format(day) + "_" + "HighLLMRisk_" + s;
        }

        FileUtil.writeLog(ruls.toString() + "\n${PATH} 路径：" + TARGET_PATH + "\n\n", otherResultLog);
        FileUtil.writeLog(ruls.toString() + "\n${PATH} 路径：" + TARGET_PATH + "\n\n", filterResultLog);
        if (LLM_ENABLE) FileUtil.writeLog(ruls.toString() + "\n${PATH} 路径：" + TARGET_PATH + "\n\n", llmResultLog);

    }

    public static void process(SinkResult s) {
        resultProcessTask = ThreadPoolUtil.submit(() ->{
            String sink = s.getInvokeDetail().get(0);
            if (!CUSTOM_SINK_RULE.isEmpty() && ruls.getSinkRules().size() == 1) sink = sink + ",";
            // 补充结果分类、等级
            for (SinkRule sr : ruls.getSinkRules()) {
                if (sr.getSinks().contains(sink)) {
                    s.setSinkCata(sr.getSinkName());
                    s.setSinkDesc(sr.getSinkDesc());
                    s.setSinkLevel(sr.getSeverityLevel());
                    break;
                }
            }

            if (s.getIsFilter()) {
                filterResult.add(s);
                FileUtil.writeLog(s.toString(false) + "\n", filterResultLog);
                logger.debug(s.toString(true));

                if (LLM_ENABLE) {
                    // done 接入 LLM 识别数据联通性
                    int score = LLMAnalysis.llmProcess(s.getInvokeDetail(), s, ruls.getDashscopeApiKey());
                    if ( score > 7 ){
                        // 7分以上的进行记录
                        llmResult.add(s);
                        FileUtil.writeLog(s.toString(false) + "\n", llmResultLog);
                    }
                }
            } else {
                otherResult.add(s);
                FileUtil.writeLog(s.toString(false) + "\n", otherResultLog);
            }

            return true;
        }, ThreadType.ResultProcess);

        resultProcessTasks.add(resultProcessTask);
    }

}
