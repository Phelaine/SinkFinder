package com.mediocrity.core;

import com.mediocrity.entity.Rules;
import com.mediocrity.entity.SinkResult;
import com.mediocrity.entity.SinkRule;
import com.mediocrity.model.ClassInfo;
import com.mediocrity.model.ClassRepo;
import com.mediocrity.entity.WrapperNode;
import com.mediocrity.util.ASMUtil;
import com.mediocrity.util.RuleUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class InsnAnalysis {

    private static String clsName;
    private static String methodName;
    private static String methodInsnOwner;
    private static String methodInsnName;
    private static String methodSink;
    private static String methodSource;
//    private static List<ParameterNode> paramNodesSink;
//    private static List<ParameterNode> paramNodesSource;

    private static ArrayList<String> result = new ArrayList<>();
    private static Boolean isRecord = true;
    private static ArrayList<SinkResult> finalResult = new ArrayList<>();

    public static ArrayList<SinkResult> run(Rules ruls, int depth) {

        int count;

        if (!ClassRepo.getInstance().listAll().isEmpty()) {
            for (SinkRule sinkRule : ruls.getSinkRules()) {
                for (String sink : sinkRule.getSinks()) {
                    log.info(sink + "规则开始执行...");
                    result.clear();
                    count = 1;
                    result.add(sink);
                    findSource(sink, sinkRule, depth, ruls, count, result);
                }
            }
        }
        return finalResult;
    }

    public static ArrayList<SinkResult> runSink(Rules ruls, String sink, int depth) {

        int count = 1;

        if (!ClassRepo.getInstance().listAll().isEmpty()) {
            log.info(sink + "规则开始执行...");
            result.add(sink);
            findSource(sink, new SinkRule(), depth, ruls, count, result);
        }

        return finalResult;
    }

    /**
     * 通过 sink 在所有调用关系中找出调用者
     *
     * @param sink   目标调用规则
     * @param depth  最大递归深度
     * @param ruls   规则限制
     * @param count  递归深度
     * @param result 结果
     * @return
     */
    private static void findSource(String sink, SinkRule sinkRule, int depth, Rules ruls,
                                   int count, ArrayList<String> result) {

        for (ClassInfo classInfo : ClassRepo.getInstance().listAll()) {
            clsName = classInfo.getClassNode().name.replaceAll("/", "\\.");

            if (RuleUtil.isIncluded(clsName, ruls.getClassInclusions()) && !RuleUtil.isExcluded(clsName, ruls.getClassExclusions())) {
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfo.getClassNode())) {

                    methodName = wrapper.getMethodNode().name;
//                    paramNodesSource = wrapper.getMethodNode().parameters;
                    methodInsnOwner = wrapper.getMethodInsnNode().owner.replaceAll("/", "\\.");
                    methodInsnName = wrapper.getMethodInsnNode().name;
                    methodSink = methodInsnOwner + ":" + methodInsnName + wrapper.getMethodInsnNode().desc;
                    methodSource = classInfo.getJarName() + "#" + clsName + ":" + methodName;

                    if (count == 1) {
                        methodSink = methodInsnOwner + ":" + methodInsnName;
//                        if (paramNodesSource == null){
//                            paramNodesSource = new ArrayList<>();
//                        }
                    }

                    // 如果根据规则找到的话
//                    if (sink.contains(methodSink) && paramNodesSource != null) {
                    if (sink.contains(methodSink) && !result.contains(methodSource)) {

                        result.add(methodSource);
                        if (count < depth || depth == -1) {
                            count++;
                            isRecord = true;
                            findSource(methodSource + wrapper.getMethodNode().desc, sinkRule, depth, ruls,
                                    count, result);
                        }
                        if (isRecord) {
//                            sinkLog(String.format(format, "深度" + count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), String.join("\t", result)));

                            if (sinkRule.getSinkName().isEmpty()) {
                                SinkResult s = new SinkResult(count, "CUSTOM",
                                        "CUSTOM", (ArrayList<String>)result.clone());
                                finalResult.add(s);
                            } else {
                                SinkResult s = new SinkResult(count, sinkRule.getSinkName(),
                                        sinkRule.getSeverityLevel(), (ArrayList<String>)result.clone());
                                finalResult.add(s);
                            }
                        }
                        count--;
                        isRecord = false;
                        result.remove(result.size() - 1);
                    }
                }
            }
        }
    }

}
