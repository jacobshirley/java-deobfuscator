package org.auriferous.macrodeob.transformers.miners;

import java.lang.reflect.Modifier;
import java.util.List;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.utils.InsnSearcher;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ControlFieldMiner extends MethodNode {
	private MethodVisitor mv;

	public ControlFieldMiner(MethodVisitor mv) {
		super(Opcodes.ASM5);
		this.mv = mv;
	}

	public ControlFieldMiner(int access, String name, String desc,
			String signature, String[] exceptions, MethodVisitor mv) {
		super(access, name, desc, signature, exceptions);
		this.mv = mv;
	}

	@Override
	public void visitEnd() {
		if (TransformClassNode.controlField == null) {
			if (instructions.size() == 0)
				return;
			
			InsnSearcher finder = new InsnSearcher(instructions);
			List<AbstractInsnNode[]> results = finder
					.search("getstatic .store");
			if (results.size() > 0) {
				AbstractInsnNode[] firstSet = results.get(0);
				AbstractInsnNode getstatic = firstSet[0];
				if (getstatic.getOpcode() == Opcodes.GETSTATIC) {
					if (firstSet[1].getOpcode() == Opcodes.ISTORE) {
						VarInsnNode var = (VarInsnNode) firstSet[1];
						int paramCount = Type.getArgumentTypes(this.desc).length;
						boolean isStatic = (access & Modifier.STATIC) > 0;
						if (!isStatic)
							paramCount++;
						if (var.var > paramCount) {
							FieldInsnNode field = (FieldInsnNode) getstatic;
							TransformClassNode.controlField = field.owner + "."
									+ field.name;
						}
					}
				}
			}
		}
		
		accept(mv);
	}
}
