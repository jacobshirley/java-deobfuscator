package org.auriferous.macrodeob.transformers.methods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.miners.ClientRemappingMiner;
import org.auriferous.macrodeob.utils.RsClassLoader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ProtectedMethods implements MethodTransform {

	@Override
	public boolean accept(ClassNode cn, MethodNode mn) {
		String name = mn.name;
		String desc = mn.desc;
		if (Modifier.isNative(mn.access)) {
			RsClassLoader.protectedMethodMap.add(cn.name + "."
					+ name + desc);
			return true;
		}
		for (String s : ClientRemappingMiner.EXCLUDED_METHOD_NAMES.split(" ")) {
			if (name.equals(s)) {
				RsClassLoader.protectedMethodMap.add(cn.name + "."
						+ name + desc);
				return true;
			}
		}
		for (String s : ClientRemappingMiner.EXCLUDED_CLASSES.split(" ")) {
			if (cn.name.matches(s)) {
				RsClassLoader.protectedMethodMap.add(cn.name + "."
						+ name + desc);
				return true;
			}
		}
		if (isMethodOverriden(cn, name, desc)) {
			//System.out.println("overriden "+cn.name + "."
				//			+ name + desc);
			RsClassLoader.protectedMethodMap.add(cn.name + "."
							+ name + desc);
		}
		return false;
	}

	private boolean isMethodOverriden(ClassNode cn, String name, String desc) {
		try {
			List<String> supers = new ArrayList<>(cn.interfaces);
			supers.add(cn.superName);
			for (String s : supers) {
				ClassNode cn2 = Main.rsClassLoader.loadClass(s);
				if (cn2 == null) {
					Class<?> cls = ClassLoader.getSystemClassLoader()
							.loadClass(s.replace("/", "."));
					if (isMethodOverriden(cls, name, desc))
						return true;
				} else {
					if (isMethodOverriden(cn2, name, desc))
						return true;
				}
			}
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return false;
	}

	private boolean isMethodOverriden(Class<?> cls, String name, String desc) {
		if (cls == null)
			return false;

		for (Method m : cls.getDeclaredMethods()) {
			if (!Modifier.isStatic(m.getModifiers())) {
				if (m.getName().equals(name)
						&& Type.getMethodDescriptor(m).equals(desc)) {
					return true;
				}
			}
		}

		List<Class<?>> supers = new ArrayList<Class<?>>(Arrays.asList(cls
				.getInterfaces()));

		supers.add(cls.getSuperclass());
		for (Class<?> cls2 : supers) {
			if (cls2 == null)
				continue;

			for (Method m : cls2.getDeclaredMethods()) {
				if (!Modifier.isStatic(m.getModifiers())) {
					if (m.getName().equals(name)
							&& Type.getMethodDescriptor(m).equals(desc)) {
						return true;
					}
				}
			}
			if (isMethodOverriden(cls2, name, desc))
				return true;
		}

		return false;
	}
}
