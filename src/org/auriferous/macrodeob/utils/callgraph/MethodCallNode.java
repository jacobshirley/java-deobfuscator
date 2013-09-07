package org.auriferous.macrodeob.utils.callgraph;

import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.SourceValue;

public class MethodCallNode {
	public MethodInsnNode reference;
	
	public MethodCall methodCall;
	
	public MethodCallNode parentCallNode;
	public MethodCallNode nextCallNode;
	public MethodCallNode prevCallNode;
	public SourceValue[] callStack;	
	
	public MethodCallNode(MethodCall methodCall) {
		this.methodCall = methodCall;
	}
	
	public MethodCallNode(MethodInsnNode reference, MethodCall methodCall) {
		this.reference = reference;
		this.methodCall = methodCall;
	}	
	
	@Override
	public String toString() {
		return "Method reference to "+reference.owner+"."+reference.name+reference.desc;
	}
}
