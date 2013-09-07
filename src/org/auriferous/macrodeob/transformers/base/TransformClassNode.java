package org.auriferous.macrodeob.transformers.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.RedundantParamsTransform;
import org.auriferous.macrodeob.transformers.methods.ConditionalTransform;
import org.auriferous.macrodeob.transformers.methods.FinalizedMethodsTransform;
import org.auriferous.macrodeob.transformers.methods.MethodTransform;
import org.auriferous.macrodeob.transformers.methods.ProtectedMethods;
import org.auriferous.macrodeob.transformers.methods.ExceptionsTransform;
import org.auriferous.macrodeob.transformers.miners.MethodCallMiner;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class TransformClassNode extends ClassNode {

	public static String controlField = null;
	private List<MethodTransform> methodTransforms = new ArrayList<>();

	public TransformClassNode() {
		addMethodTransform(new ExceptionsTransform());
		addMethodTransform(new RedundantParamsTransform());
		addMethodTransform(new MethodCallMiner());
		addMethodTransform(new ProtectedMethods());
		addMethodTransform(new FinalizedMethodsTransform());
		addMethodTransform(new ConditionalTransform());
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		TransformMethodNode mn = new TransformMethodNode(this, access,
				name, desc, signature, exceptions);
		methods.add(mn);
		return mn;
	}

	public void processMethods() {
		for (MethodNode mn : methods)
			for (MethodTransform mt : methodTransforms)
				mt.accept(this, mn);
	}

	public void addMethodTransform(MethodTransform mt) {
		methodTransforms.add(mt);
	}

	public TransformMethodNode findMethod(String name, String desc) {
		String key = name + desc;
		for (MethodNode mn : methods) {
			if ((mn.name + mn.desc).equals(key)) {
				return (TransformMethodNode) mn;
			}
		}
		return null;
	}
	
	protected TransformMethodNode findMethodPlus(ClassNode cn, String name, String desc) {
		List<String> supers = new ArrayList<>(cn.interfaces);
		supers.add(cn.superName);
		String key = name + desc;
		for (String s : supers) {
			TransformClassNode tcn = Main.rsClassLoader.loadClass(s);
			if (tcn == null)
				continue;
			for (MethodNode mn : tcn.methods) {
				if ((mn.name + mn.desc).equals(key)) {
					return (TransformMethodNode)mn;
				}
			}
			TransformMethodNode tmn = findMethodPlus(tcn, name, desc);
			if (tmn != null)
				return tmn;
		}
		return null;
	}

	public TransformMethodNode findMethodPlus(String name, String desc) {
		TransformMethodNode tmn = findMethod(name, desc);
		if (tmn != null)
			return tmn;

		return findMethodPlus(this, name, desc);
	}
}
