package com.mediocrity.model;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
}
