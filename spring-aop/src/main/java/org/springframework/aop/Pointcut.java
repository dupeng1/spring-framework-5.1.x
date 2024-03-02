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

package org.springframework.aop;

/**
 * Core Spring pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */

/**
 * 切点。切点的主要作用是定义通知所要应用到的类跟方法。
 * 具体的哪些类、哪些方法由ClassFilter和MethodMatcher匹配，只有满足切入点的条件时才插入advice。
 *
 * 	AnnotationMatchingPointcut：注解匹配切点。根据类上或方法上是否存在指定的注解判断切点的匹配性，如果没有显示指定注解，则匹配所有。
 * 	DynamicMethodMatcherPointcut：动态方法匹配器切点。它本质上是一个方法匹配器，但同时具有了切点的功能。
 * 	ComposablePointcut：可组合的切点。这种切点可以与或逻辑，任意组合其他的Pointcut、ClassFilter和MethodMatcher。其本质是通过ClassFilters和MethodMatchers两个工具类进行Pointcut内部组件的组合。
 * 	JdkRegexpMethodPointcut： JDK正则表达式切点，即使用正则表达式描述方法的拦截规则和排除规则。
 * 	AspectJExpressionPointcut：AspectJ切点表达式切点。顾名思义，使用AspectJ的切点表达式描述筛选规则。表达式基本语法如下（非完整语法）：execution(<方法修饰符>? <方法返回值类型> <包名>.<类名>.<方法名>(<参数类型>) [throws <异常类型>]?)
 *
 *
 * 如果连接点相当于数据中的记录，那么切点相当于查询条件，一个切点可以匹配多个连接点
 * 所以切点表示一组Joinpoint，这些Jointpoint或是通过逻辑关系组合起来，
 * 或是通过通配、正则表达式等方式集中起来，它定义了相应的 Advice 将要发生的地方
 *
 *
 */
public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 * @return the ClassFilter (never {@code null})
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 * @return the MethodMatcher (never {@code null})
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * Canonical Pointcut instance that always matches.
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
