package com.mediocrity.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * @Project: sinkfinder
 * @ClassName: FileUtil
 * @Author: TheKingOfDuck
 */
public class FileUtil {
    public static Object getJsonContent(String path, Class<?> type){
        File file = new File(path);
        if(!file.exists()) {
            return null;
        }
        FileReader reader = null;
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
//
//    public String covertPath(String s){
//        s = s.replaceAll("/", "\\.");
////        s = s.replaceAll("\\\\", "\\.");
//        return s;
//    }

}
