package org.auriferous.macrodeob.transformers;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.methods.MethodTransform;
import org.auriferous.macrodeob.transformers.miners.MethodCallMiner;
import org.auriferous.macrodeob.utils.InsnSearcher;
import org.auriferous.macrodeob.utils.InsnUtils;
import org.auriferous.macrodeob.utils.callgraph.CallGraph;
import org.auriferous.macrodeob.utils.callgraph.MethodCall;
import org.auriferous.macrodeob.utils.callgraph.MethodCallNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RedundantRemoveTransform implements MethodTransform {

	public static List<String> REDUNDANT_PARAMS = new ArrayList<>();
	public static List<String> CHECKED_METHODS = new ArrayList<>();

	@Override
	public boolean accept(ClassNode cn, MethodNode m) {

		List<AbstractInsnNode> toBeRemoved = new ArrayList<>();
		for (AbstractInsnNode in : m.instructions.toArray()) {
			if (in instanceof MethodInsnNode) {
				MethodInsnNode min = (MethodInsnNode) in;
				String key = min.owner + "." + min.name + min.desc;
				boolean redundant = false;
				if (!REDUNDANT_PARAMS.contains(key)) {
					TransformClassNode cn2 = Main.rsClassLoader
							.loadClass(min.owner);
					if (cn2 != null) {
						MethodNode m2 = cn2.findMethod(min.name, min.desc);
						if (m2 != null) {
							redundant = testRedundancy(cn2, m2);
							if (redundant)
								m2.desc = m2.desc.replaceAll(".\\)", ")");
						}
					}
				} else
					redundant = true;
				if (redundant) {
					min.desc = min.desc.replaceAll(".\\)", ")");
					toBeRemoved.add(InsnUtils.skipPseudosBack(min));
				}
			}
		}

		for (AbstractInsnNode r : toBeRemoved) {
			m.instructions.remove(r);
		}
		return false;
	}

	private boolean testRedundancy(ClassNode cn, MethodNode m) {
		InsnList insns = m.instructions;
		if (insns.size() == 0)
			return false;

		InsnSearcher finder = new InsnSearcher(m);
		List<AbstractInsnNode[]> results = finder
				.search("iload[0-9]* (.ipush|iconst_.|iconst_m1|ldc) if_icmp(eq|ne|ge|le|gt|lt) goto");
		boolean isStatic = Modifier.isStatic(m.access);
		int paramCount = Type.getArgumentTypes(m.desc).length;
		boolean isRedundant = false;
		for (AbstractInsnNode[] result : results) {

			JumpInsnNode ifJump = (JumpInsnNode) result[2];
			JumpInsnNode gotoJump = (JumpInsnNode) result[3];

			if (ifJump.label == gotoJump.label) {
				isRedundant = true;
				for (AbstractInsnNode in : result)
					insns.remove(in);
			} else if (InsnUtils.skipPseudos(gotoJump.label) == gotoJump) {
				isRedundant = true;
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
				isRedundant = true;
				for (AbstractInsnNode in : result)
					insns.remove(in);
			}
		}

		finder.reload();
		results = finder
				.search("if([^\\s]+) iload[0-9]* (.ipush|iconst_.|iconst_m1|ldc) if_icmp(eq|ne|ge|le|gt|lt) return");
		for (AbstractInsnNode[] result : results) {
			isRedundant = true;
			for (int i = 1; i < result.length; i++) {
				insns.remove(result[i]);
			}
		}

		if (isRedundant)
			REDUNDANT_PARAMS.add(cn.name + "." + m.name + m.desc);
		return isRedundant;
	}
}
