package org.auriferous.macrodeob.transformers;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.utils.InsnSearcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class StringTransform implements Transform {

	@Override
	public boolean accept(TransformClassNode tcn) {
		execute(tcn);
		return false;
	}

	private Method findDecodeMethod(Class<?> c, String name, Class<?> returnType) {
		for (Method m : c.getDeclaredMethods()) {
			int mods = m.getModifiers();
			boolean isPrivate = (mods & Modifier.PRIVATE) != 0;
			boolean isStatic = (mods & Modifier.STATIC) != 0;
			Class<?> rType = m.getReturnType();

			if (isPrivate && isStatic && name.equals(m.getName())
					&& rType.equals(returnType)) {
				m.setAccessible(true);
				return m;
			}
		}
		return null;
	}

	private Field findArrayField(Class<?> c, String name, Class<?> type) {
		for (Field f : c.getDeclaredFields()) {
			if (name.equals(f.getName()) && f.getType().equals(type)) {
				f.setAccessible(true);
				return f;
			}
		}
		return null;
	}

	public void execute(TransformClassNode tcn) {
		FieldInsnNode arrayFieldNode = null;
		Field arrayField = null;
		List<MethodNode> methods = tcn.methods;
		String decodeStrName = null; //lazy
		String decodeStrDesc = null;
		String decodeCharName = null;
		String decodeCharDesc = null;
		Iterator<MethodNode> it = methods.iterator();
		ArrayList<String> strings = new ArrayList<>();
		try {
			URLClassLoader cl = URLClassLoader
					.newInstance(new URL[] { new File("loader2.jar").toURI()
							.toURL() });
			for (MethodNode method : methods) {
				InsnList instructions = method.instructions;
				
				if (instructions.size() == 0)
					continue;
				
				if (method.name.equals("<clinit>")) {
					InsnSearcher iFinder = new InsnSearcher(instructions);
					List<AbstractInsnNode[]> results = iFinder
							.search("ldc invokestatic invokestatic");
					if (results.size() > 0) {
						Class<?> cur = cl.loadClass(tcn.name);
						results = iFinder.search("putstatic");
						if (results.size() > 0) {
							arrayFieldNode = (FieldInsnNode)results.get(0)[0];
							arrayField = findArrayField(cur,
									arrayFieldNode.name, String[].class);
							break;
						}
					}
				}
			}
			it = methods.iterator();
			while (it.hasNext()) {
				MethodNode method = it.next();
				if (method.name.equals(decodeStrName)
						|| method.name.equals(decodeCharName)) {
					if (method.desc.equals(decodeStrDesc)
							|| method.desc.equals(decodeCharDesc)) {
						it.remove();
						continue;
					}
				}
				InsnList instructions = method.instructions;
				if (instructions.size() == 0)
					continue;
				InsnSearcher iFinder = new InsnSearcher(method.instructions);
				List<AbstractInsnNode[]> results = iFinder
						.search("bipush invokevirtual");
				for (int i = 0; i < results.size(); i++) {
					IntInsnNode bipush = (IntInsnNode) results.get(i)[0];
					MethodInsnNode min = (MethodInsnNode) results.get(i)[1];
					if (min.name.equals("append")
							&& min.desc.equals("(C)Ljava/lang/StringBuilder;")) {
						int value = bipush.operand;
						MethodInsnNode newMin = new MethodInsnNode(
								min.getOpcode(), min.owner, min.name,
								"(Ljava/lang/String;)Ljava/lang/StringBuilder;");
						instructions.set(min, newMin);
						instructions.set(results.get(i)[0], new LdcInsnNode(""
								+ (char) value));
					}
				}
				if (arrayField != null) {
					results = iFinder
							.search("getstatic (bipush|iconst_.) aaload");
					for (int i = 0; i < results.size(); i++) {
						FieldInsnNode getstatic = (FieldInsnNode) results
								.get(i)[0];
						if (getstatic.name.equals(arrayField.getName())
								&& getstatic.owner.equals(arrayField.getDeclaringClass().getName())) {

							AbstractInsnNode in = results.get(i)[1];
							int arrIndex = -1;
							if (in instanceof IntInsnNode)
								arrIndex = ((IntInsnNode) in).operand;
							else {
								//I_CONSTS
								arrIndex = in.getOpcode() - 3;
							}

							String s = (String) Array.get(arrayField.get(null), arrIndex);
							instructions.set(getstatic,
									new LdcInsnNode(s));
							instructions.remove(results.get(i)[1]);
							instructions.remove(results.get(i)[2]);
						}
					}
				}
			}
			it = methods.iterator();
			while (it.hasNext()) {
				MethodNode method = it.next();
				InsnList instructions = method.instructions;
				
				if (instructions.size() == 0)
					continue;
				
				InsnSearcher iFinder = new InsnSearcher(method.instructions);
				List<AbstractInsnNode[]> results = iFinder
						.search("(getstatic|(ldc2_w|ldc_w|ldc)) ((ldc2_w|ldc_w|ldc)|getstatic) (imul|lmul|dmul) invokevirtual"); //to be fixed
				for (int i = 0; i < results.size(); i++) {
					MethodInsnNode append = (MethodInsnNode) results.get(i)[3];
					if (append.name.equals("append")) {
						for (int j = 0; j < results.get(i).length; j++)
							instructions.remove(results.get(i)[j]);
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
