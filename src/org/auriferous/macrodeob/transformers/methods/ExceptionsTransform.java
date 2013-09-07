package org.auriferous.macrodeob.transformers.methods;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.auriferous.macrodeob.utils.InsnSearcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class ExceptionsTransform implements MethodTransform {

	@Override
	public boolean accept(ClassNode cn, MethodNode m) {
		//System.out.println("cur method "+m.name);
		//compressTryCatchBlocks(m);
		if (m.instructions.size() == 0)
			return false;

		InsnList insns = m.instructions;
		InsnSearcher finder = new InsnSearcher(insns);
		List<AbstractInsnNode[]> results = finder
				.search("new dup invokespecial ldc invokevirtual ldc invokevirtual invokevirtual invokestatic athrow");
		for (AbstractInsnNode[] result : results) {
			LabelNode label = (LabelNode) result[0].getPrevious().getPrevious();
			Iterator<TryCatchBlockNode> it = m.tryCatchBlocks.iterator();
			while (it.hasNext()) {
				TryCatchBlockNode tcbn = it.next();
				if (tcbn.handler.equals(label)) {
					it.remove();
					for (AbstractInsnNode in : result)
						insns.remove(in);
				}
			}
		}
		insns.resetLabels();

		return true;
	}

	private void cleanup(String owner, MethodNode method)
			throws AnalyzerException {
		Analyzer<SourceValue> a = new Analyzer<>(new SourceInterpreter());
		a.analyze(owner, method);
		Frame[] frames = a.getFrames();
		AbstractInsnNode[] insns = method.instructions.toArray();
		for (int i = 0; i < frames.length; i++) {
			if ((frames[i] == null) && !(insns[i] instanceof LabelNode)) {
				method.instructions.remove(insns[i]);
			}
		}
	}

	private ArrayList<TryCatchBlockNode> removeList = new ArrayList<>();
	private boolean needsSurroundingTCBN = false;

	private void compressTryCatchBlocks(MethodNode m) {
		//TryCatchBlockNode last = tryCatchBlocks.get(0);

		List<TryCatchBlockNode> tryCatchBlocks = m.tryCatchBlocks;

		InsnList instructions = m.instructions;

		needsSurroundingTCBN = false;
		for (int i = 0; i < tryCatchBlocks.size(); i++) {
			TryCatchBlockNode curBlock = tryCatchBlocks.get(i);
			if (!trueException(m, curBlock, instructions)) {
				removeList.add(curBlock);
			}
		}

		for (int i = 0; i < removeList.size(); i++) {
			tryCatchBlocks.remove(removeList.get(i));
			//System.out.println(removeList.get(i).type);
		}

		//method.cleanup(true);

		if (needsSurroundingTCBN) {
			//System.out.println("SDFS");
			//method.tryCatchBlocks.add(createGenericHandler(method)); //easiest way, but perhaps boring
		}
		//method.cleanup(true);
		//method.reanalyze();
	}

	private boolean trueException(MethodNode tmn, TryCatchBlockNode block,
			InsnList instructions) {
		try {
			int startIndex = instructions.indexOf(block.start);
			int endIndex = instructions.indexOf(block.end);
			for (int j = 0; j < endIndex - startIndex; j++) {
				AbstractInsnNode cur = instructions.get(startIndex + j);
				if (cur instanceof MethodInsnNode) {
					MethodInsnNode methodInsn = (MethodInsnNode) cur;

					String className = methodInsn.owner.replace("/", ".");
					Class<?> testClass = ClassLoader.getSystemClassLoader()
							.loadClass(className);
					Method[] methods = testClass.getDeclaredMethods();
					if (methodInsn.name.equals("<init>")) {
						for (Constructor ctor : testClass
								.getDeclaredConstructors()) {
							String desc = Type.getConstructorDescriptor(ctor);
							if (desc.equals(methodInsn.desc)) {
								for (Class<?> t : ctor.getExceptionTypes()) {
									if (t.getName().replace(".", "/")
											.equals(block.type)) {
										return true;
									}
								}
							}
						}
					} else {
						for (Method m : methods) {
							String desc = Type.getMethodDescriptor(m);
							if (desc.equals(methodInsn.desc)
									&& m.getName().equals(methodInsn.name)) {
								for (Class<?> t : m.getExceptionTypes()) {
									if (t.getName().replace(".", "/")
											.equals(block.type)) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		}
		return false;
	}
}
