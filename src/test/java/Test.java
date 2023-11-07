//import com.mediocrity.entity.Rules;
//import com.mediocrity.entity.SinkRule;
//import com.mediocrity.model.ClassInfo;
//import com.mediocrity.model.ClassRepo;
//import com.mediocrity.util.RuleUtil;
//
//import java.util.ArrayList;
//
///**
// * @author: medi0cr1ty
// * @date: 2023/10/29
// * @time: 20:47
// */
//public class Test {
//    private static void findSource(String sink, SinkRule sinkRule, int depth, Rules ruls,
//                                   int count, ArrayList<String> result) {
//
//        for (ClassInfo classInfo : ClassRepo.getInstance().listAll()) {
//            clsName = classInfo.getClassNode().name.replaceAll("/", "\\.");
//
//            if (RuleUtil.isIncluded(clsName, ruls.getClassInclusions()) && !RuleUtil.isExcluded(clsName, ruls.getClassExclusions())) {
//                for (WrapperNode wrapper : ASMUtil.getMethodCallsInClass(classInfo.getClassNode())) {
//
//                    methodName = wrapper.getMethodNode().name;
////                    paramNodesSource = wrapper.getMethodNode().parameters;
//                    methodInsnOwner = wrapper.getMethodInsnNode().owner.replaceAll("/", "\\.");
//                    methodInsnName = wrapper.getMethodInsnNode().name;
//                    methodSink = methodInsnOwner + ":" + methodInsnName + wrapper.getMethodInsnNode().desc;
//                    methodSource = classInfo.getJarName() + "#" + clsName + ":" + methodName;
//
//                    if (count == 1) {
//                        methodSink = methodInsnOwner + ":" + methodInsnName;
////                        if (paramNodesSource == null){
////                            paramNodesSource = new ArrayList<>();
////                        }
//                    }
//
//                    // 如果根据规则找到的话
////                    if (sink.contains(methodSink) && paramNodesSource != null) {
//                    if (sink.contains(methodSink) && !result.contains(methodSource)) {
//
//                        result.add(methodSource);
//                        if (count < depth || depth == -1) {
//                            count++;
//                            isRecord = true;
//                            findSource(methodSource + wrapper.getMethodNode().desc, sinkRule, depth, ruls,
//                                    count, result);
//                        }
//                        if (isRecord) {
////                            sinkLog(String.format(format, "深度" + count, sinkRule.getSinkName(), sinkRule.getSeverityLevel(), String.join("\t", result)));
//
//                            if (sinkRule.getSinkName().isEmpty()) {
//                                SinkResult s = new SinkResult(count, "CUSTOM",
//                                        "CUSTOM", (ArrayList<String>)result.clone());
//                                finalResult.add(s);
//                            } else {
//                                SinkResult s = new SinkResult(count, sinkRule.getSinkName(),
//                                        sinkRule.getSeverityLevel(), (ArrayList<String>)result.clone());
//                                finalResult.add(s);
//                            }
//                        }
//                        count--;
//                        isRecord = false;
//                        result.remove(result.size() - 1);
//                    }
//                }
//            }
//        }
//    }
//}
