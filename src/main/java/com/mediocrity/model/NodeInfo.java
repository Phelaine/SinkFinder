package com.mediocrity.model;

import lombok.Data;
import org.objectweb.asm.tree.*;

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
    private InvokeDynamicInsnNode dynamicInsnNode;  // 动态方法调用操作码节点 - 所有操作码节点父类

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

    public NodeInfo(ClassNode classNode, MethodNode methodNode, InvokeDynamicInsnNode dynamicInsnNode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.dynamicInsnNode = dynamicInsnNode;
    }

}
