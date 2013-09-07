package org.auriferous.macrodeob.utils.callgraph;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodCall {
	public ClassNode parent;
	public MethodNode method;
	public List<MethodCallNode> references;
	public boolean isSystem;
	
	protected String owner;
	protected String name;
	protected String desc;

	public MethodCall(String owner, String name, String desc, MethodNode method) {
		this.owner = owner;
		this.method = method;
		this.name = name;
		this.desc = desc;
		this.references = new ArrayList<>();
	}

	public MethodCall(ClassNode owner, String name, String desc,
			MethodNode method) {
		this(owner.name, name, desc, method);
		this.parent = owner;
	}

	protected void merge(MethodCall mc) {
		for (MethodCallNode mcn : mc.references) {
			references.add(mcn);
		}
	}
}
