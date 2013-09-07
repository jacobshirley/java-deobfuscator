package org.auriferous.macrodeob.transformers.methods;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface MethodTransform {
	public boolean accept(ClassNode cn, MethodNode mn);
}
