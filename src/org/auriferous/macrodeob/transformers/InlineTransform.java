package org.auriferous.macrodeob.transformers;

import java.lang.reflect.Modifier;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.miners.MethodCallMiner;
import org.auriferous.macrodeob.utils.callgraph.MethodCall;
import org.objectweb.asm.tree.MethodNode;

public class InlineTransform implements Transform{

	@Override
	public boolean accept(TransformClassNode tcn) {
		for (MethodNode mn : tcn.methods) {
			String key = tcn.name+"."+mn.name+mn.desc;
			MethodCall mc = MethodCallMiner.clientCallGraph.methodCalls.get(key);
			if (Modifier.isStatic(mc.method.access) && mc.references.size() == 1) {
				//System.out.println("key "+key+" needs to be inlined");
			}
		}
		return false;
	}
	
}
