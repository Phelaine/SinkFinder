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
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

    private static final ArrayList<String> pathZoom = new ArrayList<>();

    private static final HashSet<SinkResult> pathResult = new HashSet<>();

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
        return pathResult;
    }

    /**
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param ruls  规则限制
     * @param sinkRule  sink规则信息
     */
    private static void findSource(String sink, SinkRule sinkRule, Rules ruls) {

        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassRepo.classes.entrySet()) {

            if (RuleUtil.isIncluded(classInfoEntry.getKey(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfoEntry.getKey(), ruls.getClassExclusions())) {
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfoEntry.getValue().getClassNode())) {
                    isFind = false;

                    source = classInfoEntry.getKey() + ":" + wrapper.getMethodNode().name + wrapper.getMethodNode().desc;

                    // setStatus(Ljava/lang/String;)V
                    insnMethodName = wrapper.getMethodInsnNode().name;
                    insnMethodDesc = wrapper.getMethodInsnNode().desc;
                    String insnMethodOwner = wrapper.getMethodInsnNode().owner;

                    sinkClass = sink.split(":")[0];
                    sinkMethodDesc = sink.split(":")[1];

                    // todo 如果参数为空则跳过
                    if (!(wrapper.getMethodNode().name.equals("call") || wrapper.getMethodNode().name.equals("run")) && wrapper.getMethodNode().desc.startsWith("()")) continue;

                    if (sink.split("\\(").length == 1) {
                        if (!sinkMethodDesc.equals(insnMethodName)) continue;
                    } else {
                        if (!sinkMethodDesc.equals(insnMethodName + insnMethodDesc)) continue;
                    }

                    if (sinkClass.equals(insnMethodOwner.replace("/", "."))) isFind = true;

                    // 如果没找到直接调用关系，分别看是否存在子类调用或接口类调用
                    if ( !isFind ){
                        ClassInfo info = ClassRepo.classes.get(sinkClass);
                        if (info == null) nullOrNoSubClasses.add(sinkClass);
                        else {
                            if (!nullOrNoSubClasses.contains(sinkClass)) {
                                subClasses.clear();
                                // 找子类，否则加入空缓存中
                                if (info.getSubClasses().isEmpty()) {
                                    findSubClasses(sinkClass);
                                    if (subClasses.isEmpty()) nullOrNoSubClasses.add(sinkClass);
                                    else ClassRepo.classes.get(sinkClass).setSubClasses((ArrayList<String>) subClasses.clone());
                                }

                                if (info.getSubClasses().contains(insnMethodOwner.replace("/", "."))) isFind = true;
                            }

                            if ( !isFind ){
                                // todo 接口类存在继承关系时找到所有父类
                                if (info.getClassNode().interfaces.contains(insnMethodOwner)) isFind = true;
                            }
                        }
                    }

                    //${PATH}/applications/idp4/apache-tomcat-8.5.55/webapps/ROOT/WEB-INF/classes/com/aa/bff/upgrade/AutoUpgradeExecutor.class#com.aa.bff.upgrade.AutoUpgradeExecutor:currentVersion()Lcom/aa/bff/upgrade/Version;
                    sourceInfo = classInfoEntry.getValue().getJarName() + "#" + source;

                    // 如果根据规则找到且不是自循环的情况
                    if ( isFind && !result.contains(sourceInfo) ) {

                        result.add(sourceInfo);
                        count++;
                        isRecord = true;

                        //如果之前已经找过该路径的话就不再往下遍历
                        if (count < ruls.getDepth() && !pathZoom.contains(source)) {
                            // todo 接口如果存在继承关系会在当前的interfaces中吗 - 不会 坑
                            ClassInfo i = ClassRepo.classes.get(insnMethodOwner.replace("/", "."));
                            if (i != null && !i.getClassNode().interfaces.isEmpty()){
//                                System.out.println(i.getClassNode().name + Arrays.toString(i.getClassNode().interfaces.toArray()));
                                // todo 如果当前调用类实现的接口中存在 Callable/Runnable 接口，且调用了run()/call()，需要变换为找该类的有参<init>方法
                                if((i.getClassNode().interfaces.contains("java/lang/Runnable") && wrapper.getMethodNode().name.equals("run")) || (i.getClassNode().interfaces.contains("java/util/concurrent/Callable") && wrapper.getMethodNode().name.equals("call")))
                                    source = classInfoEntry.getKey() + ":<init>";
                            }

                            findSource(source, sinkRule, ruls);
                        }
                        if ( isRecord ) {
                            SinkResult s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(),
                                    (ArrayList<String>) result.clone());
                            pathResult.add(s);

                            // todo webx 入口实现 com.alibaba.citrus.service.pipeline.Valve 的接口；
                            //  servlet 入口继承自 javax.servlet.Servlet 类；
                            //  spring入口存在 Controller/RestController 注解
//                            String lastNodeClass = StringUtils.substringBetween(result.get(result.size() - 1), "#", ":");
//                            ClassInfo i = ClassRepo.classes.get(lastNodeClass);

                            if (classInfoEntry.getValue().getClassNode().superName.contains("Servlet")  || classInfoEntry.getValue().getClassNode().superName.contains("Valve")
                                    || classInfoEntry.getValue().getClassNode().methods.stream().anyMatch(methodNode -> methodNode.name.equals("doGet") || methodNode.name.equals("doPost") )){
                                logger.debug("\n" + s.invokeLength + " - " + s.toString(true));
                            } else if (classInfoEntry.getValue().getClassNode().visibleAnnotations != null){
                                if (classInfoEntry.getValue().getClassNode().visibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.contains("Controller"))){
                                    logger.debug("\n" + s.invokeLength + " - " + s.toString(true));
                                }
                            }
                        }

                        pathZoom.add(source);
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
                if (classInfoEntry.getValue().getClassNode().superName.replace("/",".").equals(superName)){
                    subClasses.add(classInfoEntry.getKey());
                    findSubClasses(classInfoEntry.getKey());
                }
            }catch (Exception e){
                // System.out.println(e.getMessage());
            }
        }
    }
}
