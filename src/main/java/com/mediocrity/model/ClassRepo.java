package com.mediocrity.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
public class ClassRepo {

    private static ClassRepo instance;
    private final Set<ClassInfo> classes;

    public ClassRepo() {
        this.classes = new HashSet<>();
    }

    public static ClassRepo getInstance() {
        if (instance == null) {
            instance = new ClassRepo();
        }

        return instance;
    }

    public Set<ClassInfo> listAll() {
        return classes;
    }

    public ClassInfo getClassInfo(String className){
        for (ClassInfo classInfo : classes){
            if (classInfo.getClassName().equals(className)){
                return classInfo;
            }
        }
        return null;
    }

    public ArrayList<ClassInfo> getInterfaces(String interfaceName){
        interfaceName = interfaceName.replaceAll("\\.", "/");
        ArrayList<ClassInfo> interfaces = new ArrayList<>();
        for (ClassInfo classInfo : classes){
            if (classInfo.getClassNode().interfaces.contains(interfaceName)){
                interfaces.add(classInfo);
            }
        }
        return interfaces;
    }
}
