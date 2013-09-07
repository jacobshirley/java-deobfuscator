package org.auriferous.macrodeob.transformers.miners;

import org.auriferous.macrodeob.transformers.base.TransformMethodNode;
import org.auriferous.macrodeob.transformers.methods.MethodTransform;
import org.auriferous.macrodeob.utils.callgraph.CallGraph;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodCallMiner implements MethodTransform {

	public static CallGraph clientCallGraph = new CallGraph();

	@Override
	public boolean accept(ClassNode cn, MethodNode mn) {
		clientCallGraph.addMethodCall(cn, (TransformMethodNode)mn);
		for (AbstractInsnNode in : mn.instructions.toArray()) {
			if (in instanceof MethodInsnNode)
				clientCallGraph.addMethodCallNode((MethodInsnNode)in);
		}
		return false;
	}
}
