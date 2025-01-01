package com.mediocrity;

import com.mediocrity.entity.SinkRule;
import com.mediocrity.service.InsnAnalysis;
import com.mediocrity.entity.Rules;
import com.mediocrity.entity.SinkResult;
import com.mediocrity.service.ResultProcesser;
import com.mediocrity.util.FileUtil;
import com.mediocrity.util.ClassReaderUtil;
import com.mediocrity.util.RuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Slf4j
public class SinkFinder {
    private static final Logger logger = LoggerFactory.getLogger(SinkFinder.class);
    private static String SINK_RULE_FIlE = "rules.json";

    public static Rules ruls;

    public static String TARGET_PATH = ".";
    private static int RECURSION_DEPTH = 0;
    public static String DASHSCOPE_APIKEY = "";
    public static Boolean LLM_ENABLE = false;
    public static String CUSTOM_SINK_RULE = "";
    private static String CUSTOM_SINK_CATEGORY_BLOCK_RULE = "";
    private static String CUSTOM_SINK_CATEGORY_INCLUDE_RULE = "";
    private static String CUSTOM_CLASS_INCLUSIONS = "";
    private static String CUSTOM_CLASS_EXCLUSIONS = "";
    private static String CUSTOM_JAR_EXCLUSIONS = "";
    private static String CUSTOM_JAR_INCLUSIONS = "";

    public static void main(String[] args) {

        SinkFinder sinkFinder = new SinkFinder();
        sinkFinder.defaultParser(args);

        ruls = (Rules) FileUtil.getJsonContent(SINK_RULE_FIlE, Rules.class);

        sinkFinder.customRule();

        logger.info(ruls.toString());

        logger.info("SinkFinder 启动 ...");

        String[] files = TARGET_PATH.split(",");
        for ( String file : files ){
            File f = new File(file);
            readFile(f, ruls);
        }

        InsnAnalysis.run(ruls);

        // 结果统计
        logger.info("过滤 Source 入口的链路：\n");
        sinkFinder.counter(ResultProcesser.filterResult, ResultProcesser.filterResultLog);
        if (LLM_ENABLE) {
            logger.info("大模型判断为高风险链路：\n");
            sinkFinder.counter(ResultProcesser.llmResult, ResultProcesser.llmResultLog);
        }
        logger.info("其他链路：\n");
        sinkFinder.counter(ResultProcesser.otherResult, ResultProcesser.otherResultLog);

        logger.info("分析任务完成！");

//        ArrayList<SinkResult> sortResults = new ArrayList<>(InsnAnalysis.otherResult);
////        sortResults.sort((o1, o2) -> o2.invokeLength - o1.invokeLength);
//
//        //文件记录
//        sinkFinder.fileStore(sortResults, false);
//
//        ArrayList<SinkResult> sortFilterResults = new ArrayList<>(InsnAnalysis.filterResult);
//        sinkFinder.fileStore(sortFilterResults, true);

    }

    public static void readFile(File dir, Rules ruls) {

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".class") || file.getName().endsWith(".class/") || file.getName().endsWith(".jar") || file.getName().endsWith(".zip") || file.getName().endsWith(".war"))) {
                    ClassReaderUtil.readJar(file, ruls);
                } else {
                    String path = file.getPath();
                    if (!RuleUtil.isExcluded(path, ruls.getPathExclusions())) {
                        readFile(file, ruls);
                    }
                }
            }
        } else if (dir.isFile() && (dir.getName().endsWith(".class") || dir.getName().endsWith(".class/") || dir.getName().endsWith(".jar") || dir.getName().endsWith(".zip") || dir.getName().endsWith(".war"))) {
            ClassReaderUtil.readJar(dir, ruls);
        }
    }

    private void counter(HashSet<SinkResult> sinkResults, String logFile) {
        String out;
        try {
            HashMap<String, Integer> countMap = new HashMap<>();
            int count = 0;
            for (SinkResult sinkResult : sinkResults) {
                count++;
                countMap.put(sinkResult.getSinkCata(), countMap.getOrDefault(sinkResult.getSinkCata(), 0) + 1);
            }

            out = "共找到 " + count + " 条路径 \n";
            FileUtil.writeLog(out, logFile);
            logger.info(out);
            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                out = entry.getKey() + " 类别存在：" + entry.getValue() + " 条路径 \n";
                FileUtil.writeLog(out, logFile);
                logger.info(out);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void defaultParser(String[] args) {

        String banner = "       _         _      __  _             _             \n" +
                "      (_)       | |    / _|(_)           | |            \n" +
                "  ___  _  _ __  | | __| |_  _  _ __    __| |  ___  _ __ \n" +
                " / __|| || '_ \\ | |/ /|  _|| || '_ \\  / _` | / _ \\| '__|\n" +
                " \\__ \\| || | | ||   < | |  | || | | || (_| ||  __/| |   \n" +
                " |___/|_||_| |_||_|\\_\\|_|  |_||_| |_| \\__,_| \\___||_|   \n" +
                "                                             2.0@medi0cr1ty\n" +
                "                                                        ";
        System.out.println(banner);

        Options options = new Options();

        Option path = Option.builder("p").longOpt("path").hasArg().required(true).desc("指定目标分析路径，支持多个以,分隔").build();
        options.addOption(path);

        Option rule = Option.builder("r").longOpt("rule").argName("rules.json").hasArg().required(false).desc("指定Sink JSON规则路径，初始化默认resources/rules.json").build();
        options.addOption(rule);

        Option sink = Option.builder("s").longOpt("sink").hasArg().required(false).desc("自定义sink规则，可添加多个以,分隔").build();
        options.addOption(sink);

        Option targetBlock = Option.builder("scb").longOpt("sink_category_block").hasArg().required(false).desc("禁用sink规则类别").build();
        options.addOption(targetBlock);

        Option targetInclude = Option.builder("sci").longOpt("sink_category_include").hasArg().required(false).desc("配置sink规则类别").build();
        options.addOption(targetInclude);

        Option classWhiteList = Option.builder("ci").longOpt("class_inclusions").hasArg().required(false).desc("自定义class_inclusions规则，类白名单").build();
        options.addOption(classWhiteList);

        Option classBlackList = Option.builder("cb").longOpt("class_exclusions").hasArg().required(false).desc("自定义class_exclusions规则，类黑名单").build();
        options.addOption(classBlackList);

        Option jarWhiteList = Option.builder("ji").longOpt("jar_inclusions").hasArg().required(false).desc("自定义jar_inclusions规则，jar包白名单").build();
        options.addOption(jarWhiteList);

        Option jarBlackList = Option.builder("jb").longOpt("jar_exclusions").hasArg().required(false).desc("自定义jar_exclusions规则，jar包黑名单").build();
        options.addOption(jarBlackList);

        Option depth = Option.builder("d").longOpt("depth").hasArg().argName("3").required(false).desc("指定递归查找深度").build();
        options.addOption(depth);

        Option llm = Option.builder("l").longOpt("llm").required(false).desc("启用通义大模型能力").build();
        options.addOption(llm);

        Option llmKey = Option.builder("lk").longOpt("llm_key").hasArg().required(false).desc("配置通义大模型 API KEY（sk-xxx）").build();
        options.addOption(llmKey);

        Option help = Option.builder("h").longOpt("help").required(false).desc("帮助").build();
        options.addOption(help);

        CommandLine cmd;
        HelpFormatter helper = new HelpFormatter();
        try {
            cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("h")) {
                helper.printHelp("SinkFinder", options);
                System.exit(0);
            }

            TARGET_PATH = cmd.getOptionValue("path");
            log.info("目标分析路径: " + TARGET_PATH);

            if (cmd.hasOption("r")) {
                SINK_RULE_FIlE = cmd.getOptionValue("rule");
            } else {
                File file = new File(SINK_RULE_FIlE);
                if (!file.exists()) {
                    InputStream inputStream = SinkFinder.class.getClassLoader().getResourceAsStream(SINK_RULE_FIlE);
                    OutputStream outputStream = new FileOutputStream(SINK_RULE_FIlE);
                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();
                }

                log.info("规则路径：" + new File(SINK_RULE_FIlE).getAbsolutePath());
            }

            if (cmd.hasOption("s")) {
                CUSTOM_SINK_RULE = cmd.getOptionValue("sink");
                if (!CUSTOM_SINK_RULE.contains(":")){
                    log.warn("自定义sink规则格式参考：全类名:方法名(方法签名)，如：org.apache.http.client.HttpClient:execute(Lorg.apache.http.client.methods.HttpUriRequest;)，方法签名可选");
                    System.exit(0);
                }
                log.info("自定义sink规则: " + CUSTOM_SINK_RULE);
            }

            if (cmd.hasOption("scb")){
                CUSTOM_SINK_CATEGORY_BLOCK_RULE = cmd.getOptionValue("sink_category_block");
                log.info("自定义禁用的 sink 规则类别: " + CUSTOM_SINK_CATEGORY_BLOCK_RULE);
            }

            if (cmd.hasOption("sci")){
                CUSTOM_SINK_CATEGORY_INCLUDE_RULE = cmd.getOptionValue("sink_category_include");
                log.info("自定义查找的 sink 规则类别: " + CUSTOM_SINK_CATEGORY_INCLUDE_RULE);
            }

            if (cmd.hasOption("ci")){
                CUSTOM_CLASS_INCLUSIONS = cmd.getOptionValue("class_inclusions");
                log.info("自定义 class_inclusions 规则: " + CUSTOM_CLASS_INCLUSIONS);
            }

            if (cmd.hasOption("cb")){
                CUSTOM_CLASS_EXCLUSIONS = cmd.getOptionValue("class_exclusions");
                log.info("自定义 class_exclusions 规则: " + CUSTOM_CLASS_EXCLUSIONS);
            }

            if (cmd.hasOption("ji")) {
                CUSTOM_JAR_INCLUSIONS = cmd.getOptionValue("jar_inclusions");
                log.info("自定义 jar_inclusions 规则: " + CUSTOM_JAR_INCLUSIONS);
            }

            if (cmd.hasOption("jb")) {
                CUSTOM_JAR_EXCLUSIONS = cmd.getOptionValue("jar_exclusions");
                log.info("自定义 jar_exclusions 规则: " + CUSTOM_JAR_EXCLUSIONS);
            }

            if (cmd.hasOption("d")) {
                RECURSION_DEPTH = Integer.parseInt(cmd.getOptionValue("depth"));
                if (RECURSION_DEPTH < 20) {
                    log.info("递归查找深度: " + RECURSION_DEPTH);
                } else {
                    log.error("递归查找深度: " + RECURSION_DEPTH + " ，递归深度过高建议重新指定！");
                }
            }

            if (cmd.hasOption("l")) {
                LLM_ENABLE = true;
                if (cmd.hasOption("lk")){
                    DASHSCOPE_APIKEY = cmd.getOptionValue("llm_key");
                    log.info("启用通义大模型能力，配置KEY为: " + DASHSCOPE_APIKEY );
                }
                else log.info("启用大模型能力，未通过命令行配置KEY");
            }

        } catch (Exception e) {
            helper.printHelp("SinkFinder", options);
            System.out.println(e.getMessage());
            System.exit(0);
        }

    }

    private void customRule(){
        if (!CUSTOM_SINK_RULE.isEmpty()) {
            String[] cusSinkRules = CUSTOM_SINK_RULE.split(",");
            ruls.getSinkRules().clear();
            SinkRule sinkRule = new SinkRule("CUSTOM","自定义Sink点","CUSTOM", new ArrayList<>());
            if ( cusSinkRules.length == 1 ) {
                String cusSink = cusSinkRules[0] + ",";
                sinkRule.getSinks().add(cusSink);
            }
            else {
                for (String cusSinkRule : cusSinkRules) {
                    sinkRule.getSinks().add(cusSinkRule);
                }
            }
            ruls.getSinkRules().add(sinkRule);
        }

        if (RECURSION_DEPTH != 0){
            ruls.setDepth(RECURSION_DEPTH);
        }

        if (!CUSTOM_SINK_CATEGORY_BLOCK_RULE.isEmpty()) {
            String[] cusSinkBlockRules = CUSTOM_SINK_CATEGORY_BLOCK_RULE.split(",");
            for (int j = 0; j < ruls.getSinkRules().size(); j++) {
                if (Arrays.asList(cusSinkBlockRules).contains(ruls.getSinkRules().get(j).getSinkName())){
                    ruls.getSinkRules().get(j).getSinks().clear();
                }
            }
        }

        if (!CUSTOM_SINK_CATEGORY_INCLUDE_RULE.isEmpty()) {
            String[] cusSinkIncludeRules = CUSTOM_SINK_CATEGORY_INCLUDE_RULE.split(",");
            for (int j = 0; j < ruls.getSinkRules().size(); j++) {
                if (!Arrays.asList(cusSinkIncludeRules).contains(ruls.getSinkRules().get(j).getSinkName())) {
                    ruls.getSinkRules().get(j).getSinks().clear();
                }
            }
        }

        if (!CUSTOM_CLASS_INCLUSIONS.isEmpty()){
            String[] cusClassInclusions = CUSTOM_CLASS_INCLUSIONS.split(",");
            ruls.getClassInclusions().clear();
            for (String cusClassInclusion : cusClassInclusions) {
                ruls.getClassInclusions().add(cusClassInclusion);
            }
        }

        if (!CUSTOM_CLASS_EXCLUSIONS.isEmpty()){
            String[] cusClassExclusions = CUSTOM_CLASS_EXCLUSIONS.split(",");
            ruls.getClassExclusions().clear();
            for (String cusClassExclusion : cusClassExclusions) {
                ruls.getClassExclusions().add(cusClassExclusion);
            }
        }

        if (!CUSTOM_JAR_INCLUSIONS.isEmpty()){
            String[] cusJarInclusions = CUSTOM_JAR_INCLUSIONS.split(",");
            ruls.getJarNameInclusions().clear();
            for (String cusJarInclusion : cusJarInclusions) {
                ruls.getJarNameInclusions().add(cusJarInclusion);
            }
        }

        if (!CUSTOM_JAR_EXCLUSIONS.isEmpty()){
            String[] cusJarExclusions = CUSTOM_JAR_EXCLUSIONS.split(",");
            ruls.getJarNameExclusions().clear();
            for (String cusJarExclusion : cusJarExclusions) {
                ruls.getJarNameExclusions().add(cusJarExclusion);
            }
        }

        if (!DASHSCOPE_APIKEY.isEmpty()){
            ruls.setDashscopeApiKey(DASHSCOPE_APIKEY);
        }

    }

}

