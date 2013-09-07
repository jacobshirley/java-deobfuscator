package org.auriferous.macrodeob.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InsnUtils {

	public static int invertBinToUnaryJump(int opcode) {
		switch (opcode) {
		case Opcodes.IF_ICMPEQ:
			return Opcodes.IFNE;
		case Opcodes.IF_ICMPNE:
			return Opcodes.IFEQ;
		case Opcodes.IF_ICMPGE:
			return Opcodes.IFLE;
		case Opcodes.IF_ICMPLE:
			return Opcodes.IFGE;
		case Opcodes.IF_ICMPGT:
			return Opcodes.IFLT;
		case Opcodes.IF_ICMPLT:
			return Opcodes.IFGT;
		} 
		return opcode;
	}
	
	public static int invertJumpOp(int opcode) {
		switch (opcode) {
		case Opcodes.IF_ICMPEQ:
			return Opcodes.IF_ICMPNE;
		case Opcodes.IF_ICMPNE:
			return Opcodes.IF_ICMPEQ;
		case Opcodes.IF_ICMPGE:
			return Opcodes.IF_ICMPLE;
		case Opcodes.IF_ICMPLE:
			return Opcodes.IF_ICMPGE;
		case Opcodes.IF_ICMPGT:
			return Opcodes.IF_ICMPLT;
		case Opcodes.IF_ICMPLT:
			return Opcodes.IF_ICMPGT;

		case Opcodes.IF_ACMPEQ:
			return Opcodes.IF_ACMPNE;
		case Opcodes.IF_ACMPNE:
			return Opcodes.IF_ACMPEQ;

		case Opcodes.IFEQ:
			return Opcodes.IFNE;
		case Opcodes.IFNE:
			return Opcodes.IFEQ;
		case Opcodes.IFGE:
			return Opcodes.IFLE;
		case Opcodes.IFLE:
			return Opcodes.IFGE;
		case Opcodes.IFGT:
			return Opcodes.IFLT;
		case Opcodes.IFLT:
			return Opcodes.IFGT;

		case Opcodes.IFNULL:
			return Opcodes.IFNONNULL;
		case Opcodes.IFNONNULL:
			return Opcodes.IFNULL;
		}
		return opcode;
	}
	
	public static int invertAddSubOp(int opcode) {
		switch (opcode) {
		case Opcodes.IADD: return Opcodes.ISUB;
		case Opcodes.ISUB: return Opcodes.IADD;
		}
		return opcode;
	}
	
	public static AbstractInsnNode createValueNode(int value) {
		if (inRange(value, -1, 5))
			return new InsnNode(value+3);
		else if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
			return new LdcInsnNode(value);
		} else {
			return new IntInsnNode(inRange(value, Byte.MIN_VALUE, Byte.MAX_VALUE) ? Opcodes.BIPUSH : Opcodes.SIPUSH, value);
		}
	}

	public static AbstractInsnNode skipPseudosBack(AbstractInsnNode in) {
		return skipPseudosBack(in, 1);
	}

	public static AbstractInsnNode skipPseudosBack(AbstractInsnNode in,
			int count) {
		for (int i = 0; i < count; i++) {
			in = in.getPrevious();
			while ((in != null) && isPseudo(in))
				in = in.getPrevious();
		}
		return in;
	}

	public static AbstractInsnNode skipPseudos(AbstractInsnNode in) {
		return skipPseudos(in, 1);
	}

	public static AbstractInsnNode skipPseudos(AbstractInsnNode in, int count) {
		for (int i = 0; i < count; i++) {
			in = in.getNext();
			while ((in != null) && isPseudo(in))
				in = in.getNext();
		}
		return in;
	}

	public static boolean isPseudo(AbstractInsnNode in) {
		return in.getOpcode() < 0;
	}

	public static boolean inRange(int i, int low, int high) {
		return i >= low && i <= high;
	}

	public static boolean isIntConst(AbstractInsnNode inode) {
		if (inode instanceof LdcInsnNode) {
			LdcInsnNode ldc = (LdcInsnNode)inode;
			if (ldc.cst instanceof Integer)
				return true;
		}
		return inRange(inode.getOpcode(), Opcodes.ICONST_M1, Opcodes.ICONST_5) || inode instanceof IntInsnNode;
	}
	
	public static int getIntConstValue(AbstractInsnNode inode) {
		if (inode instanceof LdcInsnNode) {
			LdcInsnNode ldc = (LdcInsnNode)inode;
			if (ldc.cst instanceof Integer)
				return (int)ldc.cst;
		}
		if (inRange(inode.getOpcode(), Opcodes.ICONST_M1, Opcodes.ICONST_5)) {
			return inode.getOpcode()-3;
		} else return ((IntInsnNode)inode).operand;
	}
}
