package com.mediocrity.model;

import lombok.Data;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/17
 */
@Data
public class ClassInfo {
    private final ClassNode classNode;
    private final String jarName;

}
