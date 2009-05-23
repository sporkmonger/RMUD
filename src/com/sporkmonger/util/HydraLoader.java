package com.sporkmonger.util;

import java.util.ArrayList;

public class HydraLoader extends ClassLoader {
	private ArrayList<ClassLoader> childLoaders = new ArrayList<ClassLoader>();

	public HydraLoader() {
		super();
		childLoaders.add(this.getParent());
	}

	public HydraLoader(ClassLoader parent) {
		super(parent);
		childLoaders.add(this.getParent());
	}
	
	public void addLoader(ClassLoader loader) {
		childLoaders.add(loader);
	}
	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (ClassLoader loader : childLoaders) {
			try {
				return loader.loadClass(name);
			} catch (ClassNotFoundException e) {
			}
		}
		throw new ClassNotFoundException("FAIL!");
	}
}
