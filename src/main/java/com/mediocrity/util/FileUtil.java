package com.mediocrity.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
public class FileUtil {
    public static Object getJsonContent(String path, Class<?> type){
        File file = new File(path);
        if(!file.exists()) {
            return null;
        }
        FileReader reader;
        try {
            reader = new FileReader(file);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            return gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeLog(String content, String log) {
        try (FileWriter fileWriter = new FileWriter(log, true)){
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
