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
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class InsnAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(InsnAnalysis.class);

    private static HashSet<String> nullOrNoSubClasses = new HashSet<>();
    private static String insnMethodOwner;
    private static String insnMethodName;
    private static String insnMethodDesc;
    private static String sourceInfo;
    private static String source;

    private static String sinkClass;
    private static String sinkMethodDesc;

    private static ArrayList<String> subClasses = new ArrayList<>();

    private static Boolean isFind;

    private static ArrayList<String> result = new ArrayList<>();
    private static HashSet<SinkResult> finalResult = new HashSet<>();

    private static Boolean isRecord = true;

    private static int count = 0;

    public static HashSet<SinkResult> run(Rules ruls, int depth) {

//        int count;

        if (!ClassRepo.classes.entrySet().isEmpty()) {
            for (SinkRule sinkRule : ruls.getSinkRules()) {
                for (String sink : sinkRule.getSinks()) {
                    logger.info(sink + "规则开始执行...");
                    result.clear();
//                    count = 0;
                    result.add(sink);
                    findSource(sink, sinkRule, depth, ruls);
                }
            }
        }
        return finalResult;
    }

    public static HashSet<SinkResult> runSink(Rules ruls, String sink, int depth) {

//        int count = 0;

        if (!ClassRepo.classes.entrySet().isEmpty()) {
            logger.info(sink + "规则开始执行...");
            result.add(sink);
            findSource(sink, new SinkRule(), depth, ruls);
        }

        return finalResult;
    }

    /**
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param depth 最大递归深度
     * @param ruls  规则限制
//     * @param count 递归深度
     * @return
     */
    private static void findSource(String sink, SinkRule sinkRule, int depth, Rules ruls) {

        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassRepo.classes.entrySet()) {

            if (RuleUtil.isIncluded(classInfoEntry.getKey(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfoEntry.getKey(), ruls.getClassExclusions())) {
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfoEntry.getValue().getClassNode())) {
                    isFind = false;

                    source = classInfoEntry.getKey() + ":" + wrapper.getMethodNode().name;

                    insnMethodName = wrapper.getMethodInsnNode().name;
                    insnMethodDesc = wrapper.getMethodInsnNode().desc;
                    insnMethodOwner = wrapper.getMethodInsnNode().owner;

                    sinkClass = sink.split(":")[0];
                    sinkMethodDesc = sink.split(":")[1];

                    if (count == 0) {
                        if (!sinkMethodDesc.equals(insnMethodName)) continue;
                    } else {
                        if (!sinkMethodDesc.equals(insnMethodName + insnMethodDesc)) continue;
                    }

                    if (sinkClass.equals(insnMethodOwner.replaceAll("/", "\\."))) isFind = true;

                    // 如果没找到直接调用关系，分别去看是否存在子类调用或接口类调用
                    if ( !isFind ){
                        ClassInfo info = ClassRepo.classes.get(sinkClass);
                        if (info == null) nullOrNoSubClasses.add(sinkClass);
                        else {
                            if (!nullOrNoSubClasses.contains(sinkClass)) {
                                subClasses.clear();
                                if (info.getSubClasses().size()==0) {
                                    findSubClasses(sinkClass);
                                    if (subClasses.size() == 0) nullOrNoSubClasses.add(sinkClass);
                                    else ClassRepo.classes.get(sinkClass).setSubClasses((ArrayList<String>) subClasses.clone());
                                }

                                if (info.getSubClasses().contains(sinkClass)) isFind = true;
                            }

                            if ( !isFind ){
                                if (info.getClassNode().interfaces.contains(insnMethodOwner)) isFind = true;
                            }
                        }
                    }

                    sourceInfo = classInfoEntry.getValue().getJarName() + "#" + source;

                    // 如果根据规则找到的话
                    if ( isFind && !result.contains(sourceInfo) ) {

                        result.add(sourceInfo);
                        count++;
                        isRecord = true;
                        if (count < depth || depth == -1) {
                            findSource(source + wrapper.getMethodNode().desc, sinkRule, depth, ruls);
                        }
                        if ( isRecord ) {
                            SinkResult s;
                            if (sinkRule.getSinkName() == null) {
                                s = new SinkResult(count, "CUSTOM",
                                        "CUSTOM", (ArrayList<String>) result.clone());
                            } else {
                                s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), (ArrayList<String>) result.clone());
                            }
                            finalResult.add(s);
                            logger.debug(s.toString());
                        }
                        count--;
                        result.remove(result.size() - 1);
                        isRecord = false;
                    }
                }
            }
        }
    }

    public static void findSubClasses(String superName){
        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassRepo.classes.entrySet()){
            String s = classInfoEntry.getValue().getClassNode().superName.replaceAll("/", "\\.");
            if (s.equals("java.lang.Object")) return;
            else if (s.contains(superName)){
                subClasses.add(classInfoEntry.getKey());
                findSubClasses(classInfoEntry.getKey());
                break;
            }
        }
    }
}
