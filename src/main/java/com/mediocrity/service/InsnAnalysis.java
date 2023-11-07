package com.mediocrity.service;

import com.mediocrity.entity.Rules;
import com.mediocrity.entity.SinkResult;
import com.mediocrity.entity.SinkRule;
import com.mediocrity.model.ClassInfo;
import com.mediocrity.model.ClassRepo;
import com.mediocrity.entity.WrapperNode;
import com.mediocrity.util.ASMUtil;
import com.mediocrity.util.RuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class InsnAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(InsnAnalysis.class);

    private static final HashSet<String> nullOrNoSubClasses = new HashSet<>();
    private static String insnMethodName;
    private static String insnMethodDesc;
    private static String sourceInfo;
    private static String source;

    private static String sinkClass;
    private static String sinkMethodDesc;

    private static final ArrayList<String> subClasses = new ArrayList<>();

    private static Boolean isFind;

    private static final ArrayList<String> result = new ArrayList<>();
    private static final HashSet<SinkResult> finalResult = new HashSet<>();

    private static Boolean isRecord = true;

    private static int count = 0;

    public static HashSet<SinkResult> run(Rules ruls) {

        if (!ClassRepo.classes.entrySet().isEmpty()) {
            for (SinkRule sinkRule : ruls.getSinkRules()) {
                for (String sink : sinkRule.getSinks()) {
                    logger.info(sink + "规则开始执行...");
                    result.clear();
                    result.add(sink);
                    findSource(sink, sinkRule, ruls);
                }
            }
        }
        return finalResult;
    }

    /**
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param ruls  规则限制
     * @param sinkRule  sink规则信息
     * @return
     */
    private static void findSource(String sink, SinkRule sinkRule, Rules ruls) {

        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassRepo.classes.entrySet()) {

            if (RuleUtil.isIncluded(classInfoEntry.getKey(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfoEntry.getKey(), ruls.getClassExclusions())) {
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfoEntry.getValue().getClassNode())) {
                    isFind = false;

                    source = classInfoEntry.getKey() + ":" + wrapper.getMethodNode().name + wrapper.getMethodNode().desc;

                    insnMethodName = wrapper.getMethodInsnNode().name;
                    insnMethodDesc = wrapper.getMethodInsnNode().desc;
                    String insnMethodOwner = wrapper.getMethodInsnNode().owner;

                    sinkClass = sink.split(":")[0];
                    sinkMethodDesc = sink.split(":")[1];

                    if (sink.split("\\(").length == 1) {
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

                                if (info.getSubClasses().contains(insnMethodOwner.replaceAll("/", "\\."))) isFind = true;
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
                        if (count < ruls.getDepth()) {
                            findSource(source, sinkRule, ruls);
                        }
                        if ( isRecord ) {
                            SinkResult s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(),
                                    (ArrayList<String>) result.clone());
                            finalResult.add(s);
                            logger.debug("\n" + s.invokeLength + " - " + s.sinkCata + " - " + s.sinkLevel + " - " + s.getInvokeDetail().get(0) + "\n" + s.toString(true));
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
            try {
                String s = classInfoEntry.getValue().getClassNode().superName.replaceAll("/","\\.");
                if (s.contains(superName)){
                    subClasses.add(classInfoEntry.getKey());
                    findSubClasses(classInfoEntry.getKey());
                }
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
    }
}
