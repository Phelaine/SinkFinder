package com.mediocrity.util;

import com.mediocrity.model.NodeInfo;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */

public class ASMInsnsUtil {
    /**
     * 获取 ClassNode 中所有方法调用节点
     *
     * @param cn The ClassNode.
     * @return A list of NodeItem.
     */
    public static List<NodeInfo> getMethodInsnsInClass(ClassNode cn) {
        final List<NodeInfo> nodes = new ArrayList<>();

        cn.methods.forEach(m -> m.instructions.forEach(i -> {
            if (i instanceof MethodInsnNode) {
                nodes.add(new NodeInfo(cn, m, (MethodInsnNode) i));
            }
        }));

        return nodes;
    }

    public static List<NodeInfo> getInsnsInClass(ClassNode cn) {
        final List<NodeInfo> nodes = new ArrayList<>();

        cn.methods.forEach(m -> m.instructions.forEach(i -> nodes.add(
                new NodeInfo(cn, m, i))));

        return nodes;
    }
}
