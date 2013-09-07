package org.auriferous.macrodeob.transformers.methods;

import java.lang.reflect.Modifier;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class FinalizedMethodsTransform implements MethodTransform{

	@Override
	public boolean accept(ClassNode cn, MethodNode mn) {
		if (mn.name.equals("finalize") && mn.access == 0)
			mn.access |= Modifier.PROTECTED;
		return true;
	}
}
