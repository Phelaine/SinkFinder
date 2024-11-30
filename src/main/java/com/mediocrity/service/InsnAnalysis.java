package com.mediocrity.service;

import com.mediocrity.entity.*;
import com.mediocrity.model.ClassInfo;
import com.mediocrity.model.ClassZoom;
import com.mediocrity.model.NodeInfo;
import com.mediocrity.util.ASMInsnsUtil;
import com.mediocrity.util.RuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.mediocrity.SinkFinder.LLM_ENABLE;

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

    private static final HashSet<SinkResult> resultsZoom = new HashSet<>();

    public static final HashSet<SinkResult> filterResult = new HashSet<>();

    private static Boolean isRecord = true;

    private static Boolean isFilter = true;

    private static int count = 0;

    public static HashSet<SinkResult> run(Rules ruls) {

        if (!ClassZoom.classes.entrySet().isEmpty()) {
            for (SinkRule sinkRule : ruls.getSinkRules()) {
                for (String sink : sinkRule.getSinks()) {
                    logger.info(sink + "规则开始执行...");
                    result.clear();
                    result.add(sink);
                    findSource(sink, sinkRule, ruls);
                }
            }
        }
        return resultsZoom;
    }

    /**
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param ruls  规则限制
     * @param sinkRule  sink规则信息
     */
    private static void findSource(String sink, SinkRule sinkRule, Rules ruls) {

        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassZoom.classes.entrySet()) {

            if (RuleUtil.isIncluded(classInfoEntry.getKey(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfoEntry.getKey(), ruls.getClassExclusions())) {
                for (NodeInfo nodeInfo : ASMInsnsUtil.getMethodInsnsInClass(classInfoEntry.getValue().getClassNode())) {
                    isFind = false;

                    source = classInfoEntry.getKey() + ":" + nodeInfo.getMethodNode().name + nodeInfo.getMethodNode().desc;

                    // setStatus(Ljava/lang/String;)V
                    insnMethodName = nodeInfo.getMethodInsnNode().name;
                    insnMethodDesc = nodeInfo.getMethodInsnNode().desc;
                    String insnMethodOwner = nodeInfo.getMethodInsnNode().owner;

                    sinkClass = sink.split(":")[0];
                    sinkMethodDesc = sink.split(":")[1];

                    // done 如果参数为空则直接跳过查找
                    if (!(nodeInfo.getMethodNode().name.equals("call") || nodeInfo.getMethodNode().name.equals("run")) && nodeInfo.getMethodNode().desc.startsWith("()")) continue;

                    if (sink.split("\\(").length == 1) {
                        if (!sinkMethodDesc.equals(insnMethodName)) continue;
                    } else {
                        if (!sinkMethodDesc.equals(insnMethodName + insnMethodDesc)) continue;
                    }

                    if (sinkClass.equals(insnMethodOwner.replace("/", "."))) isFind = true;

                    // 如果没找到直接调用关系，分别看是否存在子类调用或接口类调用
                    // todo 动态调用指令分析
                    if ( !isFind ){
                        ClassInfo info = ClassZoom.classes.get(sinkClass);
                        if (info == null) nullOrNoSubClasses.add(sinkClass);
                        else {
                            if (!nullOrNoSubClasses.contains(sinkClass)) {
                                subClasses.clear();
                                // 找子类，否则加入空缓存中
                                if (info.getSubClasses().isEmpty()) {
                                    findSubClasses(sinkClass);
                                    if (subClasses.isEmpty()) nullOrNoSubClasses.add(sinkClass);
                                    else ClassZoom.classes.get(sinkClass).setSubClasses((ArrayList<String>) subClasses.clone());
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

                        // done 入口判断：
                        //  webx 入口实现 com.alibaba.citrus.service.pipeline.Valve 的接口；
                        //  servlet 入口继承自 javax.servlet.Servlet 类；
                        //  方法为 doGet/doPost
                        //  Filter/Interceptor
                        //  spring入口存在 Controller/RestController 注解
                        isFilter = classInfoEntry.getValue().getClassNode().superName.contains("Servlet") || classInfoEntry.getValue().getClassNode().superName.contains("Valve")
//                                || classInfoEntry.getValue().getClassNode().methods.stream().anyMatch(methodNode -> methodNode.name.equals("doGet") || methodNode.name.equals("doPost"))
                                || classInfoEntry.getValue().getClassNode().interfaces.stream().anyMatch( iface -> iface.contains("Filter") || iface.contains("Interceptor"))
                                || (classInfoEntry.getValue().getClassNode().visibleAnnotations != null && classInfoEntry.getValue().getClassNode().visibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.contains("Controller")));

                        // 如果之前已经找过source || 为入口 || 遍历深度 则不再往下继续遍历
                        if ( !isFilter && !pathZoom.contains(source) && count < ruls.getDepth() ) {
                            // todo 接口如果存在继承关系不会在当前类的interfaces中，待整理类所实现的接口
                            ClassInfo i = ClassZoom.classes.get(insnMethodOwner.replace("/", "."));
                            if (i != null && !i.getClassNode().interfaces.isEmpty()){
//                                System.out.println(i.getClassNode().name + Arrays.toString(i.getClassNode().interfaces.toArray()));
                                // done 如果当前调用类实现的接口中存在 Callable/Runnable 接口，且调用了run()/call()，需要变换为找该类的有参<init>方法
                                if((i.getClassNode().interfaces.contains("java/lang/Runnable") && nodeInfo.getMethodNode().name.equals("run")) || (i.getClassNode().interfaces.contains("java/util/concurrent/Callable") && nodeInfo.getMethodNode().name.equals("call")))
                                    source = classInfoEntry.getKey() + ":<init>";
                            }

                            findSource(source, sinkRule, ruls);
                        }

                        if ( isRecord ) {

                            // source如果在filterResult中，拼接路径
                            if( pathZoom.contains(source) ){
                                for (SinkResult re : filterResult){
                                    int i = re.getInvokeDetail().indexOf(source);
                                    if ( i != -1 ) {
                                        for (int j = i+1; j < re.getInvokeDetail().size(); j++){
                                            result.add(re.getInvokeDetail().get(j));
                                        }
//                                        System.out.println("&&&&&&" + Arrays.toString(result.toArray()));
                                        isFilter = true;
                                        break;
                                    }
                                }
                            }

                            SinkResult s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSinkDesc(), sinkRule.getSeverityLevel(),
                                    (ArrayList<String>) result.clone());

                            resultsZoom.add(s);
                            if ( isFilter && !filterResult.contains(s) ){
                                filterResult.add(s);

                                if (LLM_ENABLE){
                                    // done 接入 LLM 识别数据联通性
                                    LLMAnalysis.llmprocess(result, s, ruls.getDashscopeApiKey());
                                    logger.debug(s.toString(true));
                                }
                            }
                        }

                        // 当 source 已经完成遍历时加入 pathZoom , 如果达到一定层级也没找到那也加入 - - ，可能对这部分造成遗漏，推荐多层数
                        // if (count < ruls.getDepth()) pathZoom.add(source);
                        pathZoom.add(source);

                        count--;
                        result.remove(result.size() - 1);
                        isRecord = false;
                        isFilter = false;
                    }
                }
            }
        }
    }

    public static void findSubClasses(String superName){
        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassZoom.classes.entrySet()){
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
