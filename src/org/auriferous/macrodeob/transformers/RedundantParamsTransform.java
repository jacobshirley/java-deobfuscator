package org.auriferous.macrodeob.transformers;

import java.lang.reflect.Modifier;
import java.util.List;

import org.auriferous.macrodeob.transformers.methods.MethodTransform;
import org.auriferous.macrodeob.utils.InsnSearcher;
import org.auriferous.macrodeob.utils.InsnUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RedundantParamsTransform implements MethodTransform {

	@Override
	public boolean accept(ClassNode cn, MethodNode m) {
		InsnList insns = m.instructions;
		if (insns.size() == 0)
			return false;

		InsnSearcher finder = new InsnSearcher(m);
		List<AbstractInsnNode[]> results = finder
				.search("iload[0-9]* (.ipush|iconst_.|iconst_m1|ldc) if_icmp(eq|ne|ge|le|gt|lt) goto");
		boolean isStatic = Modifier.isStatic(m.access);
		int paramCount = Type.getArgumentTypes(m.desc).length;

		for (AbstractInsnNode[] result : results) {

			JumpInsnNode ifJump = (JumpInsnNode) result[2];
			JumpInsnNode gotoJump = (JumpInsnNode) result[3];

			if (ifJump.label == gotoJump.label) {
				for (AbstractInsnNode in : result)
					insns.remove(in);
			} else if (InsnUtils.skipPseudos(gotoJump.label) == gotoJump) {
				for (AbstractInsnNode in : result)
					insns.remove(in);
			}
		}

		finder.reload();
		results = finder
				.search("iload[0-9]* (.ipush|iconst_.|iconst_m1|ldc) if_icmp(eq|ne|ge|le|gt|lt) new dup invokespecial athrow");

		for (AbstractInsnNode[] result : results) {
			TypeInsnNode newType = (TypeInsnNode) result[3];
			if (newType.desc.equals("java/lang/IllegalStateException")) {
				for (AbstractInsnNode in : result)
					insns.remove(in);
			}
		}

		finder.reload();
		results = finder
				.search("if([^\\s]+) iload[0-9]* (.ipush|iconst_.|iconst_m1|ldc) if_icmp(eq|ne|ge|le|gt|lt) return");
		for (AbstractInsnNode[] result : results) {
			if (!refersToParam(((VarInsnNode) result[1]).var, isStatic,
					paramCount))
				continue;
			for (int i = 1; i < result.length; i++) {
				insns.remove(result[i]);
			}
		}

		return false;
	}

	private boolean refersToParam(int varIndex, boolean isStatic, int paramCount) {
		return InsnUtils.inRange(varIndex, isStatic ? 0 : 1, paramCount);
	}
}
