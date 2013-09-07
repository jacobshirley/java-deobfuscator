package org.auriferous.macrodeob.utils.callgraph;

import java.util.HashMap;
import java.util.Map;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.base.TransformMethodNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class CallGraph {
	public HashMap<String, MethodCall> methodCalls = new HashMap<>();
	private Map<String, String> redirects = new HashMap<>();

	public void addMethodCall(ClassNode parent, TransformMethodNode methodNode) {
		String key = getKey(parent.name, methodNode.name, methodNode.desc);
		String redirect = redirects.get(key);
		key = redirect == null ? key : redirect;

		MethodCall methodCall = methodCalls.get(key);

		if (methodCall != null) {
			methodCall.parent = parent;
			methodCall.method = methodNode;
		} else {
			methodCall = new MethodCall(parent, methodNode.name,
					methodNode.desc, methodNode);
			methodCalls.put(key, methodCall);
		}
		if (methodNode.call == null)
			methodNode.call = methodCall;
	}

	public void addMethodCallNode(MethodInsnNode reference) {
		String owner = reference.owner;
		String name = reference.name;
		String desc = reference.desc;

		String key = getKey(owner, name, desc);
		String redirect = redirects.get(key);

		key = redirect == null ? key : redirect;

		MethodCall methodCall = methodCalls.get(key);
		MethodCallNode callNode = new MethodCallNode(reference, methodCall);

		if (methodCall == null) {
			TransformClassNode tcn = Main.rsClassLoader.loadClass(owner);
			TransformMethodNode tmn = tcn == null ? null : tcn.findMethodPlus(
					name, desc);
			if (tmn != null) {
				if (tmn.call == null) {
					owner = tmn.owner.name;
					String nKey = getKey(owner, name, desc);
					if (!owner.equals(reference.owner))
						redirects.put(key, nKey);
					key = nKey;
					methodCall = new MethodCall(owner, name, desc, tmn);
					methodCalls.put(nKey, methodCall);
				} else
					methodCall = tmn.call;
			} else {
				methodCall = new MethodCall(owner, name, desc, tmn);
				methodCalls.put(key, methodCall);
			}
		}
		methodCall.references.add(callNode);
		callNode.methodCall = methodCall;
	}

	/*private String getKey(MethodInsnNode reference) {
		return reference.owner + "." + reference.name + reference.desc;
	}*/

	private String getKey(String owner, String name, String desc) {
		return owner + "." + name + desc;
	}
}
