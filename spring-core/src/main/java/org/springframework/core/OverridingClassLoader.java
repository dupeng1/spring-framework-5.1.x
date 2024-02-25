/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * {@code ClassLoader} that does <i>not</i> always delegate to the parent loader
 * as normal class loaders do. This enables, for example, instrumentation to be
 * forced in the overriding ClassLoader, or a "throwaway" class loading behavior
 * where selected application classes are temporarily loaded in the overriding
 * {@code ClassLoader} for introspection purposes before eventually loading an
 * instrumented version of the class in the given parent {@code ClassLoader}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0.1
 */

/**
 * 并不总是像普通的类加载器一样委托给父加载器。例如这使得可以
 *  覆盖的ClassLoader中强制执行检查，或者执行'丢弃'类的加载行为。在此行为中，处于自省目的，
 *  以进行自省，然后最终将类的检查版本加载到给定的父级的ClassLoader
 */
public class OverridingClassLoader extends DecoratingClassLoader {

	/** Packages that are excluded by default. */
	//默认情况下排除的软件包
	public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]
			{"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

	private static final String CLASS_FILE_SUFFIX = ".class";

	static {
		// 通过该方法，可以使得ClassLoader执行并行加载机制，提高加载效率。
		ClassLoader.registerAsParallelCapable();
	}


	@Nullable
	private final ClassLoader overrideDelegate;


	/**
	 * Create a new OverridingClassLoader for the given ClassLoader.
	 * @param parent the ClassLoader to build an overriding ClassLoader for
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent) {
		this(parent, null);
	}

	/**
	 * Create a new OverridingClassLoader for the given ClassLoader.
	 * @param parent the ClassLoader to build an overriding ClassLoader for
	 * @param overrideDelegate the ClassLoader to delegate to for overriding
	 * @since 4.3
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent, @Nullable ClassLoader overrideDelegate) {
		super(parent);
		this.overrideDelegate = overrideDelegate;
		for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
			excludePackage(packageName);
		}
	}

	//reslove为true时，会对指定类优先执行链接功能。默认情况下是false
	//ClassLoad的链接功能会调用resolveClass(Class<?>)，
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
			return this.overrideDelegate.loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (isEligibleForOverriding(name)) {
			Class<?> result = loadClassForOverriding(name);
			if (result != null) {
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * Determine whether the specified class is eligible for overriding
	 * by this class loader.
	 * @param className the class name to check
	 * @return whether the specified class is eligible
	 * @see #isExcluded
	 */
	//确定指定的类是否适合该类加载器加载
	protected boolean isEligibleForOverriding(String className) {
		//只要没有添加到 excludedPackages 或 excludedClasses的类都适合加载
		return !isExcluded(className);
	}

	/**
	 * Load the specified class for overriding purposes in this ClassLoader.
	 * <p>在此ClassLoader中加载指定的类以进行覆盖</p>
	 * <p>The default implementation delegates to {@link #findLoadedClass},
	 * {@link #loadBytesForClass} and {@link #defineClass}.
	 * <p>默认实现将委托给{@link #findLoadedClass}，{@link #loadBytesForClass},和
	 * {@link #defineClass}</p>
	 * @param name the name of the class - 类名
	 * @return the Class object, or {@code null} if no class defined for that name
	 * - Class对象，如果没有为该名称定义任何类，则为{@code null}
	 * @throws ClassNotFoundException if the class for the given name couldn't be loaded
	 * - 如果给定名称的类无法加载
	 */
	@Nullable
	protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
		//看看该类名是否已经加载过
		Class<?> result = findLoadedClass(name);
		//如果没有加载过
		if (result == null) {
			//加载给定类名的字节
			byte[] bytes = loadBytesForClass(name);
			//如果加载成功
			if (bytes != null) {
				//根据字节构建类对象
				result = defineClass(name, bytes, 0, bytes.length);
			}
		}
		return result;
	}

	/**
	 * Load the defining bytes for the given class,
	 * to be turned into a Class object through a {@link #defineClass} call.
	 * <p>加载给定类的定义字节，以通过defineClass调用将其转换为Class对象</p>
	 * <p>The default implementation delegates to {@link #openStreamForClass}
	 * and {@link #transformIfNecessary}.
	 * <p>默认实现将委托给{@link #openStreamForClass}和{@link #transformIfNecessary}</p>
	 * @param name the name of the class - 类名
	 * @return the byte content (with transformers already applied),
	 * or {@code null} if no class defined for that name
	 * - 字节内容（已经应用了转换器），如果没有该名称定义类，则为{@code null}
	 * @throws ClassNotFoundException if the class for the given name couldn't be loaded
	 * - 如果给定名称的类无法加载
	 */
	@Nullable
	protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
		//打开指定类的InputStream。默认实现通过父ClassLoader的getResourceAsStream方法加载标准类文件。
		InputStream is = openStreamForClass(name);
		//如果输入流打不开，返回null
		if (is == null) {
			return null;
		}
		try {
			// Load the raw bytes.
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			// Transform if necessary and use the potentially transformed bytes.
			//必须是进行转换，并使用转换后的字节，钩子方法，默认直接返回参数bytes
			return transformIfNecessary(name, bytes);
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * Open an InputStream for the specified class.
	 * <p>打开指定类的InputStream。</p>
	 * <p>The default implementation loads a standard class file through
	 * the parent ClassLoader's {@code getResourceAsStream} method.
	 * <p>默认实现通过父ClassLoader的getResourceAsStream方法加载标准类文件。</p>
	 * @param name the name of the class - 类名
	 * @return the InputStream containing the byte code for the specified class
	 * - 包含指定类的字节码的InputStream
	 */
	@Nullable
	protected InputStream openStreamForClass(String name) {
		//将给定类名的'.'覆盖成'/'后，后面拼接上'.class'，以形成类路径
		String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
		//通过父ClassLoader的getResourceAsStream方法加载标准类文件。
		return getParent().getResourceAsStream(internalName);
	}


	/**
	 * Transformation hook to be implemented by subclasses.
	 * <p>转换将由子类实现，钩子方法</p>
	 * <p>The default implementation simply returns the given bytes as-is.
	 * <p>默认实现只是按原样返回给定的字节</p>
	 * @param name the fully-qualified name of the class being transformed
	 *             -- 要转换的类的全限定名称
	 * @param bytes the raw bytes of the class
	 *              -- 类的原始字节
	 * @return the transformed bytes (never {@code null};
	 * same as the input bytes if the transformation produced no changes)
	 * -- 转换后的字节（从不为{@code null}；如果转换没有变化，则与传入的字节相同）
	 */
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return bytes;
	}

}
