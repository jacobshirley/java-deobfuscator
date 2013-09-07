package org.auriferous.macrodeob.transformers;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.methods.MethodTransform;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class SwapTransform implements MethodTransform {

	@Override
	public boolean accept(ClassNode cn, MethodNode mn) {
		// TODO Auto-generated method stub
		return false;
	}
}
