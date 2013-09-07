package org.auriferous.macrodeob.utils;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author super_
 */

public class InsnSearcher {

	public static final Map<Integer, String> OPCODE_NAME_MAP;
	private static final Pattern[] NON_INSTRUCTION_CONST_PATTERNS = new Pattern[] {
			Pattern.compile("acc_.+"), Pattern.compile("t_.+"),
			Pattern.compile("v1_.+") };

	protected InsnList insns;
	protected Map<AbstractInsnNode, Integer> instrIndexMap;
	public String mappedCode;

	public InsnSearcher(InsnList insns) {
		this.insns = insns;
		reload();
	}

	public InsnSearcher(MethodNode methodNode) {
		this(methodNode.instructions);
	}

	public void reload() {
		StringBuffer buffer = new StringBuffer();
		instrIndexMap = new HashMap<AbstractInsnNode, Integer>();
		Iterator<AbstractInsnNode> iterator = insns.iterator();
		while (iterator.hasNext()) {
			AbstractInsnNode insn = iterator.next();
			//ignore psuedo instructions
			if (insn.getOpcode() < 0) {
				continue;
			}
			instrIndexMap.put(insn, buffer.length());
			String s = OPCODE_NAME_MAP.get(insn.getOpcode());
			if (insn instanceof VarInsnNode) {
				s += ((VarInsnNode) insn).var;
			}
			buffer.append(s).append(" ");
		}
		mappedCode = buffer.toString();
	}

	private AbstractInsnNode getKey(Integer val) {
		for (Map.Entry<AbstractInsnNode, Integer> entry : instrIndexMap
				.entrySet()) {
			if (entry.getValue().equals(val)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private AbstractInsnNode[] getMatchFromRange(int start, int count) {
		AbstractInsnNode startInsn = getKey(start);
		if (startInsn == null)
			return null;

		int startInsnIdx = insns.indexOf(startInsn);
		AbstractInsnNode[] matches = new AbstractInsnNode[count];
		for (int idx = 0; idx < matches.length; ++idx) {
			AbstractInsnNode iMatch = insns.get(startInsnIdx + idx);

			while (iMatch.getOpcode() < 0) {
				iMatch = iMatch.getNext();
				startInsnIdx++;
			}

			matches[idx] = iMatch;
		}
		return matches;
	}

	public AbstractInsnNode searchSingle(String pattern) {
		return searchSingle(pattern, insns.getFirst());
	}
	
	public AbstractInsnNode searchSingle(String pattern, int index) {
		return searchSingle(pattern, insns.getFirst(), index);
	}
	
	public AbstractInsnNode searchSingle(String pattern, AbstractInsnNode from) {
		return searchSingle(pattern, from, 0);
	}
	
	public AbstractInsnNode searchSingle(String pattern, AbstractInsnNode from, int index) {
		List<AbstractInsnNode[]> results = search(Pattern.compile(pattern.toLowerCase()), from, null);
		return !results.isEmpty() ? results.get(index)[0] : null;
	}
	
	public List<AbstractInsnNode[]> search(String pattern, AbstractInsnNode from) {
		return search(Pattern.compile(pattern.toLowerCase()), from, null);
	}

	public List<AbstractInsnNode[]> search(String pattern, Constraint constraint) {
		return search(Pattern.compile(pattern.toLowerCase()), insns.getFirst(),
				constraint);
	}
	
	public List<AbstractInsnNode[]> search(String pattern, int index) {
		return search(Pattern.compile(pattern.toLowerCase()), insns.get(index), null);
	}

	public List<AbstractInsnNode[]> search(String pattern) {
		return search(Pattern.compile(pattern.toLowerCase()), insns.getFirst());
	}

	public List<AbstractInsnNode[]> search(Pattern pattern,
			AbstractInsnNode from) {
		return search(pattern, from, null);
	}

	public List<AbstractInsnNode[]> search(Pattern pattern,
			Constraint constraint) {
		return search(pattern, insns.getFirst(), constraint);
	}

	public List<AbstractInsnNode[]> search(Pattern pattern) {
		return search(pattern, insns.getFirst());
	}
	
	public List<AbstractInsnNode[]> search(Pattern pattern, AbstractInsnNode from, Constraint constraint) {
		Matcher matcher = pattern.matcher(mappedCode);
		while (from.getOpcode() < 0)
			from = from.getNext();
		int index = instrIndexMap.get(from);
		List<AbstractInsnNode[]> matches = new LinkedList<AbstractInsnNode[]>();
		while (matcher.find(index)) {
			int start = matcher.start();
			int end = matcher.end();
			int count = matcher.group().trim().split(" ").length;

			AbstractInsnNode[] match = getMatchFromRange(start, count);
			if (constraint == null || constraint.accept(match)) {
				if (match != null)
					matches.add(match);
			}
			index = end;
		}
		return matches;
	}
	
	public AbstractInsnNode searchBackwardSingle(String pattern, AbstractInsnNode from, int index) {
		List<AbstractInsnNode[]> results = searchBackward(Pattern.compile(pattern.toLowerCase()), from, null);
		return !results.isEmpty() ? results.get(results.size()-1-index)[0] : null;
	}
	
	public AbstractInsnNode searchBackwardSingle(String pattern, AbstractInsnNode from) {
		List<AbstractInsnNode[]> results = searchBackward(Pattern.compile(pattern.toLowerCase()), from, null);
		return !results.isEmpty() ? results.get(results.size()-1)[0] : null;
	}
	
	public List<AbstractInsnNode[]> searchBackward(String pattern, AbstractInsnNode from) {
		return searchBackward(Pattern.compile(pattern.toLowerCase()), from, null);
	}
	
	public List<AbstractInsnNode[]> searchBackward(Pattern pattern,
			AbstractInsnNode from) {
		return searchBackward(pattern, from, null);
	}
	
	public List<AbstractInsnNode[]> searchBackward(Pattern pattern,
			AbstractInsnNode from, Constraint constraint) {
		while (from.getOpcode() < 0)
			from = from.getPrevious();
		int index = insns.indexOf(from);
		List<AbstractInsnNode[]> matches = search(pattern);
		Iterator<AbstractInsnNode[]> results = matches.iterator();
		while (results.hasNext()) {
			if (insns.indexOf(results.next()[0]) >= index)
				results.remove();
		}
		return matches;
	}

	public static interface Constraint {

		public boolean accept(AbstractInsnNode[] match);
	}

	static {
		OPCODE_NAME_MAP = new HashMap<Integer, String>();
		Class<?> opcodes = Opcodes.class;
		Field[] declaredFields = opcodes.getDeclaredFields();
		for (Field field : declaredFields) {
			int modifiers = field.getModifiers();
			if (isPublic(modifiers) && isStatic(modifiers)
					&& isFinal(modifiers) && field.getType() == Integer.TYPE) {
				try {
					String name = field.getName().toLowerCase();
					boolean failed = false;
					for (Pattern pattern : NON_INSTRUCTION_CONST_PATTERNS) {
						Matcher matcher = pattern.matcher(name);
						if (matcher.find() && matcher.start() == 0) {
							failed = true;
							break;
						}
					}
					if (failed) {
						continue;
					}
					int constant = field.getInt(null);
					OPCODE_NAME_MAP.put(constant, name);
				} catch (IllegalAccessException ex) {
					//cant happen
				}
			}
		}
	}
}