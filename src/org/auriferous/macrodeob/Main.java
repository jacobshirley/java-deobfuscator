package org.auriferous.macrodeob;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.auriferous.macrodeob.transformers.DuplicatesTransform;
import org.auriferous.macrodeob.transformers.Transform;
import org.auriferous.macrodeob.transformers.base.TransformClassNode;
import org.auriferous.macrodeob.transformers.miners.ClientRemappingMiner;
import org.auriferous.macrodeob.utils.ClientRemapper;
import org.auriferous.macrodeob.utils.RsClassLoader;

public class Main {
	private HashMap<Transform, Integer> classTransforms;
	public static RsClassLoader rsClassLoader;
	public static HashMap<String, String> clientMapping = new HashMap<String, String>();
	public static ClientRemapper clientRemapper = new ClientRemapper(
			clientMapping);

	public Main() {
		System.out.println("loading classes...");
		loadClasses("C:/Users/Jake/workspace/Rs2Applet/runescape.jar");
		System.out.println("transforming classes...");
		transformClasses();
		System.out.println("writing classes...");
		writeClasses("runescape-deob3.jar");
	}

	private void addTransform(Transform t, int runLevel) {
		classTransforms.put(t, runLevel);
	}

	private void transformClasses() {
		classTransforms = new LinkedHashMap<>();

		addTransform(new DuplicatesTransform(), 0);
		addTransform(new ClientRemappingMiner(), 0);

		int MAX_RUN_LEVEL = 1;
		for (int i = 0; i < MAX_RUN_LEVEL; i++) {
			for (TransformClassNode tcn : rsClassLoader.classMap.values()) {
				for (Entry<Transform, Integer> entry : classTransforms
						.entrySet()) {
					if (entry.getValue() != i)
						continue;

					entry.getKey().accept(tcn);
				}
			}
		}
	}

	private void loadClasses(String jarFile) {
		try {
			rsClassLoader = new RsClassLoader(jarFile);
			rsClassLoader.enableRenamer(false);
			rsClassLoader.loadAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeClasses(String output) {
		try {
			rsClassLoader.dumpJar(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Main();
	}
}
