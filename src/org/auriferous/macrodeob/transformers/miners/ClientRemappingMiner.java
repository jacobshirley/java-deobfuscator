package org.auriferous.macrodeob.transformers.miners;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.Transform;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.base.TransformMethodNode;
import org.auriferous.macrodeob.utils.RsClassLoader;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ClientRemappingMiner implements Transform {
	public static String EXCLUDED_CLASSES = "";
	private static String EXCLUDED_CLASS_NAMES = "client ja.(.*)";
	public static String EXCLUDED_METHOD_NAMES = "notify <init> <clinit> supplyApplet init start stop destroy update paint run finalize";
	public static String EXCLUDED_FIELD_NAMES = "peer";
	
	private static int classCount = 1;
	private static int methodCount = 1;
	private static int fieldCount = 1;
	private static Map<String, Integer> subMap = new HashMap<>();

	@Override
	public boolean accept(TransformClassNode tcn) {
		String name = tcn.name;
		for (String s : EXCLUDED_CLASSES.split(" ")) {
			if (name.matches(s)) {
				return true;
			}
		}
		boolean matches = false;
		for (String s : EXCLUDED_CLASS_NAMES.split(" ")) {
			if (name.matches(s)) {
				matches = true;
				break;
			}
		}

		if (!matches) {
			if (!tcn.superName.equals("java/lang/Object")) {
				Integer get = subMap.get(tcn.superName);
				if (get == null) {
					get = 1;
					subMap.put(tcn.superName, get);
				} else {
					get++;
					subMap.put(tcn.superName, get);
				}

				Main.clientMapping.put(name, "Sub" + get);
			} else {
				Main.clientMapping.put(name, "Class" + classCount);
				classCount++;
			}
		}

		//if (!Modifier.isInterface(tcn.access)) {
		for (MethodNode method : tcn.methods) {
			matches = false;
			if (Modifier.isNative(method.access))
				continue;
			String methodName = method.name;
			for (String s : EXCLUDED_METHOD_NAMES.split(" ")) {
				if (methodName.equals(s)) {
					matches = true;
					break;
				}
			}

			if (matches)
				continue;

			String key = tcn.name + "." + method.name + method.desc;
			if (!RsClassLoader.protectedMethodMap.contains(key)) {
				List<MethodNode> supers = ((TransformMethodNode) method)
						.getSuperMethods();
				if (supers.size() == 0) {
					Main.clientRemapper.renameMethod(key, "method"
							+ methodCount, false);
				} else {
					/*for (MethodNode mn : supers) {
						String nKey = ((TransformMethodNode) mn).owner.name
								+ "." + mn.name + mn.desc;
						for (MethodNode mn2 : supers) {
							String nKey2 = ((TransformMethodNode) mn2).owner.name
									+ "." + mn2.name + mn2.desc;
							overridesMap.put(nKey, nKey2);
						}
					}
					abstractIndex++;*/
				}
			}
			methodCount++;

		}

		for (FieldNode fn : tcn.fields) {
			matches = false;
			for (String s : EXCLUDED_FIELD_NAMES.split(" "))
				if (fn.name.equals(s)) {
					matches = true;
					break;
				}
			if (matches)
				continue;
			Main.clientMapping.put(tcn.name + "." + fn.name + fn.desc, ""
					+ fieldCount);
			fieldCount++;
		}
		return false;
	}
}
