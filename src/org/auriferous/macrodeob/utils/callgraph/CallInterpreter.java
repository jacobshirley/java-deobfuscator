package org.auriferous.macrodeob.utils.callgraph;

import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class CallInterpreter extends SourceInterpreter {
	
	public HashMap<AbstractInsnNode, SourceValue[]> callStackMap = new HashMap<>();
	
	@Override
	public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
		callStackMap.put(insn, createCallStack(value));
		return super.unaryOperation(insn, value);
	}
	
	@Override
	public SourceValue newOperation(AbstractInsnNode insn) {
		callStackMap.put(insn, null);
		return super.newOperation(insn);
	}
	
	@Override
	public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
		callStackMap.put(insn, createCallStack(value));
		return super.copyOperation(insn, value);
	}
	
	@Override
	public SourceValue ternaryOperation(AbstractInsnNode insn,
			SourceValue value1, SourceValue value2, SourceValue value3) {
		callStackMap.put(insn, createCallStack(value1, value2, value3));
		return super.ternaryOperation(insn, value1, value2, value3);
	}
	
	@Override
	public SourceValue binaryOperation(AbstractInsnNode insn,
			SourceValue value1, SourceValue value2) {
		callStackMap.put(insn, createCallStack(value1, value2));
		return super.binaryOperation(insn, value1, value2);
	}
	
	@Override
	public SourceValue naryOperation(AbstractInsnNode insn,
			List<? extends SourceValue> values) {
		callStackMap.put(insn, values.toArray(new SourceValue[values.size()]));
		return super.naryOperation(insn, values);
	}
	
	private SourceValue[] createCallStack(SourceValue... sources) {
		return sources;
	}
}
