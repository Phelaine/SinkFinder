package com.mediocrity.model;

import lombok.Data;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

@Data
public class NodeInfo {

    private final ClassNode classNode;
    private final MethodNode methodNode;
    private MethodInsnNode methodInsnNode;      // 方法调用操作码节点 - METHOD_INSN - 5
    private AbstractInsnNode abstractInsnNode;  // 抽象操作码节点 - 所有操作码节点父类

    public NodeInfo(ClassNode classNode, MethodNode methodNode, MethodInsnNode methodInsnNode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.methodInsnNode = methodInsnNode;
    }

    public NodeInfo(ClassNode classNode, MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.abstractInsnNode = abstractInsnNode;
    }

}
