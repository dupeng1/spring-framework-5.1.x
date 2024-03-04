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

package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * Convenient superclass when we want to force subclasses to
 * implement MethodMatcher interface, but subclasses
 * will want to be pointcuts. The getClassFilter() method can
 * be overridden to customize ClassFilter behaviour as well.
 *
 * @author Rod Johnson
 */

/**
 * 动态方法匹配器切点。它本质上是一个方法匹配器，但同时具有了切点的功能。
 * 动态匹配的Pointcut；
 */
public abstract class DynamicMethodMatcherPointcut extends DynamicMethodMatcher implements Pointcut {

	@Override
	public ClassFilter getClassFilter() {
		// 匹配任意类
		return ClassFilter.TRUE;
	}

	@Override
	public final MethodMatcher getMethodMatcher() {
		return this;
	}

}
