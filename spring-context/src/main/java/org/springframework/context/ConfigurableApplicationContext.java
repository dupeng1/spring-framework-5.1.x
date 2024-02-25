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

package org.springframework.context;

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.lang.Nullable;

/**
 * SPI interface to be implemented by most if not all application contexts.
 * Provides facilities to configure an application context in addition
 * to the application context client methods in the
 * {@link org.springframework.context.ApplicationContext} interface.
 *
 * <p>Configuration and lifecycle methods are encapsulated here to avoid
 * making them obvious to ApplicationContext client code. The present
 * methods should only be used by startup and shutdown code.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 03.11.2003
 */
/*
这个接口是被大多数但并非全部应用程序上下文实现的服务提供接口。除了ApplicationContext接口中的应用程序上下文客户端方法以外，
还提供了配置应用程序上下文的工具。
这里封装了配置和生命周期方法，使ApplicationContext客户端对它们不可见，目前的方法只能由启动和关闭代码使用。
*/

public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * Any number of these characters are considered delimiters between
	 * multiple context config paths in a single String value.
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	/*
    这个字符串中的任意字符都可以作为单个字符串值中多个上下文配置路径之间的分隔符
    */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * Name of the ConversionService bean in the factory.
	 * If none is supplied, default conversion rules apply.
	 * @since 3.0
	 * @see org.springframework.core.convert.ConversionService
	 */
	 /*
    factory中ConversionService bean的名称，如果没有提供将才有默认的转换规则
    */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * Name of the LoadTimeWeaver bean in the factory. If such a bean is supplied,
	 * the context will use a temporary ClassLoader for type matching, in order
	 * to allow the LoadTimeWeaver to process all actual bean classes.
	 * @since 2.5
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 */
	 /*
    factory中LoadTimeWeaver bean的名称，如果提供了这样的bean，
    context将使用一个临时类加载器进行类型匹配，以允许LoadTimeWeaver处理所有实际的bean类
    */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * Name of the {@link Environment} bean in the factory.
	 * @since 3.1
	 */
	/*
    factory中Environment bean的名称
    */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * Name of the System properties bean in the factory.
	 * @see java.lang.System#getProperties()
	 */
	/*
    系统属性bean的名称
    */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * Name of the System environment bean in the factory.
	 * @see java.lang.System#getenv()
	 */
	 /*
    系统环境bean的名称
    */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";


	/**
	 * Set the unique id of this application context.
	 * @since 3.0
	 */
	 /*
    设置应用程序上下文的唯一ID
    */
	void setId(String id);

	/**
	 * Set the parent of this application context.
	 * <p>Note that the parent shouldn't be changed: It should only be set outside
	 * a constructor if it isn't available when an object of this class is created,
	 * for example in case of WebApplicationContext setup.
	 * @param parent the parent context
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	/*
    设置应用程序上下文的父Context
    请注意，父类不应该被修改，只有在创建此类的对象时父级不可用时，才应在构造函数外部设置父级
    */
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * Set the {@code Environment} for this application context.
	 * @param environment the new environment
	 * @since 3.1
	 */
	/*
    设置应用程序上下文的环境
    */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * @since 3.1
	 */
	/*
    获得应用程序上下文的环境
    */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * Add a new BeanFactoryPostProcessor that will get applied to the internal
	 * bean factory of this application context on refresh, before any of the
	 * bean definitions get evaluated. To be invoked during context configuration.
	 * @param postProcessor the factory processor to register
	 */
	/*
    增加一个新的BeanFactoryPostProcessor，在任何bean definition被检查之前，
    它将会在当前应用程序上下文刷新的时候将会应用于内部的bean工厂

    在进行context配置期间调用
    */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * Add a new ApplicationListener that will be notified on context events
	 * such as context refresh and context shutdown.
	 * <p>Note that any ApplicationListener registered here will be applied
	 * on refresh if the context is not active yet, or on the fly with the
	 * current event multicaster in case of a context that is already active.
	 * @param listener the ApplicationListener to register
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	/*
    添加一个ApplicationListener，它将会被通知上下文事件
    例如context刷新和关闭事件
    注意，即使上下文没有启动，注册在这里的ApplicationListener也会被应用与刷新事件
    或者在上下文已经激活的情况下，使用当前事件多播进行实时处理
    */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * Register the given protocol resolver with this application context,
	 * allowing for additional resource protocols to be handled.
	 * <p>Any such resolver will be invoked ahead of this context's standard
	 * resolution rules. It may therefore also override any default rules.
	 * @since 4.3
	 */
	/*
    注册应用程序上下文中给定的协议解析器
    允许额外的资源协议被处理
    任何这样的解析器都将在这个上下文的标准解析规则之前被调用。
    因此，它也可以覆盖任何默认规则
    */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * Load or refresh the persistent representation of the configuration,
	 * which might an XML file, properties file, or relational database schema.
	 * <p>As this is a startup method, it should destroy already created singletons
	 * if it fails, to avoid dangling resources. In other words, after invocation
	 * of that method, either all or no singletons at all should be instantiated.
	 * @throws BeansException if the bean factory could not be initialized
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	/*
    加载或者刷新配置，这些配置可能来自基于Java注解的配置，或者xml文件，或者属性文件，或者关联的数据库等等
	在应用上下文关闭的情况下调用refresh即可启动应用上下文，在已经启动的状态下条用refresh则可清除缓存并重新装载配置信息
    */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * Register a shutdown hook with the JVM runtime, closing this context
	 * on JVM shutdown unless it has already been closed at that time.
	 * <p>This method can be called multiple times. Only one shutdown hook
	 * (at max) will be registered for each context instance.
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	/*
    注册JVM运行时的关闭钩子，在JVM关闭时关闭应用程序上下文，除非它已经关闭
    这个方法可以多次调用，每个context实例仅注册一个关闭钩子
    */
	void registerShutdownHook();

	/**
	 * Close this application context, releasing all resources and locks that the
	 * implementation might hold. This includes destroying all cached singleton beans.
	 * <p>Note: Does <i>not</i> invoke {@code close} on a parent context;
	 * parent contexts have their own, independent lifecycle.
	 * <p>This method can be called multiple times without side effects: Subsequent
	 * {@code close} calls on an already closed context will be ignored.
	 */
	/*
    关闭应用程序上下文，释放所有的资源，同时会销毁单例池中的所有bean
    */
	@Override
	void close();

	/**
	 * Determine whether this application context is active, that is,
	 * whether it has been refreshed at least once and has not been closed yet.
	 * @return whether the context is still active
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	/*
    判断当前应用程序上下文是否在使用
    判断它是否至少被刷新一次并且没有被关闭
    */
	boolean isActive();

	/**
	 * Return the internal bean factory of this application context.
	 * Can be used to access specific functionality of the underlying factory.
	 * <p>Note: Do not use this to post-process the bean factory; singletons
	 * will already have been instantiated before. Use a BeanFactoryPostProcessor
	 * to intercept the BeanFactory setup process before beans get touched.
	 * <p>Generally, this internal factory will only be accessible while the context
	 * is active, that is, in-between {@link #refresh()} and {@link #close()}.
	 * The {@link #isActive()} flag can be used to check whether the context
	 * is in an appropriate state.
	 * @return the underlying bean factory
	 * @throws IllegalStateException if the context does not hold an internal
	 * bean factory (usually if {@link #refresh()} hasn't been called yet or
	 * if {@link #close()} has already been called)
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	/*
    返回当前应用程序上下文的内部bean工厂，可以用于访问指定的功能
    不要用它来对bean工厂进行后期处理；单例之前已经实例化过了。
    使用BeanFactoryPostProcessor在接触Bean之前拦截BeanFactory启动过程
    */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
