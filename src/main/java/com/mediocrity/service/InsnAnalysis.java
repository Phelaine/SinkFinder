package com.mediocrity.service;

import com.mediocrity.entity.*;
import com.mediocrity.model.ClassInfo;
import com.mediocrity.model.ClassZoom;
import com.mediocrity.model.NodeInfo;
import com.mediocrity.util.ASMInsnsUtil;
import com.mediocrity.util.RuleUtil;
import com.mediocrity.util.ThreadType;
import com.mediocrity.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mediocrity.service.ResultProcesser.resultProcessTasks;

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
    private static String insnMethodOwner;
    private static String sourceInfo;
    private static String source;

    private static String sinkClass;
    private static String sinkMethodDesc;

    private static final ArrayList<String> subClasses = new ArrayList<>();

    private static Boolean isFind;

    private static final ArrayList<String> result = new ArrayList<>();

    private static final Set<String> pathZoom = new HashSet<>();

    public static final HashSet<String> otherResult = new HashSet<>();

    public static final HashSet<String> filterResult = new HashSet<>();

    private static Boolean isRecord = true;

    private static Boolean isFilter = true;

    private static int count = 0;

    public static void run(Rules ruls) {

        if (!ClassZoom.classes.entrySet().isEmpty()) {
            String sinks = ruls.getSinkRules().stream()
                            .flatMap(rule -> rule.getSinks().stream())
                            .collect(Collectors.joining(","));

            findSource(sinks, ruls);
        }

        for(Future<Boolean> future : resultProcessTasks) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        ThreadPoolUtil.getPoolExecutor(ThreadType.ResultProcess).shutdown();
    }

    /**
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param ruls  规则限制
     */
    private static void findSource(String sink, Rules ruls) {

        for (Map.Entry<String, ClassInfo> classInfoEntry: ClassZoom.classes.entrySet()) {

            if (RuleUtil.isIncluded(classInfoEntry.getKey(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfoEntry.getKey(), ruls.getClassExclusions())) {
                ClassInfo classInfo = classInfoEntry.getValue();

                for (NodeInfo nodeInfo : ASMInsnsUtil.getMethodInsnsInClass(classInfo.getClassNode())) {
                    isFind = false;
                    MethodNode m  = nodeInfo.getMethodNode();

                    if (!(m.name.equals("call") || m.name.equals("run")) && m.desc.startsWith("()")) continue;

                    // setStatus(Ljava/lang/String;)V  如果 sink 规则配置参数时需带全
                    insnMethodName = nodeInfo.getMethodInsnNode().name;
                    insnMethodDesc = nodeInfo.getMethodInsnNode().desc;
                    insnMethodOwner = nodeInfo.getMethodInsnNode().owner;
                    if (classInfoEntry.getKey().contains("org.mediocrity.vulbox.controller.RceController")){
                        int i = sink.length();
                    }

                    if (sink.contains(",")){
                        // 第一次匹配
                        if (!sink.contains(insnMethodOwner.replace("/", "."))) continue;
                        // done sink点为同一个类只是方法名不同推荐合并为一个sink点，注意这种类型不支持带参数，不支持"("符号，【会被识别为参数
//                        if (!sink.contains(insnMethodName)) continue;
                        String[] sum = sink.split(",");
                        for (String s : sum) {
                            String[] ss = s.split(":");
                            sinkClass = ss[0];
                            sinkMethodDesc = ss[1];
                            if (s.split("\\(").length == 1) {
                                Pattern pattern = Pattern.compile(sinkMethodDesc);
                                Matcher matcher = pattern.matcher(insnMethodName);
                                if (sinkClass.equals(insnMethodOwner.replace("/", "."))
                                        && (sinkMethodDesc.equals(insnMethodName) || matcher.find())) {
                                    isFind = true;
                                    if ( !result.isEmpty() ) result.clear();
                                    result.add(s);
                                    break;
                                }
                            }else {
                                if (sinkClass.equals(insnMethodOwner.replace("/", "."))
                                        // 截掉返回类型，匹配：setStatus(Ljava.lang.String;)
                                        && sinkMethodDesc.equals(insnMethodName + insnMethodDesc.replace("/",".")
                                        .substring(0, insnMethodDesc.indexOf(")")+1 ))) {
                                    isFind = true;
                                    if ( !result.isEmpty() )  result.clear();
                                    result.add(s);
                                    break;
                                }
                            }
                            // 找父类调用/接口调用的情况
                            dynamicLookup();
                            if ( isFind ) {
                                if ( !result.isEmpty() ) result.clear();
                                result.add(s);
                                break;
                            }
                        }
                    }else {
                        if (m.name.contains("ServiceTemplate$")){
                            int i = sink.length();
                        }
                        sinkClass = sink.split(":")[0];
                        sinkMethodDesc = sink.split(":")[1];

                        if (sink.split("\\(").length == 1) {
                            if (!sinkMethodDesc.equals(insnMethodName)) continue;
                        } else {
                            if (!sinkMethodDesc.equals(insnMethodName + insnMethodDesc)) continue;
                        }

                        if (sinkClass.equals(insnMethodOwner.replace("/", "."))) isFind = true;

                        // 如果没找到直接调用关系，分别看是否存在子类调用或接口类调用
                        // todo 动态调用指令分析
                        if ( !isFind ) {
                            dynamicLookup();
                        }
                    }

                    // 如果根据规则找到且不是自循环的情况
                    if ( isFind ) {

                        source = classInfoEntry.getKey() + ":" + m.name + m.desc;
                        //${PATH}/applications/idp4/apache-tomcat-8.5.55/webapps/ROOT/WEB-INF/classes/com/aa/bff/upgrade/AutoUpgradeExecutor.class#com.aa.bff.upgrade.AutoUpgradeExecutor:currentVersion()Lcom/aa/bff/upgrade/Version;
                        sourceInfo = classInfo.getJarName() + "#" + source;

                        if ( !result.contains(sourceInfo) ) {

                            result.add(sourceInfo);
                            count++;
                            isRecord = true;

                            // 如果之前已经找过source || 为入口 || 遍历深度 则不再往下继续遍历
                            if ( !pathZoom.contains(source) && count < ruls.getDepth()) {
                                // done 入口判断：
                                //  webx 入口实现 com.alibaba.citrus.service.pipeline.Valve 的接口；
                                //  servlet 入口继承自 javax.servlet.Servlet 类；
                                //  方法为 doGet/doPost
                                //  Filter/Interceptor
                                //  spring入口存在 Controller/RestController 注解
                                ClassNode c = classInfo.getClassNode();
                                isFilter = c.superName.contains("Servlet") || c.superName.contains("Valve")
//                                      || c.methods.stream().anyMatch(methodNode -> methodNode.name.equals("doGet") || methodNode.name.equals("doPost"))
                                        || c.interfaces.stream().anyMatch(iface -> iface.contains("Filter") || iface.contains("Interceptor"))
                                        || (c.visibleAnnotations != null && c.visibleAnnotations.stream().anyMatch(an -> an.desc.contains("Controller")));

                                if ( !isFilter ) {
                                    // todo 接口如果存在继承关系不会在当前类的interfaces中，待整理类所实现的接口
                                    ClassInfo i = ClassZoom.classes.get(classInfoEntry.getKey());
                                    if (i != null && !i.getClassNode().interfaces.isEmpty()) {
//                                  System.out.println(i.getClassNode().name + Arrays.toString(i.getClassNode().interfaces.toArray()));
                                        // done 如果当前调用类实现的接口中存在 Callable/Runnable 接口，且调用了run()/call()，需要变换为找该类的有参<init>方法
                                        // todo 如果是队列，暂时无法判断出来，需要人工排查
                                        if ((i.getClassNode().interfaces.contains("java/lang/Runnable") && m.name.equals("run"))
                                                || (i.getClassNode().interfaces.contains("java/util/concurrent/Callable") && m.name.equals("call"))){
                                            source = classInfoEntry.getKey() + ":<init>";
                                        }
                                    }
                                    if ( m.name.startsWith("lambda$") && m.name.length()!=8 ) source = classInfoEntry.getKey() + ":" + m.name.replaceAll("lambda\\$|\\$\\d+", "") + m.desc;
                                    findSource(source, ruls);
                                }
                            }

                            if (isRecord) {

                                // source如果在filterResult中，拼接路径
                                if (pathZoom.contains(source)) {
                                    for (String res : filterResult) {
                                        if ( res.contains(sourceInfo) ) {
                                            if ( !res.endsWith(sourceInfo) )
                                                Collections.addAll(result, res.substring(res.indexOf(sourceInfo) + sourceInfo.length()).split(","));
                                            isFilter = true;
                                            break;
                                        }
                                    }
                                }

                                if (isFilter) {
                                    int len = filterResult.size();
                                    filterResult.add( String.join(",", result) );
                                    if (len != filterResult.size()) {
                                        ResultProcesser.process(new SinkResult(true, (ArrayList<String>) result.clone()));
//                                        logger.info(s.toString(true));
                                    }
                                } else {
                                    int len = otherResult.size();
                                    otherResult.add( String.join(",", result) );
                                    if (len != otherResult.size()) {
                                        ResultProcesser.process(new SinkResult(false, (ArrayList<String>) result.clone()));
//                                        logger.info(s.toString(true));
                                    }
                                }
                            }

                            // 当 source 已经完成遍历时加入 pathZoom , 目前情况是如果达到一定层级时当前 source 没找到也会加入 pathZoom - -
                            // 可能对这部分造成遗漏，推荐多层数，多层数下可能已经走得很深，即使有遗漏但可以接受
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

    public static void dynamicLookup(){
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
}
