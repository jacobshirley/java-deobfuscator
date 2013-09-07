package org.auriferous.macrodeob.utils;

import java.util.HashMap;
import java.util.Iterator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InsnSearchPlus extends InsnSearcher{
    public InsnSearchPlus(InsnList insns) {
    	super(insns);
    }
    
    public InsnSearchPlus(MethodNode methodNode) {
        this(methodNode.instructions);
    }

    @Override
    public void reload() {
        StringBuffer buffer = new StringBuffer();
        instrIndexMap = new HashMap<AbstractInsnNode, Integer>();
        Iterator<AbstractInsnNode> iterator = insns.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode insn = iterator.next();
            //ignore psuedo instructions
            if (insn.getOpcode() < 0) {
                continue;
            }
            instrIndexMap.put(insn, buffer.length());
            String s = OPCODE_NAME_MAP.get(insn.getOpcode());
            if (insn instanceof VarInsnNode) {
            	s += ((VarInsnNode)insn).var;
            }
            buffer.append(s).append(" ");
        }
        mappedCode = buffer.toString();
        //System.out.println(mappedCode);
    }
}