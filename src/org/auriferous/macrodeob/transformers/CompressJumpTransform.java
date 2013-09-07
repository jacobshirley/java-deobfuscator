package org.auriferous.macrodeob.transformers;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.utils.InsnUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class CompressJumpTransform implements Transform {

	@Override
	public boolean accept(TransformClassNode tcn) {
		//if (!tcn.name.equals("d"))
			//return false;
		
		for (MethodNode method : tcn.methods) {

			InsnList instructions = method.instructions;
			
			if (instructions.size() == 0)
				continue;

			/*InsnSearcher finder = new InsnSearcher(instructions);
			
			for (AbstractInsnNode inode : instructions.toArray()) {
				if (inode instanceof FieldInsnNode) {
					FieldInsnNode staticField = (FieldInsnNode) inode;
					if ((staticField.owner + "." + staticField.name)
							.equals(TransformClassNode.controlField)) {
						AbstractInsnNode next = InsnUtils
								.skipPseudos(staticField);
						if (next instanceof VarInsnNode) {
							List<AbstractInsnNode[]> results = finder.search(".load if(eq|ne)");
							VarInsnNode store = (VarInsnNode) next;
							for (int i = 0; i < results.size(); i++) {
								VarInsnNode load = (VarInsnNode)results.get(i)[0];
								if (load.var == store.var) {
									instructions.remove(load);
									JumpInsnNode jump = (JumpInsnNode)results.get(i)[1];
									if (jump.getOpcode() == Opcodes.IFEQ)
										jump.setOpcode(Opcodes.GOTO);
									else
										instructions.remove(jump);
								}
							}
							instructions.remove(store);
							instructions.remove(staticField);
						} else if (next instanceof JumpInsnNode) {
							instructions.remove(staticField);
							if (next.getOpcode() == Opcodes.IFEQ)
								((JumpInsnNode)next).setOpcode(Opcodes.GOTO);
							else
								instructions.remove(next);
						} else if (staticField.getOpcode() == Opcodes.PUTSTATIC) {
							AbstractInsnNode prev = staticField;
							List<AbstractInsnNode> toBeRemoved = new LinkedList<>();
							toBeRemoved.add(prev);
							while (prev.getOpcode() != Opcodes.GETSTATIC) {
								prev = InsnUtils.skipPseudosBack(prev);
								toBeRemoved.add(prev);
							}
							for (AbstractInsnNode in : toBeRemoved)
								instructions.remove(in);
						}
					}
				} 
			}
			
			//clean up dead code
			((TransformMethodNode)method).cleanup(true);
			*/
			
			for (AbstractInsnNode inode : instructions.toArray()) {
				if (isGoto(inode)) {
					AbstractInsnNode target = InsnUtils
							.skipPseudos(((JumpInsnNode) inode).label);
					if (target.equals(InsnUtils.skipPseudos(inode))) {
						instructions.remove(inode);
					} else if (InsnUtils.inRange(target.getOpcode(),
							Opcodes.IRETURN, Opcodes.RETURN)) {
						instructions.set(inode, target.clone(null));
					}
				} else if (isComparator(inode)) {
					JumpInsnNode jump = (JumpInsnNode) inode;
					AbstractInsnNode next = InsnUtils.skipPseudos(jump);
					AbstractInsnNode target = InsnUtils
							.skipPseudos(jump.label);
					if (isGoto(jump)) {
						jump.label = ((JumpInsnNode) target).label;
					} else if (isGoto(next)) {
						int compIndex = instructions.indexOf(jump.label);
						if (compIndex > instructions.indexOf(inode)) {
							int gotoIndex = instructions
									.indexOf(((JumpInsnNode) next).label);
							if (gotoIndex > compIndex) {
								jump.setOpcode(InsnUtils
										.invertJumpOp(jump.getOpcode()));
								jump.label = ((JumpInsnNode) next).label;
								instructions.remove(next);
							}
						}
					}
				}
			}
		}
		
		return false;
	}

	private boolean isComparator(AbstractInsnNode in) {
		int op = in.getOpcode();
		return InsnUtils.inRange(op, Opcodes.IFEQ, Opcodes.IF_ACMPNE)
				|| op == Opcodes.IFNULL || op == Opcodes.IFNONNULL;
	}

	private boolean isGoto(AbstractInsnNode in) {
		return in.getOpcode() == Opcodes.GOTO;
	}

}
