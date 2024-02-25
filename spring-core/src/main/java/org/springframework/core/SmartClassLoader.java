/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Interface to be implemented by a reloading-aware ClassLoader
 * (e.g. a Groovy-based ClassLoader). Detected for example by
 * Spring's CGLIB proxy factory for making a caching decision.
 * <p>要由可重新加载的ClassLoader(例如，基于Groovy的ClassLoader)
 * 实现的接口。例如，由Spring的CGLIB代理工厂检测到是否做出了缓存
 * 决定
 *
 * <p>If a ClassLoader does <i>not</i> implement this interface,
 * then all of the classes obtained from it should be considered
 * as not reloadable (i.e. cacheable).
 * <p>如果ClassLoader没有实现该接口，则从该接口获得的所有类都应
 * 被视为无法重载（即可缓存）</p>
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public interface SmartClassLoader {

	/**
	 * Determine whether the given class is reloadable (in this ClassLoader).
	 *  <p>确定给定的类是否可重载（在此ClassLoader中）</p>
	 * <p>Typically used to check whether the result may be cached (for this
	 * ClassLoader) or whether it should be reobtained every time.
	 * <p>通常用于检查结果是否可以缓存（针对此ClassLoader)或是应重新获取</p>
	 * @param clazz the class to check (usually loaded from this ClassLoader)
	 *               - 要检查的类（通常从此ClassLoaser加载）
	 * @return whether the class should be expected to appear in a reloaded
	 * version (with a different {@code Class} object) later on
	 * - 以后是否应该期望该类出现在重新加载的版本中(具有不同的Class对象)
	 */
	boolean isClassReloadable(Class<?> clazz);

}
