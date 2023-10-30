package com.mediocrity;

import com.mediocrity.core.InsnAnalysis;
import com.mediocrity.entity.Rules;
import com.mediocrity.entity.SinkResult;
import com.mediocrity.util.FileUtil;
import com.mediocrity.util.JarReaderUtil;
import com.mediocrity.util.RuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Slf4j
public class SinkFinder {

    public static String SINK_RULE_FIlE = "rules.json";

    public static String TARGET_PATH = ".";

    public static int RECURSION_DEPTH = 1;

    public static String CUSTOM_SINK_RULE = "";

    public static String DATABASE_ADDR = "";
    public static String DATABASE_NAME = "";
    public static String DATABASE_USER = "";
    public static String DATABASE_PASS = "";

    public static void main(String[] args) {
        String format = "|%1$-5s|%2$-10s|%3$-6s|%4$-1s\n";
        ArrayList<SinkResult> results = new ArrayList<>();

        SinkFinder sinkFinder = new SinkFinder();
        sinkFinder.defaultParser(args);

        log.info("SinkFinder 启动 ...");

        File target_file = new File(TARGET_PATH);

        Rules ruls = (Rules) FileUtil.getJsonContent(SINK_RULE_FIlE, Rules.class);

//        Boolean isSingleJar = false;
        readFile(target_file, ruls);

        if (!CUSTOM_SINK_RULE.isEmpty()) {
            results = InsnAnalysis.runSink(ruls, CUSTOM_SINK_RULE, RECURSION_DEPTH);
        } else {
            results = InsnAnalysis.run(ruls, RECURSION_DEPTH);
        }

        Collections.sort(results, new Comparator<SinkResult>() {
            @Override
            public int compare(SinkResult o1, SinkResult o2) {
                return o2.invokeLength - o1.invokeLength;
            }
        });

        //文件记录
        for (SinkResult sinkResult : results)
            sinkFinder.sinkLog(String.format(format, sinkResult.invokeLength, sinkResult.sinkCata, sinkResult.sinkLevel,
                    String.join("\t", sinkResult.invokeDetail)));

//        //数据库存储
//        if (!DATABASE_ADDR.isEmpty())
//            sinkFinder.databaseStore(results);

        log.info("任务完成！");
    }

    public static void readFile(File dir, Rules ruls) {

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".class") || file.getName().endsWith(".class/") || file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
                    JarReaderUtil.readJar(file, ruls);
                } else {
                    String path = file.getAbsolutePath();
                    if (!RuleUtil.isExcluded(path, ruls.getPathExclusions())) {
                        readFile(file, ruls);
                    }
                }
            }
        } else if (dir.isFile() && (dir.getName().endsWith(".class") || dir.getName().endsWith(".class/") || dir.getName().endsWith(".jar") || dir.getName().endsWith(".zip"))) {
//            isSingleJar = true;
            JarReaderUtil.readJar(dir, ruls);
        }
    }

    private void defaultParser(String[] args) {

        String banner = "       _         _      __  _             _             \n" +
                "      (_)       | |    / _|(_)           | |            \n" +
                "  ___  _  _ __  | | __| |_  _  _ __    __| |  ___  _ __ \n" +
                " / __|| || '_ \\ | |/ /|  _|| || '_ \\  / _` | / _ \\| '__|\n" +
                " \\__ \\| || | | ||   < | |  | || | | || (_| ||  __/| |   \n" +
                " |___/|_||_| |_||_|\\_\\|_|  |_||_| |_| \\__,_| \\___||_|   \n" +
                "                                             0.1@mediocrity\n" +
                "                                                        ";
        System.out.println(banner);

        Options options = new Options();

        Option path = Option.builder("p").longOpt("path")
                .hasArg()
                .required(false)
                .desc("指定目标分析路径").build();
        options.addOption(path);

        Option rule = Option.builder("r").longOpt("rule")
                .argName("rules.json")
                .hasArg()
                .required(false)
                .desc("指定 sink JSON 规则路径，默认为 resource/rules.json").build();
        options.addOption(rule);

        Option target = Option.builder("s").longOpt("sink")
                .hasArg()
                .required(false)
                .desc("自定义 sink 规则（默认无限递归下去）").build();
        options.addOption(target);

        Option depth = Option.builder("d").longOpt("depth")
                .hasArg()
                .argName("3")
                .required(false)
                .desc("指定递归查找深度").build();
        options.addOption(depth);

        Option DB_Addr = Option.builder("da").longOpt("DB_Addr")
                .argName("127.0.0.1")
                .hasArg()
                .required(false)
                .desc("数据库地址").build();
        options.addOption(DB_Addr);

        Option DB_Name = Option.builder("dn").longOpt("DB_Name")
                .argName("demo")
                .hasArg()
                .required(false)
                .desc("数据库名").build();
        options.addOption(DB_Name);

        Option DB_User = Option.builder("du").longOpt("DB_User")
                .argName("root")
                .hasArg()
                .required(false)
                .desc("数据库用户名").build();
        options.addOption(DB_User);

        Option DB_PassWD = Option.builder("dp").longOpt("DB_PassWD")
                .argName("root")
                .hasArg()
                .required(false)
                .desc("数据库密码").build();
        options.addOption(DB_PassWD);

        CommandLine cmd;
        HelpFormatter helper = new HelpFormatter();
        try {
            cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("p")) {
                TARGET_PATH = cmd.getOptionValue("path");
            } else {
                TARGET_PATH = ".";
            }

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

            if (cmd.hasOption("s"))
                CUSTOM_SINK_RULE = cmd.getOptionValue("sink");

            if (cmd.hasOption("d"))
                RECURSION_DEPTH = Integer.parseInt(cmd.getOptionValue("depth"));

            if (cmd.hasOption("da"))
                DATABASE_ADDR = cmd.getOptionValue("DB_Addr");

            if (cmd.hasOption("dn"))
                DATABASE_NAME = cmd.getOptionValue("DB_Name");

            if (cmd.hasOption("du"))
                DATABASE_USER = cmd.getOptionValue("DB_User");

            if (cmd.hasOption("dp"))
                DATABASE_PASS = cmd.getOptionValue("DB_PassWD");

        } catch (Exception e) {
            helper.printHelp("SinkFinder", options);
            System.out.println(e.getMessage());
            System.exit(0);
        }

        log.info("目标分析路径: " + TARGET_PATH);

        if (RECURSION_DEPTH < 20) {
//            log.info("recursion sink rule: " + RECURSION_SINK_RULE);
            log.info("递归查找深度: " + RECURSION_DEPTH);
        } else {
            log.error("递归查找深度: " + RECURSION_DEPTH + " ，递归深度过高建议重新指定！");
        }

        if (CUSTOM_SINK_RULE.length() > 0) {
//            log.info("recursion sink rule: " + RECURSION_SINK_RULE);
            log.info("自定义sink规则: " + CUSTOM_SINK_RULE);
        }

    }

    public void sinkLog(String msg) {
//        System.out.println(msg);

        try {
            File d = new File("logs");
            d.mkdir();
        } catch (Exception e) {
        }
        java.util.Date day = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String logFile = "";
        if (CUSTOM_SINK_RULE == null){
            logFile = "logs/vul_" + sdf.format(day) + "_" + TARGET_PATH.replaceAll("\\.", "_") + ".log";
        }else{
            logFile = "logs/vul_" + sdf.format(day) + "_" + CUSTOM_SINK_RULE.split(":")[0].replaceAll("\\.","_") +
                    ".log";
        }
        try {
            File file = new File(logFile);
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write(msg);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
        }

    }

    private void databaseStore(ArrayList<SinkResult> results){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 建立数据库连接
            String url = "jdbc:mysql://"+DATABASE_ADDR+"/"+DATABASE_NAME;
            String username = DATABASE_USER;
            String password = DATABASE_PASS;
//            Collections.sort(results, new Comparator<SinkResult>() {
//                @Override
//                public int compare(SinkResult o1, SinkResult o2) {
//                    return o2.invokeLength - o1.invokeLength;
//                }
//            });

            int feildLen = results.get(0).invokeLength;

            Connection connection = DriverManager.getConnection(url, username, password);
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + "venustech" +
                    "(count INT NOT NULL," +
                    "sinkcata VARCHAR(45) NOT NULL," +
                    "level VARCHAR(45) NOT NULL," +
                    "result VARCHAR(255)," +
                    "result1 VARCHAR(255)";

            for (int i = 2; i<feildLen; i++) {
                createTableSQL += ",result"+i+" VARCHAR(255)";
            }
            createTableSQL += ")";

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableSQL);
            }

            for (SinkResult sinkResult : results) {

                int len = sinkResult.invokeLength;

                String insertSQL = "INSERT INTO venustech (count, sinkcata, level, result, result1";
                for (int i = 2; i < len; i++) {
                    insertSQL += ",result" + i;
                }
                insertSQL += ") VALUES (?,?,?,?,?";
                for (int i = 2; i < len; i++) {
                    insertSQL += ",?";
                }
                insertSQL += ")";

                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                    preparedStatement.setString(1, String.valueOf(sinkResult.invokeLength));
                    preparedStatement.setString(2, sinkResult.sinkCata);
                    preparedStatement.setString(3, sinkResult.sinkLevel);
                    preparedStatement.setString(4, sinkResult.invokeDetail.get(0));
                    preparedStatement.setString(5, sinkResult.invokeDetail.get(1));
                    for (int i = 2; i < len; i++) {
                        preparedStatement.setString(4+i, sinkResult.invokeDetail.get(i));
                    }
                    // 执行插入操作
                    preparedStatement.execute();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}

