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

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class InsnAnalysis {

    private static HashSet<String> nullOrNoSuperClasses = new HashSet<>();
    // sink 类名
    private static String insnMethodOwner;
    // sink 父类集合
    private static ArrayList<String> insnMethodOwners = new ArrayList<>();
    // sink 方法名
    private static String insnMethodName;
    // sink 方法描述
    private static String insnMethodDesc;
    // sink 信息
    // private static ArrayList<String> sinkInfo = new ArrayList<>();
    // source 信息
    private static String sourceInfo;
    private static String source;

    private static Boolean isFind;

    private static ArrayList<String> result = new ArrayList<>();
    private static ArrayList<SinkResult> finalResult = new ArrayList<>();

    private static Boolean isRecord = true;

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
     * 通过 sink 找 source ，并将 source 记录下来
     *
     * @param sink  目标调用规则
     * @param depth 最大递归深度
     * @param ruls  规则限制
     * @param count 递归深度
     * @return
     */
    private static void findSource(String sink, SinkRule sinkRule, int depth, Rules ruls,
                                   int count) {

        for (ClassInfo classInfo : ClassRepo.getInstance().listAll()) {
//            clsName = classInfo.getClassName();

            if (RuleUtil.isIncluded(classInfo.getClassName(), ruls.getClassInclusions()) && !RuleUtil.isExcluded(classInfo.getClassName(), ruls.getClassExclusions())) {
                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfo.getClassNode())) {
//                    methodName = wrapper.getMethodNode().name;
                    isFind = false;

                    source = classInfo.getClassName() + ":" + wrapper.getMethodNode().name;
                    sourceInfo = classInfo.getClassInfo() + ":" + wrapper.getMethodNode().name;

                    insnMethodName = wrapper.getMethodInsnNode().name;
                    insnMethodDesc = wrapper.getMethodInsnNode().desc;
                    insnMethodOwner = wrapper.getMethodInsnNode().owner.replaceAll("/", "\\.");

                    if (count == 1) {
                        if (!sink.split(":")[1].equals(insnMethodName)) continue;
                        if (sink.equals(insnMethodOwner + ":" + insnMethodName)) isFind = true;
                    } else {
                        if (!sink.split(":")[1].equals(insnMethodName + insnMethodDesc)) continue;
                        if (sink.equals(insnMethodOwner + ":" + insnMethodName + insnMethodDesc)) isFind = true;
                    }

                    // 如果没找到，分别去看是否存在父类或是否为接口类
                    if (!isFind) {
                        ClassInfo info = ClassRepo.getInstance().getClassInfo(insnMethodOwner);
                        if (info == null) nullOrNoSuperClasses.add(insnMethodOwner);
                        else {
                            if (!nullOrNoSuperClasses.contains(insnMethodOwner)) {

                                insnMethodOwners.clear();

                                if (info.getSuperClasses() == null) {
                                    findSuperClasses(insnMethodOwner);
                                    if (insnMethodOwners.size() == 0) nullOrNoSuperClasses.add(insnMethodOwner);
                                    else info.setSuperClasses((ArrayList<String>) insnMethodOwners.clone());
                                } else {
                                    insnMethodOwners = (ArrayList<String>) info.getSuperClasses().clone();
                                }

                                for (String superName : insnMethodOwners){
                                    if(superName.equals(sink.split(":")[0])){
                                        isFind = true;
                                        break;
                                    }
                                }
                            }

                            // 判断是否为接口类
                            if (!isFind && ( info.getClassNode().access & Opcodes.ACC_INTERFACE ) != 0 ) {
                                ArrayList<ClassInfo> intrfaceImpls = ClassRepo.getInstance().getInterfaces(insnMethodOwner);
                                for (ClassInfo i : intrfaceImpls){
                                    if (i.getClassName().equals(sink.split(":")[0])){
                                        isFind = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 如果根据规则找到的话
                    if (isFind && !result.contains(sourceInfo)) {

                        result.add(sourceInfo);
                        if (count < depth || depth == -1) {
                            count++;
                            isRecord = true;
                            findSource(source + wrapper.getMethodNode().desc, sinkRule, depth, ruls, count);
                        }
                        if (isRecord) {
                            SinkResult s;
                            if (sinkRule.getSinkName() == null) {
                                s = new SinkResult(count, "CUSTOM",
                                        "CUSTOM", (ArrayList<String>) result.clone());
                            } else {
                                s = new SinkResult(count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), (ArrayList<String>) result.clone());
                            }
                            System.out.println(s);
                            finalResult.add(s);
                        }
                        count--;
                        if (count == 0) return;
                        result.remove(result.size() - 1);
                        isRecord = false;
                    }
                }
            }
        }
    }

    private static void findSuperClasses(String className) {
        if (nullOrNoSuperClasses.contains(className))
            return;
        ClassInfo i = ClassRepo.getInstance().getClassInfo(className);
        if (i != null) {
            String s = i.getClassNode().superName.replaceAll("/", "\\.");
            if (!s.equals("java.lang.Object")) {
                insnMethodOwners.add(s);
                findSuperClasses(s);
            } else {
                nullOrNoSuperClasses.add(className);
            }
        } else {
            nullOrNoSuperClasses.add(className);
        }
    }
}
