package org.auriferous.macrodeob.transformers.methods;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.auriferous.macrodeob.utils.InsnSearcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class ConditionalTransform implements MethodTransform {

	@Override
	public boolean accept(ClassNode cn, MethodNode m) {
		if (m.instructions.size() == 0)
			return false;

		InsnList insns = m.instructions;
		InsnSearcher finder = new InsnSearcher(insns);
		List<AbstractInsnNode[]> results = finder.search("(.ipush|iconst_[1-5]*) iload[0-9]* if_icmp..");
		for (AbstractInsnNode[] result : results) {
			//System.out.println("found dff");
			insns.remove(result[1]);
			insns.insertBefore(result[0], result[1]);
		}
		
		results = finder.search("iconst_0 iload[0-9]* if_icmp..");
		for (AbstractInsnNode[] result : results) {
			insns.remove(result[0]);
			((JumpInsnNode)result[2]).setOpcode(result[2].getOpcode()-6);
		}
		
		results = finder.search("iload[0-9]* iconst_0 if_icmp..");
		for (AbstractInsnNode[] result : results) {
			insns.remove(result[1]);
			((JumpInsnNode)result[2]).setOpcode(result[2].getOpcode()-6);
		}
		
		results = finder.search("ldc aload0 (getfield)+ imul");
		for (AbstractInsnNode[] result : results) {
			insns.remove(result[0]);
			insns.insertBefore(result[result.length-1], result[0]);
		}

		return true;
	}
}
