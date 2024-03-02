/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

/**
 * This interface represents a generic runtime joinpoint (in the AOP
 * terminology).
 *
 * <p>A runtime joinpoint is an <i>event</i> that occurs on a static
 * joinpoint (i.e. a location in a the program). For instance, an
 * invocation is the runtime joinpoint on a method (static joinpoint).
 * The static part of a given joinpoint can be generically retrieved
 * using the {@link #getStaticPart()} method.
 *
 * <p>In the context of an interception framework, a runtime joinpoint
 * is then the reification of an access to an accessible object (a
 * method, a constructor, a field), i.e. the static part of the
 * joinpoint. It is passed to the interceptors that are installed on
 * the static joinpoint.
 *
 * @author Rod Johnson
 * @see Interceptor
 */

/**
 * 连接点。在拦截器中使用，封装了原方法调用的相关信息，如参数、原对象信息，以及直接调用原方法的proceed方法。
 *
 * 程序执行的某个特定位置，比如某个方法调用前、调用后，方法抛出异常后
 * 一个类或一段程序代码拥有一些具有边界性质的特定点，这些代码中的特定点就是连接点
 * AOP中的Joinpoint可以有多种类型：构造方法调用，字段的设置和获取，方法的调用，方法的执行，异常的处理执行，类的初始化
 * Spring仅支持方法执行类型的Joinpoint。
 *
 * 程序在执行过程中一个运行时Joinpoint，在这些点关联的静态位置通常会安装有一些Interceptor，
 * 当程序运行到这个运行时Joinpoint时，AOP框架会拦截运行时Joinpoint的执行，
 * 把运行时Joinpoint交给已经安装的Interceptor们进行处理
 * 1、连接点（Join Point），在应用执行过程中能够插入切面的一个点
 * 在 Spring AOP 中, join point 总是【方法的执行点】, 即只有【方法连接点】
 * 2、切点（point cut）：匹配连接点的谓词，一组连接点的总称，用于指定某个增强应该在何时被调用，
 * 提供一组规则来匹配连接点，给满足规则的连接点添加Advice
 */
public interface Joinpoint {

	/**
	 * Proceed to the next interceptor in the chain.
	 * <p>The implementation and the semantics of this method depends
	 * on the actual joinpoint type (see the children interfaces).
	 * @return see the children interfaces' proceed definition
	 * @throws Throwable if the joinpoint throws an exception
	 */
	//该方法用于执行拦截器逻辑；
	Object proceed() throws Throwable;

	/**
	 * Return the object that holds the current joinpoint's static part.
	 * <p>For instance, the target object for an invocation.
	 * @return the object (can be null if the accessible object is static)
	 */
	//返回保持当前连接点的静态部分的对象；
	Object getThis();

	/**
	 * Return the static part of this joinpoint.
	 * <p>The static part is an accessible object on which a chain of
	 * interceptors are installed.
	 */
	//返回此连接点的静态部分（通常包含构造函数，成员变量，方法等信息）
	AccessibleObject getStaticPart();

}
