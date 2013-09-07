package org.auriferous.macrodeob.transformers;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.base.TransformMethodNode;
import org.auriferous.macrodeob.utils.RsClassLoader;
import org.auriferous.macrodeob.utils.callgraph.MethodCall;
import org.auriferous.macrodeob.utils.callgraph.MethodCallNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class DuplicatesTransform implements Transform {

	@Override
	public boolean accept(TransformClassNode tcn) {
		//if (!tcn.name.equals("client"))
		//return false;
		if (Modifier.isInterface(tcn.access))
			return false;

		List<MethodNode> list = tcn.methods;

		//System.out.println("now "+list.size());
		Iterator<MethodNode> it = list.iterator();
		while (it.hasNext()) {
			MethodNode mn = it.next();
			MethodCall mc = ((TransformMethodNode) mn).call;
			if (mc.references.size() == 0) {
				String key = tcn.name + "." + mn.name + mn.desc;
				if (!RsClassLoader.protectedMethodMap.contains(tcn.name + "."
						+ mn.name + mn.desc)) {
					//System.out.println("sdfsd");
					List<MethodNode> supers = ((TransformMethodNode) mn)
							.getSuperMethods();
					boolean isReferenced = false;
					for (MethodNode sup : supers) {
						
						TransformMethodNode tmn = (TransformMethodNode) sup;
						if (RsClassLoader.protectedMethodMap
								.contains(tmn.owner.name + "." + tmn.name
										+ tmn.desc)) {
							isReferenced = true;
							break;
						}
						MethodCall mc2 = tmn.call;
						if (mc2.references.size() > 0) {
							isReferenced = true;
							break;
						}
					}
					if (!isReferenced) {
						//System.out.println("removing "+key);
						it.remove();
						for (MethodNode sup : supers) {
							((TransformMethodNode) sup).owner.methods
									.remove(sup);
						}//*/
					}
				}
			}
		}
		
		//System.out.println("then "+list.size());

		int len = list.size();
		HashMap<MethodNode, MethodNode> toBeRemoved = new HashMap<>();
		for (int i = 0; i < len; i++) {
			MethodNode cur = list.get(i);
			if (cur.instructions.size() == 0)
				continue;
			
			String key = tcn.name+"."+cur.name+cur.desc;
			
			if (((TransformMethodNode)cur).getSuperMethods().size() > 0)
				continue;
			
			InsnList curInsns = cur.instructions;
			for (int j = i + 1; j < len; j++) {
				MethodNode sub2 = list.get(j);
				key = tcn.name + "."
						+ sub2.name + sub2.desc;
				if (RsClassLoader.protectedMethodMap.contains(tcn.name + "."
						+ sub2.name + sub2.desc)) {
					continue;
				}
				if (((TransformMethodNode)sub2).getSuperMethods().size() > 0)
					continue;
				
				if (sub2.access != cur.access)
					continue;

				MethodNode in1 = sub2.desc.length() < cur.desc.length() ? sub2 : cur;
				MethodNode in2 = sub2.desc.length() > cur.desc.length() ? sub2 : cur;
				if (cur.desc.equals(sub2.desc)) {
					InsnList sub2List = sub2.instructions;
					if (instructionListsSame(curInsns, sub2List)) {
						if (!toBeRemoved.containsKey(sub2))
							toBeRemoved.put(sub2, cur);
					}
				}
			}
		}

		for (Entry<MethodNode, MethodNode> entry : toBeRemoved.entrySet()) {
			TransformMethodNode duplicate = (TransformMethodNode) entry
					.getKey();
			String key = tcn.name + "."
					+ duplicate.name + duplicate.desc;
			if (RsClassLoader.protectedMethodMap.contains(key)) {
				continue;
			}
			
			TransformMethodNode val = (TransformMethodNode) entry.getValue();

			for (MethodCallNode mcn : duplicate.call.references) {
				mcn.reference.name = val.name;
			}
			
			tcn.methods.remove(duplicate);
		}//*/

		return false;
	}

	private boolean instructionListsSame(InsnList insnList1, InsnList insnList2) {
		int size = insnList1.size();
		if (size < 5) {
			return false;
		}
		if (size == insnList2.size()) {
			for (int i = 0; i < size; i++) {
				AbstractInsnNode cur1 = insnList1.get(i);
				AbstractInsnNode cur2 = insnList2.get(i);
				if (cur1.getOpcode() != cur2.getOpcode())
					return false;
				if (cur1 instanceof VarInsnNode) {
					if (((VarInsnNode) cur1).var != ((VarInsnNode) cur2).var) {
						//System.out.println("Solved!");
						return false;
					}
				}

				/*if (cur2 instanceof LdcInsnNode) {
					if (((LdcInsnNode) cur1).cst != ((LdcInsnNode) cur2).cst) {
						//System.out.println("Solved!");
						return false;
					}
				}*/
				
				if (cur2 instanceof IntInsnNode) {
					if (((IntInsnNode) cur1).operand != ((IntInsnNode) cur2).operand) {
						//System.out.println("Solved!");
						return false;
					}
				}
				
				if (cur2 instanceof TypeInsnNode) {
					if (((TypeInsnNode) cur1).desc != ((TypeInsnNode) cur2).desc) {
						//System.out.println("Solved!");
						return false;
					}
				}
				
				if (cur2 instanceof MethodInsnNode) {
					MethodInsnNode min1 = (MethodInsnNode) cur1;
					MethodInsnNode min2 = (MethodInsnNode) cur2;
					if (!min1.owner.equals(min2.owner) || !min1.name.equals(min2.name) || !min1.desc.equals(min2.desc)) {
						//System.out.println("Solved!");
						return false;
					}
				}
				
				if (cur2 instanceof FieldInsnNode) {
					FieldInsnNode min1 = (FieldInsnNode) cur1;
					FieldInsnNode min2 = (FieldInsnNode) cur2;
					if (!min1.name.equals(min2.name) || !min1.desc.equals(min2.desc)) {
						//System.out.println("Solved!");
						return false;
					}
				}

				/*if (cur2 instanceof LineNumberNode) {
					LineNumberNode min1 = (LineNumberNode) cur1;
					LineNumberNode min2 = (LineNumberNode) cur2;
					if (min1.line != min2.line || min1.start != min2.start) {
						//System.out.println("Solved!");
						return false;
					}
				}*/
				
			}
			return true;
		}
		return false;
	}
}
