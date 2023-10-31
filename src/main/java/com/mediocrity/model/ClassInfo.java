package com.mediocrity.model;

import lombok.Data;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/17
 */
@Data
public class ClassInfo {
    private ClassNode classNode;
    private String jarName;
    private ArrayList<String> superClasses;

    public ClassInfo(ClassNode classNode, String jarName) {
        this.classNode = classNode;
        this.jarName = jarName;
    }

    public String getClassInfo(){
        return jarName + "#" + classNode.name.replaceAll("/", "\\.");
    }

    public String getClassName(){
        return classNode.name.replaceAll("/", "\\.");
    }

}
