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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * A {@code TargetSource} is used to obtain the current "target" of
 * an AOP invocation, which will be invoked via reflection if no around
 * advice chooses to end the interceptor chain itself.
 *
 * <p>If a {@code TargetSource} is "static", it will always return
 * the same target, allowing optimizations in the AOP framework. Dynamic
 * target sources can support pooling, hot swapping, etc.
 *
 * <p>Application developers don't usually need to work with
 * {@code TargetSources} directly: this is an AOP framework interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */

/**
 * 目标对象
 * 包含连接点的对象。也被称作【被通知】或【被代理】对象。
 * TargetSource（目标源）是被代理的target（目标对象）实例的来源。
 *
 *   TargetSource被用于获取当前MethodInvocation（方法调用）所需要的target（目标对象），这个target通过反射的方式被调用（如：method.invode(target,args)）。换句话说，proxy（代理对象）代理的不是target，而是TargetSource。
 *   为什么Spring AOP代理不直接代理target，而需要通过代理TargetSource（target的来源，其内部持有target），间接代理target呢？
 *   通常情况下，一个proxy（代理对象）只能代理一个target，每次方法调用的目标也是唯一固定的target。但是，如果让proxy代理TargetSource，可以使得每次方法调用的target实例都不同（当然也可以相同，这取决于TargetSource实现）。这种机制使得方法调用变得灵活，可以扩展出很多高级功能，如：target pool（目标对象池）、hot swap（运行时目标对象热替换）等等。
 *   TargetSource组件本身与Spring IoC容器无关，target的生命周期不一定是受spring容器管理的，我们以往的XML中的AOP配置，只是对受容器管理的bean而言的，我们当然可以手动创建一个target，同时使用Spring的AOP框架（而不使用IoC容器）
 *
 * TargetSource包含4个简单实现和3大类实现。
 * 4个简单实现包括：
 * （1）EmptyTargetSource：静态目标源，当不存在target目标对象，或者甚至连targetClass目标类都不存在（或未知）时，使用此类实例。
 *
 * （2）HotSwappableTargetSource：动态目标源，支持热替换的目标源，支持spring应用运行时替换目标对象。
 *
 * （3）JndiObjectTargetSource：spring对JNDI管理bean的支持，static属性可配置。
 *
 * （4）SingletonTargetSource：静态目标源，单例目标源。Spring的AOP框架默认为受IoC容器管理的bean创建此目标
 *
 * 3大类实现包括：
 * （1）AbstractBeanFactoryBasedTargetSource：此类目标源基于IoC容器实现，也就是说target目标对象可以通过beanName从容器中获取。此类又扩展出：① SimpleBeanTargetSource：简单实现，直接调用getBean从容器获取目标对象；② LazyInitTargetSource：延迟初始化目标源，子类可重写postProcessTargetObject方法后置处理目标对象；③AbstractPrototypeBasedTargetSource：原型bean目标源，此抽象类可确保beanName对应的bean的scope属性为prototype。其子类做了简单原型、池化原型、线程隔离原型这3种实现。
 *
 * （2）AbstractRefreshableTargetSource：可刷新的目标源。此类实现可根据配置的刷新延迟时间，在每次获取目标对象时自动刷新目标对象。
 *
 * （3）AbstractLazyCreationTargetSource：此类实现在调用getTarget()获取时才创建目标对象
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * Return the type of targets returned by this {@link TargetSource}.
	 * <p>Can return {@code null}, although certain usages of a {@code TargetSource}
	 * might just work with a predetermined target class.
	 * @return the type of targets returned by this {@link TargetSource}
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 * @return the target object which contains the joinpoint,
	 * or {@code null} if there is no actual target instance
	 * @throws Exception if the target object can't be resolved
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * Release the given target object obtained from the
	 * {@link #getTarget()} method, if any.
	 * @param target object obtained from a call to {@link #getTarget()}
	 * @throws Exception if the object can't be released
	 */
	void releaseTarget(Object target) throws Exception;

}
