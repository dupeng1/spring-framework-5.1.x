/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop;

/**
 * Filter that restricts matching of a pointcut or introduction to
 * a given set of target classes.
 *
 * <p>Can be used as part of a {@link Pointcut} or for the entire
 * targeting of an {@link IntroductionAdvisor}.
 *
 * <p>Concrete implementations of this interface typically should provide proper
 * implementations of {@link Object#equals(Object)} and {@link Object#hashCode()}
 * in order to allow the filter to be used in caching scenarios &mdash; for
 * example, in proxies generated by CGLIB.
 *
 * @author Rod Johnson
 * @see Pointcut
 * @see MethodMatcher
 */

/**
 *主要作用是在类级别上对通知的应用进行一次过滤，如果它的match方法对任意的类都返回true的话，说明在类级别上我们不需要过滤，
 * 这种情况下，通知的应用，就完全依赖MethodMatcher的匹配结果。ClassFilter有4中简单方式的实现：
 *     （1）TypePatternClassFilter：基于AspectJ的类型匹配实现；
 *         （2）AnnotationClassFilter：通过检查目标类是否存在指定的注解，决定是否匹配；
 *         （3）RootClassFilter：通过判断目标类是否是指定类型（或其子类型），决定是否匹配；
 *         （4）TrueClassFilter：这是最简单实现，matches方法总会返回true。此类设计使用了单例模式，且其对象引用直接被在ClassFilter接口中声明成了常量。
 */
@FunctionalInterface
public interface ClassFilter {

	/**
	 * Should the pointcut apply to the given interface or target class?
	 * @param clazz the candidate target class
	 * @return whether the advice should apply to the given target class
	 */
	// 是否匹配指定的接口/类
	boolean matches(Class<?> clazz);


	/**
	 * Canonical instance of a ClassFilter that matches all classes.
	 */
	// 匹配任意类型的示例
	ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
