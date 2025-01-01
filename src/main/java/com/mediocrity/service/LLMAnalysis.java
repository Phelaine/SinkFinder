package com.mediocrity.service;

import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.mediocrity.config.PromptConfig;
import com.mediocrity.entity.SinkResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.System;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.mediocrity.model.ClassZoom;
import com.mediocrity.util.DecompileUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(LLMAnalysis.class);

    private static final Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content(PromptConfig.SYS_PROMPT).build();
    private static final Message analysisMsg = Message.builder().role(Role.SYSTEM.getValue()).content(PromptConfig.GUIDELINES_TEMPLATE).build();

    private static String LLMDetail = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + "_LLMDetail.md";

    private static File log;

    public static HashMap<String, String> contextClasses = new HashMap<>();

//    public static Future<Integer> llmTask;
//    public static List<Future<Integer>> llmTasks = new ArrayList<>();

    public static int llmProcess(ArrayList<String> pathResult, SinkResult s, String apiKey){
        StringBuilder contextString = new StringBuilder();
        HashSet<String> pathClass = new HashSet<>();
        String classInfo = "";
        try {
            for (int i = pathResult.size()-1, j = 1 ; i > 0 ; i--, j++ ){
                String className = pathResult.get(i).substring(pathResult.get(i).indexOf("#")+1, pathResult.get(i).indexOf(":"));
                if (!contextClasses.containsKey(className)){
                    ClassNode node = ClassZoom.classes.get(className).getClassNode();
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    byte[] byteArray = writer.toByteArray();

                    classInfo = DecompileUtil.decompile(byteArray, null);
                    contextClasses.put(className, classInfo);
                    contextString.append("代码调用链中第").append(j).append("层 ").append(className).append(" 的代码上下文逻辑:\n").append(classInfo);
                } else if (pathClass.contains(className))
                    contextString.append("代码调用链中第").append(j).append("层 ").append(className).append(" 的上下文逻辑已包含在上文\n");
                else {
                    contextString.append("代码调用链中第").append(j).append("层 ").append(className).append(" 的代码上下文逻辑:\n").append(contextClasses.get(className));
                }
                pathClass.add(className);
            }

            String finalContext = contextString.toString();
//            llmTask = ThreadPoolUtil.submit(() -> call(finalContext, s, apiKey), ThreadType.LLM);
//            llmTasks.add(llmTask);
            return call(finalContext, s, apiKey);
        } catch (Exception e) {
            logger.info("AsyncProcessor executeTask error", e);
        }
        return 0;
    }

    public static int call(String contextClass, SinkResult pathResult, String apiKey) {
        Generation gen = new Generation();
        int score = 0;
        GenerationResult result;
        StringBuilder sb =  new StringBuilder();
        sb.append("\n\n").append(pathResult.toString(false,true)).append("\n");

        List<Message> messages = new ArrayList<>(Arrays.asList(systemMsg, analysisMsg));

        Message msg = Message.builder().role(Role.USER.getValue()).content("最终高危风险功能点及代码调用链如下：" + pathResult.toString(true,true)).build();
        Message msg1 = Message.builder().role(Role.USER.getValue()).content("上下文代码：" + contextClass).build();
        messages.addAll(Arrays.asList(msg, msg1));

        for (int i = 0; i < 2 ; i++) {
            try {
                result = gen.call(createGenerationParam(messages, apiKey));
            } catch (NoApiKeyException | InputRequiredException e) {
                throw new RuntimeException(e);
            }

            for (int j = 0; j < result.getOutput().getChoices().size() ; j++) {
                if ( i==0 ){
                    messages.add(result.getOutput().getChoices().get(j).getMessage());
                    sb.append(result.getOutput().getChoices().get(j).getMessage().getContent());
                }
                else{
                    String r = result.getOutput().getChoices().get(j).getMessage().getContent();
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(r);
                    if (matcher.find()) {
                        score = Integer.parseInt(matcher.group());
                        break;
                    }
                }
            }
            if ( i==0 ) messages.add(Message.builder().role(Role.USER.getValue()).content("只需要给出最终评分数字即可").build());
        }

        LLMAnalysis.write(sb.toString());
        return score;
    }


    public static GenerationParam createGenerationParam(List<Message> messages, String apiKey) {
        GenerationParam param = GenerationParam.builder()
                .model("qwen-plus")
                .messages(messages)
                .resultFormat(ConversationParam.ResultFormat.MESSAGE)
                .build();

        if (!apiKey.isEmpty())
            param.setApiKey(apiKey);
        else if (System.getenv("DASHSCOPE_API_KEY")!=null && !System.getenv("DASHSCOPE_API_KEY").isEmpty())
            param.setApiKey(System.getenv("DASHSCOPE_API_KEY"));
        else{
            throw new RuntimeException("未识别到通义APIKEY，请通过配置文件或环境变量配置DASHSCOPE_API_KEY（sk-xxx）");
        }
        return param;
    }

    public static void write(String content) {
//        lock.lock();
        try (FileWriter fileWriter = new FileWriter(log, true)){
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            lock.unlock();
        }
    }

    static {
        log = new File("logs" + File.separator + LLMDetail);
        try {
            if (!log.exists()) {
                log.createNewFile();
            }
            write("[TOC]\n");
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
