package org.auriferous.macrodeob.utils;

import java.util.Map;

import org.auriferous.macrodeob.Main;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

public class ClientRemapper extends SimpleRemapper {
	private Map<String, String> mapping;
	
	public ClientRemapper(Map<String, String> mapping) {
		super(mapping);
		this.mapping = mapping;
	}
	
	@Override
	public String map(String key) {	
		String newkey = super.map(key);
		
		if (newkey == null)
			return null;
		
		TransformClassNode tcn = Main.rsClassLoader.loadClass(key);

		if (tcn != null) {		
			String superName = map(tcn.superName);
			if (superName != null) {
				newkey = superName+"_"+newkey;
			} else {
				superName = tcn.superName;
				superName = superName.substring(superName.lastIndexOf("/")+1);
				newkey = !superName.equals("Object") ? superName+"_"+newkey : newkey;
			}
		}
		
		return newkey;
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		
		String key = super.mapMethodName(owner, name, desc);
		if (key.equals(name)) {
			ClassNode cn = Main.rsClassLoader.loadClass(owner);
			if (cn != null) {
				key = mapMethodName(cn.superName, name, desc);
				if (key.equals(name)) {
					for (String interfaceS : cn.interfaces) {
						key = mapMethodName(interfaceS, name, desc);
						if (!key.equals(name))
							break;
					}
				}
			}
			//System.out.println(mapMethodName(cn.superName, name, desc));
		}
		
		if ((owner+"."+name).equals("alt.w")) {
			//System.out.println(key);
		}
		
		return key;
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		String f = super.map(owner+"."+name+desc);
		
		String oldTypeName = Type.getType(mapDesc(desc)).getClassName();

		if (f != null){
			oldTypeName = oldTypeName.substring(oldTypeName.lastIndexOf(".")+1);
			oldTypeName = oldTypeName.replaceAll("\\[\\]", "Array");
			oldTypeName = oldTypeName.substring(0, 1).toUpperCase() + oldTypeName.substring(1);
			
			String anOrA = beginsWithCapitalVowel(oldTypeName) ? "an" : "a";
			
			return anOrA+oldTypeName+"_"+f;
		} else {
			TransformClassNode tcn = Main.rsClassLoader.loadClass(owner);
			if (tcn != null) {
				owner = tcn.superName;
				name = mapFieldName(owner, name, desc);
			}
			return name == null ? super.mapFieldName(owner, name, desc) : name;
		}
	}
	
	public void renameMethod(String oldName, String newName, boolean force) {
		if (mapping.containsKey(oldName) && !force)
			return;
		mapping.put(oldName, newName);
	}
	
	private boolean beginsWithCapitalVowel (String s) {
		return (s.startsWith ("A") || s.startsWith ("E") || s.startsWith ("I") || s.startsWith ("O") || s.startsWith ("U"));
	}
}