package org.auriferous.macrodeob.transformers;

import java.util.Iterator;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.utils.InsnUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class RedundantJumpsTransform implements Transform {

	@Override
	public boolean accept(TransformClassNode tcn) {
		for (MethodNode method : tcn.methods) {
			InsnList instructions = method.instructions;
			Iterator<AbstractInsnNode> inIt = instructions.iterator();
			while (inIt.hasNext()) {
				AbstractInsnNode cur = inIt.next();
				if (cur.getOpcode() == Opcodes.GOTO) {
					//System.out.println("OPCODE");
					JumpInsnNode jump = (JumpInsnNode) cur;
					AbstractInsnNode target = InsnUtils
							.skipPseudos(jump.label);

					int opcode = target.getOpcode();
					//System.out.println("opcode "+opcode);
					if (opcode == Opcodes.GOTO) {
						instructions.set(cur, new JumpInsnNode(cur.getOpcode(),
								((JumpInsnNode) target).label));
						//instructions.remove(target);
					}
				}
			}
		}
		return false;
	}
}
