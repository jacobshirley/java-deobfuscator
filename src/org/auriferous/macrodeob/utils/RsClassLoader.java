package org.auriferous.macrodeob.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RsClassLoader {
	public Map<String, TransformClassNode> classMap = new HashMap<>();
	public Map<String, byte[]> dataMap = new HashMap<>();

	public static List<String> protectedMethodMap = new ArrayList<>();

	private JarFile rsJar;
	private boolean rename;
	private boolean transform = true;

	public RsClassLoader(String jarFile) throws IOException {
		rsJar = new JarFile(jarFile);
	}

	public void enableRenamer(boolean flag) {
		rename = flag;
	}
	
	public void enableTransforms(boolean flag) {
		transform = flag;
	}

	private TransformClassNode loadClass(JarEntry entry) {
		if (entry == null)
			return null;
		String name = entry.getName().replace(".class", "");
		TransformClassNode cn = classMap.get(name);
		if (cn != null)
			return cn;
		try {
			InputStream jis = rsJar.getInputStream(entry);
			ClassReader cr = new ClassReader(jis);
			TransformClassNode tcn = new TransformClassNode();
			cr.accept(tcn, ClassReader.EXPAND_FRAMES);
			classMap.put(name, tcn);
			if (transform)
				tcn.processMethods();

			return tcn;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public TransformClassNode loadClass(String name) {
		TransformClassNode cn = classMap.get(name);
		if (cn != null)
			return cn;
		return loadClass(rsJar.getJarEntry(name + ".class"));
	}

	public void loadAll() throws IOException {
		Enumeration<JarEntry> entries = rsJar.entries();
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			String name = e.getName();
			if (name.endsWith(".class")) {
				loadClass(e);
			} else if (!name.contains("META-INF")) {
				InputStream jis = rsJar.getInputStream(e);
				DataInputStream iStream = new DataInputStream(jis);
				byte[] data = new byte[iStream.available()];
				iStream.readFully(data);

				dataMap.put(name, data);
			}
		}
	}

	public void dumpJar(String dir) throws IOException {
		JarOutputStream outStream = new JarOutputStream(new FileOutputStream(
				new File(dir)));
		final ClassLoader classLoader = new URLClassLoader(new URL[] {new File(rsJar.getName()).toURI().toURL()});
		for (ClassNode tf : classMap.values()) {
			if (tf.superName.equals("java/awt/Canvas")) {
				canvasHack(tf);
			}
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
					| ClassWriter.COMPUTE_FRAMES) {
				@Override
				protected String getCommonSuperClass(String type1, String type2) {
					if (rename) {
						if (type1.contains(type2))
							return type2;
						else if (type2.contains(type1))
							return type1;
						else {
							return "java/lang/Object";
						}
					} else {
						Class<?> c, d;
				        try {
				            c = Class.forName(type1.replace('/', '.'), false, classLoader);
				            d = Class.forName(type2.replace('/', '.'), false, classLoader);
				        } catch (Exception e) {
				            throw new RuntimeException(e.toString());
				        }
				        if (c.isAssignableFrom(d)) {
				            return type1;
				        }
				        if (d.isAssignableFrom(c)) {
				            return type2;
				        }
				        if (c.isInterface() || d.isInterface()) {
				            return "java/lang/Object";
				        } else {
				            do {
				                c = c.getSuperclass();
				            } while (!c.isAssignableFrom(d));
				            return c.getName().replace('.', '/');
				        }
					}
				}
			};
			String name = null;
			if (rename) {
				RemappingClassAdapter rca = new RemappingClassAdapter(cw,
					Main.clientRemapper);
				tf.accept(rca);
				name = Main.clientRemapper.map(tf.name);
			} else
				tf.accept(cw);
			outStream.putNextEntry(new JarEntry((name == null? tf.name : name)
					+ ".class"));
			outStream.write(cw.toByteArray());
		}
		for (Entry<String, byte[]> entry : dataMap.entrySet()) {
			outStream.putNextEntry(new JarEntry(entry.getKey()));
			outStream.write(entry.getValue());
		}
		outStream.close();
	}
	
	private void canvasHack(ClassNode cn) {
		cn.superName = "org/macronite2/rsapplet/Rs2Canvas";
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals("<init>")) {
				for (AbstractInsnNode in : mn.instructions.toArray()) {
					if (in.getOpcode() == Opcodes.INVOKESPECIAL) {
						MethodInsnNode min = (MethodInsnNode)in;
						if (min.owner.equals("java/awt/Canvas"))
							((MethodInsnNode)in).owner = cn.superName;
					}
				}
			} else if (mn.name.equals("paint") || mn.name.equals("update")) {
				InsnList newInsns = new InsnList();
				newInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
				newInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, cn.superName, mn.name, mn.desc));
				
				//mn.instructions.insert(newInsns);
			}
		}
	}
}
