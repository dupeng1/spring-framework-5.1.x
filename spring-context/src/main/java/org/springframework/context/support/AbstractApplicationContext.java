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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */

/**
 * 1、一个抽象应用程序上下文，主要实现了上下文初始化的功能，如：定义了初始化BeanFactory，初始化一些BeanFactoryPostProcessors，
 * 初始化MessageSource（国际化），还要一些事件广播器ApplicationEventMulticaster等等
 * 2、因为是抽象类，其实其中一些初始化操作还是交个子类实现，如初始化BeanFactory，在AbstractApplicationContext只是做了简单的定义，
 * 具体实现还是交个子类。子类根据实际情况选择使用那种BeanFactory并进行初始化
 * 3、是一个比BeanFactory更具丰富IOC容器接口，而AbstractApplicationContext是其的一个抽象实现，定义和实现了一系列的初始化流程
 * 4、作用如下
 * 		1、对属性的getter和setter方法
 * 		2、进行事件的发布，通过applicationEventMulticaster进行事件的发布。
 * 		3、对应用程序上下文进行刷新，在刷新的过程中：改变了context的状态，
 * 		初始化了messageSource和applicationEventMulticaster，注册了一系列的监听者，
 * 		完成了spring中bean的创建等，refresh方法是重点。
 * 		4、可以通过context获得对应beanFactory，然后通过这个bean工厂来查询bean工厂中缓存的各种bean，
 * 		还可以判断bean的生命周期，通过类型获取对应的bean，获取其中的BeanDefinition
 * 		5、注册了一系列的BeanFactoryPostProcessor，
 * 		并在refresh()方法执行过程中通过调用PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors
 * 		执行了我们注册的BeanFactoryPostProcessor
 * 		6、通过LifecycleProcessor对beans的生命周期进行管理
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Unique id for this context, if any. */
	/**	应用上下文的id，组成形式为：classname + '@' + 对象的hashCode*/
	private String id = ObjectUtils.identityToString(this);

	/** Display name. */
	// 应用环境上下文的名称，和id的值相同，两者调用了同样的方法传递了同样的参数
	private String displayName = ObjectUtils.identityToString(this);

	/** Parent context. */
	// 这个应用上下文的父context
	@Nullable
	private ApplicationContext parent;

	/** Environment used by this context. */
	// 当前上下文使用的Environment对象，用于设置激活环境中的哪些配置文件，查询系统的环境属性等
	@Nullable
	private ConfigurableEnvironment environment;

	/** BeanFactoryPostProcessors to apply on refresh. */
	// 存储在刷新应用程序上下文时使用的BeanFactoryPostProcessor
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/** System time in milliseconds when this context started. */
	// context的启动时间，以毫秒为单位
	private long startupDate;

	/** Flag that indicates whether this context is currently active. */
	// 标识当前上下文是否处于活跃状态
	private final AtomicBoolean active = new AtomicBoolean();

	/** Flag that indicates whether this context has been closed already. */
	// 标识当前上下文是否已经被关闭
	private final AtomicBoolean closed = new AtomicBoolean();

	/** Synchronization monitor for the "refresh" and "destroy". */
	// 刷新和关闭的同步监听器，类似于锁
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered. */
	// JVM的关闭钩子
	@Nullable
	private Thread shutdownHook;

	/** ResourcePatternResolver used by this context. */
	// 该接口使用了策略模式来封装算法，调用其getResources方法来获取资源，但是方法是在其实现类实现的
	private ResourcePatternResolver resourcePatternResolver;

	/** LifecycleProcessor for managing the lifecycle of beans within this context. */
	// 策略接口，管理应用程序上下文中bean的生命周期，用于通知context刷新和关闭，然后自动开启和关闭context包含的bean
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/** MessageSource we delegate our implementation of this interface to. */
	// 用于国际化的策略接口，将接口的实现委派给它
	@Nullable
	private MessageSource messageSource;

	/** Helper class used in event publishing. */
	// 事件广播器，用来发布事件，将事件通知给监听器
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Statically specified listeners. */
	// 存储应用程序监听器
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/** Local listeners registered before refresh. */
	// 存储刷新前注册的本地监听器
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/** ApplicationEvents published before the multicaster setup. */
	// 存储事件
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 * @param id the unique id of the context
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	//配置环境
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	//获取配置环境
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 * @param eventType the resolved event type, if known
	 * @since 4.2
	 */
	//发布一些应用上下文事件，然后监听器再对这些事件进行处理，这里使用ApplicationEventMulticaster进行事件的发布
	//调用相关的监听器处理对应的事件
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		// 如果有必要的话，就将event装饰成ApplicationEvent，这里使用了装饰者模式
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			// 如果event是ApplicationEvent类型的，就直接强制类型转换
			applicationEvent = (ApplicationEvent) event;
		}
		// 否则就直接将event装饰成applicationEvent
		else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		// 如果earlyApplicationEvents不为空，说明广播器还未启动，需要先发布earlyApplicationEvents里的事件
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			// 否则就调用applicationEventMulticaster进行事件的发布
			// multicastEvent会查找事件对应的监听器，然后再处理这个事件
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		// 同时也通过父上下文发布事件
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the parent of this application context.
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}
	//添加BeanFactory处理器
	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}
	//设置应用监听器
	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	//从java注解，或者xml文件中加载配置。也就是这个阶段把我们的bean加入到了spring IOC容器中
	//1、刷新了上下文的状态
	//2、添加了部分的监听器
	//3、发布了在刷新之前存在的一些事件
	//4、还完成了bean的加载，将bean加载到beanFactory中，让用户之后可以通过getBean方法获得对应的bean
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			//1、准备刷新上下文
			//设置了context的启动时间，并设置了context的close和active属性，用以标识该context已经启动了
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			//1、AbstractRefreshableApplicationContext.refreshBeanFactory()->createBeanFactory->DefaultListableBeanFactory
			//2、AbstractRefreshableApplicationContext.loadBeanDefinitions()->AbstractXmlApplicationContext.loadBeanDefinitions()->
			// XMLBeanDefinitionReader->BeanDefinition
			//2、获取到beanFactory，其中会调用 refreshBeanFactory 方法也是由子类实现
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			//3、配置BeanFactory，对BeanFactory进行填充属性
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				//可以对eanFactory进行一些后置处理（子类实现）？？？
				/**
				 * 该方法由子类实现
				 * 当bean Factory初始化完成以后，改变应用程序上下文的内部bean Factory
				 * 此时，所有的bean定义将会被加载，但是没有bean被实例化
				 * 并且允许在指定的应用程序上下文实现中注册特殊的Bean后置处理器
				 */
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				//根据明确的顺序实例化并调用所有已注册的BeanFactoryPostProcessor
				//执行实现了 BeanFactoryPostProcessor接口的类，可手动添加
				// 	1.会在此将class扫描成beanDefinition
				// 	2.BeanFactory的后置处理器调用
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				//实例化并注册拦截bean创建的BeanPostProcessor
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				// 对上下文中的消息源进行初始化
				initMessageSource();

				// Initialize event multicaster for this context.
				//初始化事件广播器
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				//初始化其它特殊的bean对象，springboot中 tomcat的创建就在这里
				onRefresh();

				// Check for listener beans and register them.
				//注册监听器
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				//执行bean对象的创建，将所有bean定义创建成对象存入到容器当中
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				//最后发布完成创建的事件
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				// 为防止Bean资源占用，在异常处理中，销毁已经在前面过程中生成的单件Bean
				destroyBeans();

				// Reset 'active' flag.
				// 重置 'active'标志
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
	protected void prepareRefresh() {
		// Switch to active.
		this.startupDate = System.currentTimeMillis();
		//切换容器的状态
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		//初始化配置资源，通过子类来实现
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
		//主要是获取所有被标记为required的配置类都是可以解析的
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		// 如果earlyApplicationListeners为null，就将applicationListeners的内容传递给它
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			// 如果earlyApplicationListeners不为null
			// 那就将applicationListeners清空，并将earlyApplicationListeners添加到applicationListeners
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		// 初始化earlyApplicationEvents，当广播可用时就发布事件
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		//刷新内部工厂，AbstractRefreshableApplicationContext 类中实现该方法，主要是将原来的beanFactory先干掉，然后创建新的
		refreshBeanFactory();
		//获取到beanFactory
		return getBeanFactory();
	}

	/**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 */
	//当前方法的主要目的是注册一些beanPostProcessor处理器
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		// 获取当前上下文使用的类加载器
		beanFactory.setBeanClassLoader(getClassLoader());
		//这里增加了SPEL语言支持（使得能解析#{bean.xxx}相关形式的属性值），会在解析Bean时用到
		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		/**
		 * 如果bean是以下接口的实现类，就会执行以下对应的方法
		 * EnvironmentAware
		 * EmbeddedValueResolverAware
		 * ResourceLoaderAware
		 * ApplicationEventPublisherAware
		 * MessageSourceAware
		 * ApplicationContextAware
		 * ApplicationStartupAware
		 */
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		//设置需要忽略依赖注入的类
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
		//添加 bean的后置处理器，在bean创建完成后，会判断是否是 ApplicationListener的实现类，如果是，会将其添加到上下文中
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		//这里增加对AspectJ的支持
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			//设置 LoadTimeWeaver 类型的对象注入：LoadTimeWeaver代码的织入
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			// 设置类型的临时匹配 ClassLoader
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		//直接注册对象到单例池中
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			//注册环境对象到单例池种
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			//注册系统配置类
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			//注册系统环境对象
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 */
	//允许在spring初始化完成之后，向其中再注入一些特殊的postProcessBean
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		/**
		 * 1、判断 beanFactory 的类型是否是 BeanDefinitionRegistry
		 * 2、获取到实现了 BeanDefinitionRegistryPostProcessor 的bean对象
		 * 		ConfigurationClassPostProcessor 早先在 初始化时就注入了的配置处理类，根据其实现了 PriorityOrdered，Ordered 来决定执行顺序
		 * 3、invokeBeanFactoryPostProcessors() 执行其中所有 beanFactoryPostProcessor中的postProcessBeanFactory() 方法
		 * 4、invokeBeanFactoryPostProcessors() 执行其中类型 不是BeanDefinitionRegistryPostProcessor的后置处理器
		 *
		 * 执行 BeanFactoryPostProcessors：包括执行自定义的BeanFactoryPostProcessors类，以及加载配置类，扫描包路径下的Bean对象
		 * 优先执行 PriorityOrdered的实现类，并且这里会将Bean容器中加载的@Configuration类加载出来，包括将@Import注解以及ComponentScan等注解下面的类，注册到Bean容器中
		 * 然后执行 Ordered的BeanFactoryPostProcessors
		 */
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		//判断配置文件中是否有 org.graalvm.nativeimage.imagecode，以及容器中是否有 loadTimeWeaver bean对象
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			//设置代码植入处理器
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 */
	//当前方法主要作用是将实现了 BeanPostProcessor 接口的类取出来进行创建，并且添加到容器的 BeanPostProcessors集合中进行缓存起来
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	//初始化 MessageSource 的对象，主要用于支持国际化；执行流程，先去容器中查找，如果有直接赋值，没有则创建一个 DelegatingMessageSource
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 判断beanFactory中是否存在名为"messageSource"的bean
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 如果存在的话就直接从beanFactory中获取对应的bean
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 设置父类上下文的messageSource
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		// 如果beanFactory不存在对应的bean，那么就直接创建DelegatingMessageSource对象，并插入beanFactory中
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			//如果容器中没有 直接创建一个初始 messageSource然后直接注册到单例池中
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	//初始化事件广播器，在这一步的时候才对广播器进行初始化，也就是说在这一步之前就发布的事件会被存入到早期事件集合当中，
	// 并且等待registerListeners() 方法注册监听器后就直接处理早期事件；否则后续发布的事件就进行惰性执行
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 判断beanFactory中是否存在名为"applicationEventMulticaster"的bean
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 如果存在的话就直接从beanFactory中获取对应的bean
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		// 如果beanFactory不存在对应的bean，那么就直接创建SimpleApplicationEventMulticaster对象，并插入beanFactory中
		else {
			//不能使用beanFactoryPostProcessor() 方法中创建，因为是直接new出来的，根本不会通过ioc容器
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			//new出来之后 存入单例池当中
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor.
	 * Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	//初始化其他特殊的bean在特定上下文子容器中；一般与web上下文相关的容器有关
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		// 首先注册applicationListeners中指定的应用程序监听器
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			//将监听器添加到广播器当中
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		/**
		 * 获取容器当中实现了 ApplicationListener接口的事件监听器，并且将其存入广播器当中
		 */
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		/**
		 * 然后获取到早期事件，并且开始处理，这里就是在容器初始化时就已经发布的早期事件，需要在添加完监听器之后就执行的事件
		 * 主要目的就是提供spring在开始创建容器的时候就可以发布各种事件及时进行加载
		 */
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (earlyEventsToProcess != null) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 */
	//完成beanFactory的初始化操作，也是在这里加载了我们自己注册到spring中的bean
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no bean post-processor
		// (such as a PropertyPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		//beanFacotory就是DefaultListableBeanFactory
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 */
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		// 清空上下文级别的资源缓存
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		 /*
        初始化当前上下文的生命周期处理器
        这里也是先检测beanFactory中是否包含名为"lifecycleProcessor"的bean
        如果不包含的话会创建DefaultLifecycleProcessor对象加入到beanFactory中
        */
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		// 首先将刷新消息传播到生命周期处理器
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		// 发布一些不可变的对象
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		LiveBeansView.registerApplicationContext(this);
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook with the JVM runtime, closing this context
	 * on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread() {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			LiveBeansView.unregisterApplicationContext(this);

			try {
				// Publish shutdown event.
				// 发布ContextClosedEvent事件
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			if (this.lifecycleProcessor != null) {
				try {
					//Spring容器生命周期
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			// 销毁Bean
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			// Reset local application listeners to pre-refresh state.
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		//销毁所有单例Bean（不管有没有定义销毁方法）
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------
	//获取beanFactory中对应的bean
	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}
	//获取beanFactory中对应的bean
	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}
	//获取beanFactory中对应的bean
	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}
	//获取beanFactory中对应的bean
	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}
	//获取beanFactory中对应的bean
	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}
	//获得ObjectProvider对象，该对象继承自ObjectFactory，可以通过其getObject获得需要的bean
	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}
	//获得ObjectProvider对象，该对象继承自ObjectFactory，可以通过其getObject获得需要的bean
	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}
	//判断beanFactory中是否包含某个bean
	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}
	//判断单例
	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}
	//判断原型
	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}
	//判断给定名称的bena的类型和给定的类型是否匹配
	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}
	//判断给定名称的bena的类型和给定的类型是否匹配
	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}
	//获取bean的类型
	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}
	// 获得bean的别名
	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------
	//判断是否包含某个BeanDefinition
	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}
	//获得beanFactory中BeanDefinition的数量
	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}
	//获得beanFactory中BeanDefinition的全部名称
	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}
	//通过类型获得bean的名称
	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}
	//通过类型获得bean的名称
	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}
	//通过类型获得bean的名称
	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}
	//获得与给定类型匹配的bean
	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}
	//获得与给定类型匹配的bean
	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}
	//获取给定被给定annotation注释的bean的名称
	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}
	//获取给定被给定annotation注释的bean
	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}
	//获取给定bean包含的annotation
	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------
	//获得当前context的父context
	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}
	//判断本地的bean工厂是否包含给定名称的bean
	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	//如果实现了ConfigurableApplicationContext，则返回父上下文的内部bean工厂；否则，返回父上下文本身
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------
	//解析消息，交给具体的实现类去做
	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	//获取messageSource
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	//如果父上下文也是AbstractApplicationContext，则返回父上下文的内部消息源；否则，返回父上下文本身
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------
	//获取给定路径的资源
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	//获取BeanFactory容器
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
