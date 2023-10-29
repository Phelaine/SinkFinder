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
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class InsnAnalysis {

//    private static ClassInfo classInfoBackup;
//    private static ArrayList<ClassInfo> superClasses;
    private static String clsName;
    // 类中的方法名
    private static String methodName;
    // sink 类名
    private static String insnMethodOwner;
    // sink 类名集合
    private static ArrayList<String> insnMethodOwners = new ArrayList<>();
    // sink 方法名
    private static String insnMethodName;
    // sink 信息
    private static ArrayList<String> sinkInfo = new ArrayList<>();
    // source 信息
    private static String sourceInfo;
    private static String source;
    private static Boolean isFind;

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
                    findSource(sink, sinkRule, depth, ruls, count);
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
            findSource(sink, new SinkRule(), depth, ruls, count);
        }

        return finalResult;
    }

    /**
     * 通过 sink 找调用者
     *
     * @param sink   目标调用规则
     * @param depth  最大递归深度
     * @param ruls   规则限制
     * @param count  递归深度
//     * @param result 结果
     * @return
     */
    private static void findSource(String sink, SinkRule sinkRule, int depth, Rules ruls,
                                   int count) {

        for (ClassInfo classInfo : ClassRepo.getInstance().listAll()) {
            clsName = classInfo.getClassName();
//            classInfoBackup = classInfo;

            if (RuleUtil.isIncluded(clsName, ruls.getClassInclusions()) && !RuleUtil.isExcluded(clsName, ruls.getClassExclusions())) {
//                findInFind(classInfo, count, sink, depth, ruls, sinkRule);
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfo.getClassNode())) {
//                    methodName = wrapper.getMethodNode().name;

                    source = clsName + ":" + wrapper.getMethodNode().name;
                    sourceInfo = classInfo.getClassInfo() + ":" + wrapper.getMethodNode().name;

                    insnMethodOwner = wrapper.getMethodInsnNode().owner.replaceAll("/", "\\.");;
                    insnMethodOwners.clear();
                    ClassInfo info = ClassRepo.getInstance().getClassInfo(insnMethodOwner);
                    if (info != null){
                        if (info.getSuperClasses() == null) {
                            findSuperClasses(insnMethodOwner);
                            if (insnMethodOwners.size()==0){
                                insnMethodOwners.add(info.getClassNode().superName);
                            }
                            info.setSuperClasses((ArrayList<String>) insnMethodOwners.clone());
                        }else{
                            insnMethodOwners = (ArrayList<String>) info.getSuperClasses().clone();
                        }
                    }
//                    if (ClassRepo.getInstance().getClassInfo(insnMethodOwner).getSuperClasses() == null) {
//                        findSuperClasses(insnMethodOwner);
//                        ClassRepo.getInstance().getClassInfo(insnMethodOwner).setSuperClasses((ArrayList<String>) insnMethodOwners.clone());
//                    }else{
//                        insnMethodOwners = (ArrayList<String>) ClassRepo.getInstance().getClassInfo(insnMethodOwner).getSuperClasses().clone();
//                    }
                    insnMethodOwners.add(insnMethodOwner);

                    insnMethodName = wrapper.getMethodInsnNode().name;
//                    ClassInfo info = ClassRepo.getInstance().getClassInfo(insnMethodOwner);
//                    superClasses.add(info);
//                    superClasses.forEach(info1 -> insnMethodOwners.add(info1.getClassName()));
//                    methodSink = insnMethodOwner + ":" + insnMethodName + wrapper.getMethodInsnNode().desc;

                    sinkInfo.clear();
                    if (count == 1) {
//                        methodSink = insnMethodOwner + ":" + insnMethodName;
                        insnMethodOwners.forEach(s -> sinkInfo.add(s + ":" + insnMethodName));
                    }else{
                        insnMethodOwners.forEach(s -> sinkInfo.add(s + ":" + insnMethodName + wrapper.getMethodInsnNode().desc));
                    }
//                isFind = methodSink.contains(sink) && !(classInfo.getClassName() + ":" + methodName).equals(methodSink);
                    isFind = sinkInfo.contains(sink);
//                    }else {
////                isFind = sink.contains(methodSink) && !(classInfo.getClassName() + ":" + methodName + wrapper.getMethodNode().desc).equals(methodSink);
//                        isFind = sink.contains(sinkInfo);
//                    }

                    // 如果根据规则找到的话
                    if (isFind && !result.contains(sourceInfo)) {
//                        methodSource = classInfoBackup.getClassInfo() + ":" + methodName;

                        result.add(sourceInfo);
                        if (count < depth || depth == -1) {
                            count++;
                            isRecord = true;
//                    findSource(methodSource + wrapper.getMethodNode().desc, sinkRule, depth, ruls, count);
                            findSource(source + wrapper.getMethodNode().desc, sinkRule, depth, ruls, count);
                        }
                        if (isRecord) {
                            if (sinkRule.getSinkName() == null) {
                                SinkResult s = new SinkResult(count, "CUSTOM",
                                        "CUSTOM", (ArrayList<String>)result.clone());
                                System.out.println(s);
                                finalResult.add(s);
                            } else {
                                SinkResult s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), (ArrayList<String>)result.clone());
                                System.out.println(s);
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

//    private static void findInFind(ClassInfo classInfo, int count, String sink, int depth, Rules ruls,
//                                   SinkRule sinkRule){
//        for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfo.getClassNode())) {
//            // 类中的方法名
//            methodName = wrapper.getMethodNode().name;
//            // 类中方法信息
//            methodSource = classInfo.getClassInfo() + ":" + methodName;
////                    paramNodesSource = wrapper.getMethodNode().parameters;
//            // 调用方法类名
//            methodInsnOwner = wrapper.getMethodInsnNode().owner;
//            superClasses = null;
//            ClassInfo info = ClassRepo.getInstance().getClassInfo(methodInsnOwner);
//            findSuperClasses(info);
//            superClasses.add(info);
//
//            // 调用方法方法名
//            methodInsnName = wrapper.getMethodInsnNode().name;
//            // 调用方法信息
//            methodSink = methodInsnOwner + ":" + methodInsnName + wrapper.getMethodInsnNode().desc;
//
//            if (count == 1) {
//                methodSink = methodInsnOwner + ":" + methodInsnName;
////                        if (paramNodesSource == null){
////                            paramNodesSource = new ArrayList<>();
////                        }
//
////                isFind = methodSink.contains(sink) && !(classInfo.getClassName() + ":" + methodName).equals(methodSink);
//                isFind = methodSink.contains(sink);
//            }else {
////                isFind = sink.contains(methodSink) && !(classInfo.getClassName() + ":" + methodName + wrapper.getMethodNode().desc).equals(methodSink);
//                isFind = sink.contains(methodSink);
//            }
//
//            // 如果根据规则找到的话
//            if (isFind) {
//                methodSource = classInfoBackup.getClassInfo() + ":" + methodName;
//                if (!result.contains(methodSource))
//                result.add(methodSource);
//                if (count < depth || depth == -1) {
//                    count++;
//                    isRecord = true;
////                    findSource(methodSource + wrapper.getMethodNode().desc, sinkRule, depth, ruls, count);
//                    findSource(methodSource + wrapper.getMethodNode().desc, sinkRule, depth, ruls, count);
//                }
//                if (isRecord) {
////                            sinkLog(String.format(format, "深度" + count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), String.join("\t", result)));
//
//                    if (sinkRule.getSinkName() == null) {
//                        SinkResult s = new SinkResult(count, "CUSTOM",
//                                "CUSTOM", (ArrayList<String>)result.clone());
//                        System.out.println(s);
//                        finalResult.add(s);
//                    } else {
//                        SinkResult s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), (ArrayList<String>)result.clone());
//                        System.out.println(s);
//                        finalResult.add(s);
//                    }
//                }
//                count--;
//                isRecord = false;
//                result.remove(result.size() - 1);
//            }
//        }
////        if (!isFind){
////            if (classInfo.getClassNode().superName != "java/lang/Object"){
////                for (ClassInfo classInfo1 : ClassRepo.getInstance().listAll()){
////                    if (classInfo1.getClassNode().name.equals(classInfo.getClassNode().superName)){
////                        sink = sink.split(":")[(sink.split(":")).length-1];
////                        if (RuleUtil.isIncluded(classInfo1.getClassNode().name.replaceAll("/", "\\."), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfo1.getClassNode().name.replaceAll("/", "\\."), ruls.getClassExclusions()))
////                            findInFind(classInfo1, count, sink, depth, ruls, sinkRule);
//////                        System.out.println(classInfo1.getClassNode().name + "count");
////                        break;
////                    }
////                }
////            }
////        }
//    }

//    private static void findSuperClasses(String className){
//        ClassInfo i = ClassRepo.getInstance().getClassInfo(className);
//        if (i != null && !i.getClassNode().superName.equals("java/lang/Object")) {
//            String s = i.getClassNode().superName.replaceAll("/", "\\.");;
//            insnMethodOwners.add(s);
//            findSuperClasses(s);
//        }
////        String superName = classInfo.getClassNode().superName;
////        if (!superName.equals("java/lang/Object")) {
////            for (ClassInfo classInfo1 : ClassRepo.getInstance().listAll()) {
////                if (classInfo1.getClassNode().name.equals(superName)) {
////                    superClasses.add(classInfo1);
////                    findSuperClasses(classInfo1);
////                    break;
////                }
////            }
////        }
//    }

    private static void findSuperClasses(String className){
        ClassInfo i = ClassRepo.getInstance().getClassInfo(className);
        if (i != null && !i.getClassNode().superName.equals("java/lang/Object")) {
            String s = i.getClassNode().superName.replaceAll("/", "\\.");;
            insnMethodOwners.add(s);
            findSuperClasses(s);
        }
    }
}
