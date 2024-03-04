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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator should be registered yet multiple concrete
 * implementations are available. This class provides a simple escalation protocol,
 * allowing a caller to request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see AopNamespaceUtils
 */

/**
 * 这个是关于AOP配置的工具类。因为配置AOP的方式有多种（比如xml、注解等），
 * 此工具类就是针对不同配置，提供不同的工具方法的。它的好处是不管什么配置，最终走底层逻辑都归一了。
 */
public abstract class AopConfigUtils {

	/**
	 * The bean name of the internally managed auto-proxy creator.
	 */
	public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";

	/**
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

	static {
		// Set up the escalation list...
		/** 默认初始化3个APC（AutoProxyCreator）实现类 */
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);// 第0级别
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);// 第1级别
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);// 第2级别
	}


	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}

	//用于注册或者升级AnnotationAwareAspectJAutoProxyCreator类型的APC
	//对于AOP的实现，基本都是在AnnotationAwareAspectJAutoProxyCreator类中完成的
	//它可以根据@Point注解定义的切点来自动代理相匹配的bean
	//为了配置简便，Spring使用了自定义配置来帮助我们自动注册AnnotationAwareAspectJAutoProxyCreator
	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}

	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
		}
	}

	public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
		}
	}

	@Nullable
	private static BeanDefinition registerOrEscalateApcAsRequired(
			Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		//如果已经存在了【自动代理创建器】且存在的自动代理创建器与现在的不一致，
		/** 步骤1：如果容器中已经存在apc实例，则试图替换为AnnotationAwareAspectJAutoProxyCreator类型 */
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			//如果已注册的 `internalAutoProxyCreator` 和入参的 Class 不相等，说明可能是继承关系
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				//获取已注册的 `internalAutoProxyCreator` 的优先级
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				//获取需要注册的 `internalAutoProxyCreator` 的优先级
				int requiredPriority = findPriorityForClass(cls);
				// InfrastructureAdvisorAutoProxyCreator < AspectJAwareAdvisorAutoProxyCreator < AnnotationAwareAspectJAutoProxyCreator
				// 三者都是 AbstractAutoProxyCreator 自动代理对象的子类
				if (currentPriority < requiredPriority) {
					//改变bean最重要的就是改变bean所对应的className属性
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			//如果已经存在自动代理创建器且与将要创建的一致，那么无需再次创建
			return null;
		}
		/** 步骤2：如果不存在，则向容器中创建AnnotationAwareAspectJAutoProxyCreator类型的apc实例对象 */
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		//设置来源
		beanDefinition.setSource(source);
		//设置为最高优先级
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		//设置角色为**ROLE_INFRASTRUCTURE**，表示是 Spring 框架内部的 Bean
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		//注册自动代理的 Bean，名称为 `org.springframework.aop.config.internalAutoProxyCreator`
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		//返回刚注册的 RootBeanDefinition 对象
		return beanDefinition;
	}

	/** 通过Class获得优先级别 */
	private static int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}

	/** 通过className获得优先级别 */
	private static int findPriorityForClass(@Nullable String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
