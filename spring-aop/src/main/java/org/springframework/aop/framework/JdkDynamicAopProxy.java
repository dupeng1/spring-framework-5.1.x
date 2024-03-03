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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */

/**
 * 生成jdk动态代理。可以看到这个类本身就是一个InvocationHandler，
 * 这意味着当调用代理对象中的方法时，最终会调用到JdkDynamicAopProxy的invoke方法。
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance. (We have a good test suite to ensure that the different
	 * proxies behave the same :-)
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */

	/** We use a static Log to avoid serialization issues. */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/** Config used to configure this proxy. */
	// 代理的配置信息
	private final AdvisedSupport advised;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 */
	// 代理的接口是否定义equals方法
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 */
	// 代理的接口是否定义hashCode方法
	private boolean hashCodeDefined;


	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 * exception in this case, rather than let a mysterious failure happen later.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		// 校验AdvisedSupport
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			// 如果Advisor和`TargetSource`都不存在，则无法创建代理
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		//AdvisedSupport类是Aop代理的配置管理类，里面包括了代理的对象和被代理对象需要织入的通知Advice
		this.advised = config;
	}


	@Override
	public Object getProxy() {
		// 使用默认地类加载器，通常是线程上下文类加载器
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	/**
	 *  1、创建目标类（委托类）的接口
	 *  2、创建目标类（委托类）
	 *  3、定义一个代理类的调用处理程序。该程序必须实现接口InvocationHandler，且必须实现接口的invoke方法。
	 *  4、通过Proxy的静态方法newProxyInstance()创建一个代理对象。
	 *  5、通过代理对象调用委托类对象的方法。
	 *  其实Proxy类只是一个连接桥，把代理（InvocationHandler）与被代理类关联起来，真正处理事情的是InvocaHandler。
	 *  InvocationHandler接口中的invoke方法在代理类中是动态实现的，
	 *  当我们通过动态代理调用一个方法的时候，这个方法的调用会被转发到到调用处理程序的invoke方法中。
	 * @param classLoader the class loader to create the proxy with
	 * (or {@code null} for the low-level proxy facility's default)
	 * @return
	 */
	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}
		// 获取生成代理所需要实现的接口
		Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		// 查找当前所有的需要代理的接口，看看 是否有
		// equals 方法 和 hashcode 方法，如果有，就打个标记。
		findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
		// classLoader ：类加载器
		// proxiedInterfaces ：生成的代理类 需要 实现的接口集合
		// this : JdkDynamicAopProxy，该类实现了InvocationHandler 接口
		// 该方法最终会返回一个 代理类 对象。
		return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
	}

	/**
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
	 * on the supplied set of interfaces.
	 * @param proxiedInterfaces the interfaces to introspect
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;
		// 通过this.advised得到代理的目标类，
		TargetSource targetSource = this.advised.targetSource;
		// 真正的target 的一个引用
		Object target = null;

		try {
			// 如何代理接口没有定义equals方法，则调用JdkDynamicAopProxy的重写的equals方法
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
				return equals(args[0]);
			}
			// 如何代理接口没有定义equals方法，则调用JdkDynamicAopProxy的重写的hashCode方法
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
				return hashCode();
			}
			// 也就是说我们调用的是DecoratingProxy这个接口中的方法
			// 这个接口中只定义了一个getDecoratedClass方法，用于获取到最终的目标对象，
			// 在方法实现中会通过一个while循环来不断接近最终的目标对象，直到得到的目标对象不是一个被代理的对象才会返回
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			//如果执行的方法是来自于接口的，并且方法属于的类还是一个Advised.class
			//证明了这是代理对象的代理对象（即被代理对象是一个代理对象，还实现了接口）
			//opaque-->标记是否需要阻止通过该配置创建的代理对象转换为Advised类型，默认值为false，表示代理对象可以被转换为Advised类型
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;
			//有时候目标对象内部的自我调用将无法实施切面中的增强，则需要通过此属性暴露代理
			//也就是说，代理了一个对象，并且去执行方法，如果方法里面又调用了内部的方法是会失效的
			//这是属于JDK动态代理的问题
			//Spring通过暴露oldProxy的方法去解决这个问题
			//如果配置允许暴露代理
			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				//通过AopContext去获取之前的代理对象
				//并且会更新现在的代理对象
				//此时oldProxy就是上一个的代理对象，暴露了代理出来
				oldProxy = AopContext.setCurrentProxy(proxy);
				//标志暴露了Proxy
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			// 根据targetSource拿到真正的目标对象
			target = targetSource.getTarget();
			// 获取到目标对象的 class
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
			//获取所有的增强器和拦截器
			//所有的增强器和拦截器相当于形成了一条链，下面简称为拦截器链吧
			//反正增强器跟拦截器作用类似，都是拦截下来做额外动作的
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			// 如果拦截链为空
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				//直接调用被代理的方法，也就是切点的方法
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			// 说明有匹配当前method的方法拦截器，所以要做增强处理了。
			else {
				// We need to create a method invocation...
				// 否则创建 MethodInvocation 执行拦截调用
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				//执行拦截器链
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			// 方法 返回值类型
			Class<?> returnType = method.getReturnType();
			//如果返回值为null，并且返回类型不是void类型，并且返回类型还被定义为基本数据类型
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			//如果暴露过proxyContext
			if (setProxyContext) {
				// Restore old proxy.
				//要进行换回来
				//因为oldProxy记录了之前旧的Proxy
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		}
		else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
