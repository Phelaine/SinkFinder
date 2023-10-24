package com.mediocrity.util;

import com.mediocrity.entity.WrapperNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

public class ASMUtil {
    /**
     * 获取 ClassNode 中所有方法调用节点
     *
     * @param c The ClassNode.
     * @return A list of WrapperNode.
     */
    public static List<WrapperNode> getMethodCallsInClass(ClassNode c) {
        final List<WrapperNode> nodes = new ArrayList<>();

        c.methods.forEach(m -> m.instructions.forEach(i -> {
            if (i instanceof MethodInsnNode) {
                nodes.add(new WrapperNode(c, m, (MethodInsnNode) i));
            }
        }));

        return nodes;
    }

    /**
     * 获取 ClassNode 中所有的指令节点
     *
     * @param c The ClassNode.
     * @return A list of WrapperNode.
     */
    public static List<WrapperNode> getInstructionsInClass(ClassNode c) {
        final List<WrapperNode> nodes = new ArrayList<>();

        c.methods.forEach(m -> m.instructions.forEach(i -> nodes.add(
                new WrapperNode(c, m, i))));

        return nodes;
    }
}
