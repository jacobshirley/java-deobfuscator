package org.auriferous.macrodeob.transformers.base;

import java.util.ArrayList;
import java.util.List;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.utils.callgraph.MethodCall;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class TransformMethodNode extends MethodNode {
	public ClassNode owner;
	public MethodCall call;
	private List<MethodNode> superMethods;
	
	public TransformMethodNode(ClassNode owner, int access, String name, String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM5, access, name, desc, signature, exceptions);
		this.owner = owner;
	}
	
	public List<MethodNode> getSuperMethods() {
		if (superMethods == null) {
			superMethods = new ArrayList<>();
			findMethodsPlus(owner);
		}
		return superMethods;
	}
	
	protected void findMethodsPlus(ClassNode cn) {
		List<String> supers = new ArrayList<>(cn.interfaces);
		supers.add(cn.superName);
		String key = name + desc;
		for (String s : supers) {
			TransformClassNode tcn = Main.rsClassLoader.loadClass(s);
			if (tcn == null)
				continue;
			for (MethodNode mn : tcn.methods) {
				if ((mn.name + mn.desc).equals(key)) {
					superMethods.add(mn);
					break;
				}
			}
			findMethodsPlus(tcn);
		}
	}
}
