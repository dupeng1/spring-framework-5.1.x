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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CompositeIterator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Spring's default implementation of the {@link ConfigurableListableBeanFactory}
 * and {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
 * based on bean definition metadata, extensible through post-processors.
 *
 * <p>Typical usage is registering all bean definitions first (possibly read
 * from a bean definition file), before accessing beans. Bean lookup by name
 * is therefore an inexpensive operation in a local bean definition table,
 * operating on pre-resolved bean definition metadata objects.
 *
 * <p>Note that readers for specific bean definition formats are typically
 * implemented separately rather than as bean factory subclasses:
 * see for example {@link PropertiesBeanDefinitionReader} and
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>For an alternative implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
 * have a look at {@link StaticListableBeanFactory}, which manages existing
 * bean instances rather than creating new ones based on bean definitions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 16 April 2001
 * @see #registerBeanDefinition
 * @see #addBeanPostProcessor
 * @see #getBean
 * @see #resolveDependency
 */
/**
 * 基于beanDefinition对象的一个成熟的beanFactory，真正可以作为一个独立使用的IOC容器，因此可以说DefaultListableBeanFactory是整个spring IOC的始祖
 * 1、BeanDefinitionRegistry（在此包中）提供了beanDefinition的管理
 * 2、AbstractAutowireCapableBeanFactory实现属性的自动绑定功能
 * 3、ConfigurableListableBeanFactory提供了对bean定义的分析和修改的便利方法，同时也提供了对单例的预实例化
 * 主要功能：
 * 1、bean定义管理：存储了所有的bean定义，这些定义通常来自于XML配置文件、注解或其他配置方式
 * 2、bean实例化：根据 bean 的定义，容器负责实例化它们
 * 3、依赖注入：容器负责自动装配 bean 之间的依赖关系
 * 4、生命周期管理：容器负责管理 bean 的生命周期，从创建到销毁
 * 5、事件发布：容器负责发布与 bean 相关的各种事件，如初始化、销毁等
 */
@SuppressWarnings("serial")
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

	@Nullable
	private static Class<?> javaxInjectProviderClass;

	static {
		try {
			javaxInjectProviderClass =
					ClassUtils.forName("javax.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - Provider interface simply not supported then.
			javaxInjectProviderClass = null;
		}
	}


	/** Map from serialized id to factory instance. */
	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
			new ConcurrentHashMap<>(8);

	/** Optional id for this factory, for serialization purposes. */
	@Nullable
	private String serializationId;

	/** Whether to allow re-registration of a different definition with the same name. */
	private boolean allowBeanDefinitionOverriding = true;

	/** Whether to allow eager class loading even for lazy-init beans. */
	private boolean allowEagerClassLoading = true;

	/** Optional OrderComparator for dependency Lists and arrays. */
	@Nullable
	private Comparator<Object> dependencyComparator;

	/** Resolver to use for checking if a bean definition is an autowire candidate. */
	private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

	/** Map from dependency type to corresponding autowired value. */
	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

	/** Map of bean definition objects, keyed by bean name. */
	//Bean 定义存储
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	/** Map of singleton and non-singleton bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

	/** Map of singleton-only bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

	/** List of bean definition names, in registration order. */
	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

	/** List of names of manually registered singletons, in registration order. */
	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

	/** Cached array of bean definition names in case of frozen configuration. */
	@Nullable
	private volatile String[] frozenBeanDefinitionNames;

	/** Whether bean definition metadata may be cached for all beans. */
	private volatile boolean configurationFrozen = false;


	/**
	 * Create a new DefaultListableBeanFactory.
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 * @param parentBeanFactory the parent BeanFactory
	 */
	public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * Specify an id for serialization purposes, allowing this BeanFactory to be
	 * deserialized from this id back into the BeanFactory object, if needed.
	 */
	public void setSerializationId(@Nullable String serializationId) {
		if (serializationId != null) {
			serializableFactories.put(serializationId, new WeakReference<>(this));
		}
		else if (this.serializationId != null) {
			serializableFactories.remove(this.serializationId);
		}
		this.serializationId = serializationId;
	}

	/**
	 * Return an id for serialization purposes, if specified, allowing this BeanFactory
	 * to be deserialized from this id back into the BeanFactory object, if needed.
	 * @since 4.1.2
	 */
	@Nullable
	public String getSerializationId() {
		return this.serializationId;
	}

	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. This also applies to overriding aliases.
	 * <p>Default is "true".
	 * @see #registerBeanDefinition
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Return whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * @since 4.1.2
	 */
	public boolean isAllowBeanDefinitionOverriding() {
		return this.allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>Default is "true". Turn this flag off to suppress class loading
	 * for lazy-init beans unless such a bean is explicitly requested.
	 * In particular, by-type lookups will then simply ignore bean definitions
	 * without resolved class name, instead of loading the bean classes on
	 * demand just to perform a type check.
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}

	/**
	 * Return whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * @since 4.1.2
	 */
	public boolean isAllowEagerClassLoading() {
		return this.allowEagerClassLoading;
	}

	/**
	 * Set a {@link java.util.Comparator} for dependency Lists and arrays.
	 * @since 4.0
	 * @see org.springframework.core.OrderComparator
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
		this.dependencyComparator = dependencyComparator;
	}

	/**
	 * Return the dependency comparator for this BeanFactory (may be {@code null}.
	 * @since 4.0
	 */
	@Nullable
	public Comparator<Object> getDependencyComparator() {
		return this.dependencyComparator;
	}

	/**
	 * Set a custom autowire candidate resolver for this BeanFactory to use
	 * when deciding whether a bean definition should be considered as a
	 * candidate for autowiring.
	 */
	//添加额外的自动装配解析器
	//如：QualifierAnnotationAutowireCandidateResolver提供@Qualifier注解解析的功能和@Autowire
	public void setAutowireCandidateResolver(final AutowireCandidateResolver autowireCandidateResolver) {
		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
		if (autowireCandidateResolver instanceof BeanFactoryAware) {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(DefaultListableBeanFactory.this);
					return null;
				}, getAccessControlContext());
			}
			else {
				((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
			}
		}
		this.autowireCandidateResolver = autowireCandidateResolver;
	}

	/**
	 * Return the autowire candidate resolver for this BeanFactory (never {@code null}).
	 */
	public AutowireCandidateResolver getAutowireCandidateResolver() {
		return this.autowireCandidateResolver;
	}


	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
			this.dependencyComparator = otherListableFactory.dependencyComparator;
			// A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware...
			setAutowireCandidateResolver(
					BeanUtils.instantiateClass(otherListableFactory.getAutowireCandidateResolver().getClass()));
			// Make resolvable dependencies (e.g. ResourceLoader) available here as well...
			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of remaining BeanFactory methods
	//---------------------------------------------------------------------
	//获取指定类型的bean对象
	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType, (Object[]) null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		//根据类型解析bean
		Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
		if (resolved == null) {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
		return (T) resolved;
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return new BeanObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				T resolved = resolveBean(requiredType, null, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}
			@Override
			public T getObject(Object... args) throws BeansException {
				T resolved = resolveBean(requiredType, args, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}
			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				return resolveBean(requiredType, null, false);
			}
			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				return resolveBean(requiredType, null, true);
			}
			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForTypedStream(requiredType))
						.map(name -> (T) getBean(name))
						.filter(bean -> !(bean instanceof NullBean));
			}
			@Override
			public Stream<T> orderedStream() {
				String[] beanNames = getBeanNamesForTypedStream(requiredType);
				Map<String, T> matchingBeans = new LinkedHashMap<>(beanNames.length);
				for (String beanName : beanNames) {
					Object beanInstance = getBean(beanName);
					if (!(beanInstance instanceof NullBean)) {
						matchingBeans.put(beanName, (T) beanInstance);
					}
				}
				Stream<T> stream = matchingBeans.values().stream();
				return stream.sorted(adaptOrderComparator(matchingBeans));
			}
		};
	}

	@Nullable
	private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
		NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
		if (namedBean != null) {
			return namedBean.getBeanInstance();
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
		}
		else if (parent != null) {
			ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
			if (args != null) {
				return parentProvider.getObject(args);
			}
			else {
				return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
			}
		}
		return null;
	}

	private String[] getBeanNamesForTypedStream(ResolvableType requiredType) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return this.beanDefinitionMap.containsKey(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		String[] frozenNames = this.frozenBeanDefinitionNames;
		if (frozenNames != null) {
			return frozenNames.clone();
		}
		else {
			return StringUtils.toStringArray(this.beanDefinitionNames);
		}
	}
	//根据给定的类型获取所有相关的 bean 名称
	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		Class<?> resolved = type.resolve();
		if (resolved != null && !type.hasGenerics()) {
			return getBeanNamesForType(resolved, true, true);
		}
		else {
			return doGetBeanNamesForType(type, true, true);
		}
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		//如果没有冻结，就根据类型去BeanFactory找，如果冻结了，可能就跳过这个if然后去缓存中去拿了
		//configurationFrozen：是否可以为所有bean  缓存  bean定义元数据。
		if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
			//ResolvableType.forRawClass返回指定Class的ResolvableType，仅对原始类进行可分配性检查
			return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
		}
		//值得注意的是，不管type是否不为空，allowEagerInit是否为true
		//只要isConfigurationFrozen()为false就一定不会走这里
		//因为isConfigurationFrozen()为false的时候表示BeanDefinition
		//可能还会发生更改和添加，所以不能进行缓存
		//如果允许非单例的bean，那么从保存所有bean的集合中获取，否则从
		//单例bean中获取
		Map<Class<?>, String[]> cache =
				(includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
		String[] resolvedBeanNames = cache.get(type);
		if (resolvedBeanNames != null) {
			return resolvedBeanNames;
		}
		//如果缓存中没有获取到，那么只能重新获取，获取到之后就存入缓存
		resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
		if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
			cache.put(type, resolvedBeanNames);
		}
		return resolvedBeanNames;
	}

	//allowEagerInit：是否允许急切初始化：为类型检查，是否初始化 懒加载的单例Bean对象 和 由FactoryBeans(或由带有“factory-bean”引用的工厂方法)创建的对象。
	// 请注意，factorybean需要紧急初始化以确定它们的类型:因此请注意，为该标志传入“true”将初始化factorybean和“factory-bean”引用
	private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = new ArrayList<>();

		// Check all bean definitions.
		// 遍历所有BeanDefinition的名字（beanDefinitionNames不会包含"&xxx"的）
		for (String beanName : this.beanDefinitionNames) {
			// Only consider bean as eligible if the bean name
			// is not defined as alias for some other bean.
			// 只有当bean名称没有定义为其他bean的别名时，才认为bean是合格的。
			if (!isAlias(beanName)) {//只处理规范的BeanName
				try {
					//获取合并的BeanDefinition，合并的BeanDefinition是指spring整合了父BeanDefinition的属性，将其他类型的BeanDefinition变成了RootBeanDefintion
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// Only check bean definition if it is complete.
					//1.1 判断mbd允不允许解析对应的类型
					//首先mdb不能是抽象的
					//然后allowEagerInit为true，说明允许立即初始化，那接下来就去推测解析mdb的类型，并进行匹配
					//如果allowEagerInit为false，说明不允许立即初始化，但出现以下情况也可以继续去推测类型：
					// 需要同时满足a和b：
					//	a.以下三个条件满足其一：
					//		mbd.hasBeanClass()：是否已经加载过类了
					//		mbd.isLazyInit()：返回该bean是否应该延迟初始化，即在启动时不急于实例化。只适用于单例bean。（是不是懒加载的）
					//		isAllowEagerClassLoading()：返回是否允许工厂紧急加载bean类，即使对于标记为“lazy-init”的bean定义也是如此。
					//
					//		即已经加载过类、或者非懒加载的mdb、或者工厂允许紧急加载bean类

					//	b.requiresEagerInitForType：检查指定的bean是否需要紧急初始化以确定其类型。
					//	什么情况需要紧急初始化？当前beanName是FactoryBean，且FactoryBean本身还没有实例化的时候
					if (!mbd.isAbstract() && (allowEagerInit ||
							(mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
						// In case of FactoryBean, match object created by FactoryBean.
						boolean isFactoryBean = isFactoryBean(beanName, mbd);
						BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
						// 如果允许早期初始化，那么只要允许返回非单例 bean 或者该 bean 就是单例的情况下就会调用最后的 isTypeMatch 方法
						// 如果当前 bean 是一个 FactoryBean，那么 isTypeMatch 方法会进行早期实例化，再调用其 getObjectType 方法获取类型
						// 在不允许早期初始化的情况下，如果当前bean是 FactoryBean，那么它只能在已经被创建的情况下（不能提早初始化）调用 isTypeMatch 进行匹配判断
						// 否则只能宣告匹配失败，返回false
						boolean matchFound =
								(allowEagerInit || !isFactoryBean ||
										(dbd != null && !mbd.isLazyInit()) || containsSingleton(beanName)) &&
								(includeNonSingletons ||
										(dbd != null ? mbd.isSingleton() : isSingleton(beanName))) &&
								isTypeMatch(beanName, type);
						// 如果用户是要查找 FactoryBean 类型，那么在 matchFound 为 false 的情况下，给 beanName 加上 & 符合，用来匹配 FactoryBean
						if (!matchFound && isFactoryBean) {
							// In case of FactoryBean, try to match FactoryBean instance itself next.
							beanName = FACTORY_BEAN_PREFIX + beanName;
							matchFound = (includeNonSingletons || mbd.isSingleton()) && isTypeMatch(beanName, type);
						}
						// 找到便记录到result集合中，等待返回
						if (matchFound) {
							result.add(beanName);
						}
					}
				}
				catch (CannotLoadBeanClassException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably a class name with a placeholder: let's ignore it for type matching purposes.
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring bean class loading failure for bean '" + beanName + "'", ex);
					}
					onSuppressedException(ex);
				}
				catch (BeanDefinitionStoreException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably some metadata with a placeholder: let's ignore it for type matching purposes.
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring unresolvable metadata in bean definition '" + beanName + "'", ex);
					}
					onSuppressedException(ex);
				}
			}
		}

		// Check manually registered singletons too.
		// 还要检查手动注册的单例
		for (String beanName : this.manualSingletonNames) {
			try {
				// In case of FactoryBean, match object created by FactoryBean.
				//处理FactoryBean的情况
				if (isFactoryBean(beanName)) {
					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
						result.add(beanName);
						// Match found for this bean: do not match FactoryBean itself anymore.
						// 为该bean找到匹配:不再匹配FactoryBean本身。
						continue;
					}
					// In case of FactoryBean, try to match FactoryBean itself next.
					// 对于FactoryBean，接下来尝试匹配FactoryBean实例本身
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// Match raw bean instance (might be raw FactoryBean).
				//如果没有匹配成功，那么匹配工厂类
				if (isTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Shouldn't happen - probably a result of circular reference resolution...
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to check manually registered singleton with name '" + beanName + "'", ex);
				}
			}
		}

		return StringUtils.toStringArray(result);
	}

	/**
	 * Check whether the specified bean would need to be eagerly initialized
	 * in order to determine its type.
	 * @param factoryBeanName a factory-bean reference that the bean definition
	 * defines a factory method for
	 * @return whether eager initialization is necessary
	 */
	private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
		//如果是FactoryBean情况，FactoryBean本身如果还没有初始化过，会返回true
		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
	}
	//根据给定的类型获取所有相关的 bean 实例
	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		Map<String, T> result = new LinkedHashMap<>(beanNames.length);
		for (String beanName : beanNames) {
			try {
				Object beanInstance = getBean(beanName);
				if (!(beanInstance instanceof NullBean)) {
					result.put(beanName, (T) beanInstance);
				}
			}
			catch (BeanCreationException ex) {
				Throwable rootCause = ex.getMostSpecificCause();
				if (rootCause instanceof BeanCurrentlyInCreationException) {
					BeanCreationException bce = (BeanCreationException) rootCause;
					String exBeanName = bce.getBeanName();
					if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " +
									ex.getMessage());
						}
						onSuppressedException(ex);
						// Ignore: indicates a circular reference when autowiring constructors.
						// We want to find matches other than the currently created bean itself.
						continue;
					}
				}
				throw ex;
			}
		}
		return result;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> result = new ArrayList<>();
		for (String beanName : this.beanDefinitionNames) {
			BeanDefinition beanDefinition = getBeanDefinition(beanName);
			if (!beanDefinition.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		for (String beanName : this.manualSingletonNames) {
			if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		return StringUtils.toStringArray(result);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
		String[] beanNames = getBeanNamesForAnnotation(annotationType);
		Map<String, Object> result = new LinkedHashMap<>(beanNames.length);
		for (String beanName : beanNames) {
			Object beanInstance = getBean(beanName);
			if (!(beanInstance instanceof NullBean)) {
				result.put(beanName, beanInstance);
			}
		}
		return result;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		A ann = null;
		Class<?> beanType = getType(beanName);
		if (beanType != null) {
			ann = AnnotationUtils.findAnnotation(beanType, annotationType);
		}
		if (ann == null && containsBeanDefinition(beanName)) {
			// Check raw bean class, e.g. in case of a proxy.
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			if (bd.hasBeanClass()) {
				Class<?> beanClass = bd.getBeanClass();
				if (beanClass != beanType) {
					ann = AnnotationUtils.findAnnotation(beanClass, annotationType);
				}
			}
		}
		return ann;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if (autowiredValue != null) {
			if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
				throw new IllegalArgumentException("Value [" + autowiredValue +
						"] does not implement specified dependency type [" + dependencyType.getName() + "]");
			}
			this.resolvableDependencies.put(dependencyType, autowiredValue);
		}
	}

	//判断候选对象是否可用，有三重过滤规则
	//1、bd.autowireCandidate=true
	//2、泛型匹配
	//3、@Qualifier
	//委托给 ContextAnnotationAutowireCandidateResolver
	@Override
	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
	}

	/**
	 * Determine whether the specified bean definition qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 * @param beanName the name of the bean definition to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
	 * @return whether the bean should be considered as autowire candidate
	 */
	protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
			throws NoSuchBeanDefinitionException {

		String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
		if (containsBeanDefinition(beanDefinitionName)) {
			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanDefinitionName), descriptor, resolver);
		}
		else if (containsSingleton(beanName)) {
			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
		}
		else if (parent instanceof ConfigurableListableBeanFactory) {
			// If no DefaultListableBeanFactory, can't pass the resolver along.
			return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
		}
		else {
			return true;
		}
	}

	/**
	 * Determine whether the specified bean definition qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 * @param beanName the name of the bean definition to check
	 * @param mbd the merged bean definition to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
	 * @return whether the bean should be considered as autowire candidate
	 */
	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
			DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

		String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
		// 1. 传统方式：解析 bd.beanClass，注意 Spring注解驱动时根本不会配置beanClassName
		resolveBeanClass(mbd, beanDefinitionName);
		// 2. 注解驱动：解析工厂方法 bd.factoryMethodToIntrospect
		if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
			new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
		}
		// 3. 直接委托给AutowireCandidateResolver
		return resolver.isAutowireCandidate(
				new BeanDefinitionHolder(mbd, beanName, getAliases(beanDefinitionName)), descriptor);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	@Override
	public Iterator<String> getBeanNamesIterator() {
		CompositeIterator<String> iterator = new CompositeIterator<>();
		iterator.add(this.beanDefinitionNames.iterator());
		iterator.add(this.manualSingletonNames.iterator());
		return iterator;
	}

	@Override
	public void clearMetadataCache() {
		super.clearMetadataCache();
		clearByTypeCache();
	}

	@Override
	public void freezeConfiguration() {
		this.configurationFrozen = true;
		this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
	}

	@Override
	public boolean isConfigurationFrozen() {
		return this.configurationFrozen;
	}

	/**
	 * Considers all beans as eligible for metadata caching
	 * if the factory's configuration has been marked as frozen.
	 * @see #freezeConfiguration()
	 */
	@Override
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
	}

	@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		for (String beanName : beanNames) {
			//1. 合并BeanDefinition
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			//判断BeanDefinition，非抽象，是单例，非懒加载
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				//2. FactoryBean逻辑
				if (isFactoryBean(beanName)) {
					//FACTORY_BEAN_PREFIX="&"
					//这里创建的是FactoryBean本身的对象
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						final FactoryBean<?> factory = (FactoryBean<?>) bean;
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
											((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						}
						else {
							//如果我们FactoryBean实现的是SmartFactoryBean接口
							//并重写了isEagerInit方法，返回true
							//那么Spring容器启动的时候，就会把改FactoryBean对应的getObject()方法的对象生成，否则只有getBean的时候才会生成。
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						if (isEagerInit) {
							//创建真正的Bean对象（这里是FactoryBean.getObject()对象）
							getBean(beanName);
						}
					}
				}
				else {
					//3. 创建Bean对象
					getBean(beanName);
				}
			}
		}

		// Trigger post-initialization callback for all applicable beans...
		// 4. 所有非懒加载单例Bean都创建完成后
		for (String beanName : beanNames) {
			// 从单例池里拿beanName对应的单例Bean
			Object singletonInstance = getSingleton(beanName);
			// 判断单例Bean是否实现了SmartInitializingSingleton接口
			// 这个接口也属于Spring生命周期里的一种接口
			if (singletonInstance instanceof SmartInitializingSingleton) {
				final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						smartSingleton.afterSingletonsInstantiated();
						return null;
					}, getAccessControlContext());
				}
				else {
					//触发afterSingletonsInstantiated方法
					smartSingleton.afterSingletonsInstantiated();
				}
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry interface
	//---------------------------------------------------------------------
	//用于注册一个新的 bean 定义
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				// 如果是AbstractBeanDefinition, 验证它是否重写了抽象方法，同时把重写的方法对父类的方法进行覆盖
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}
		// beanDefinitionMap: 使用map存放BeanDefinition,以beanName为key Definition为value
		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			// 这个beanName已经被注册了，下面根据不同的规则 进行处理
			// 是否允许覆盖bean，用这个新的bean替换已经注册的旧的bean
			if (!isAllowBeanDefinitionOverriding()) {
				// 如果Spring容器不允许重载BeanDefinition，抛出异常，beanName冲突
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				// 这个bean在程序承担的角色
				// 如果应用自己定义的beanName和框架底层的beanName重复, 则覆盖这个应用级别的bean
				// 优先保证框架的基础类被注册
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(existingDefinition)) {
				// 如果是两个不同的beanDefinition, 用新的bean覆盖已经存在从bean
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			// 注册到注册表中
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			// 检查这个工厂的 bean 创建阶段是否已经开始，即在此期间是否有任何 bean 被标记为已创建。
			if (hasBeanCreationStarted()) {
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				// 对注册表加锁，在它处理的期间不能再修改启动时集合元素（用于稳定迭代）
				synchronized (this.beanDefinitionMap) {
					// 注册到注册表
					this.beanDefinitionMap.put(beanName, beanDefinition);
					// 加当前beanName添加到beanDefinitionNames集合中
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					// 从manualSingletonNames中删除这个单例bean的名称
					removeManualSingletonName(beanName);
				}
			}
			else {
				// Still in startup registration phase
				// 处于启动阶段，直接加入到注册表中
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				// 从manualSingletonNames中删除这个单例bean的名称
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			// 如果这个beanName已经在注册表中了
			// 或者单例对象的注册表中同时也有这个beanName
			// 重置给定 bean 的所有 bean 定义缓存，包括从它派生的 bean 的缓存
			resetBeanDefinition(beanName);
		}
	}
	//移除一个已注册的 bean 定义
	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		Assert.hasText(beanName, "'beanName' must not be empty");

		BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}

		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			synchronized (this.beanDefinitionMap) {
				List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
				updatedDefinitions.remove(beanName);
				this.beanDefinitionNames = updatedDefinitions;
			}
		}
		else {
			// Still in startup registration phase
			this.beanDefinitionNames.remove(beanName);
		}
		this.frozenBeanDefinitionNames = null;

		resetBeanDefinition(beanName);
	}

	/**
	 * Reset all bean definition caches for the given bean,
	 * including the caches of beans that are derived from it.
	 * <p>Called after an existing bean definition has been replaced or removed,
	 * triggering {@link #clearMergedBeanDefinition}, {@link #destroySingleton}
	 * and {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition} on the
	 * given bean and on all bean definitions that have the given bean as parent.
	 * @param beanName the name of the bean to reset
	 * @see #registerBeanDefinition
	 * @see #removeBeanDefinition
	 */
	protected void resetBeanDefinition(String beanName) {
		// Remove the merged bean definition for the given bean, if already created.
		clearMergedBeanDefinition(beanName);

		// Remove corresponding bean from singleton cache, if any. Shouldn't usually
		// be necessary, rather just meant for overriding a context's default beans
		// (e.g. the default StaticMessageSource in a StaticApplicationContext).
		destroySingleton(beanName);

		// Notify all post-processors that the specified bean definition has been reset.
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			if (processor instanceof MergedBeanDefinitionPostProcessor) {
				((MergedBeanDefinitionPostProcessor) processor).resetBeanDefinition(beanName);
			}
		}

		// Reset all bean definitions that have the given bean as parent (recursively).
		for (String bdName : this.beanDefinitionNames) {
			if (!beanName.equals(bdName)) {
				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
				// Ensure bd is non-null due to potential concurrent modification
				// of the beanDefinitionMap.
				if (bd != null && beanName.equals(bd.getParentName())) {
					resetBeanDefinition(bdName);
				}
			}
		}
	}

	/**
	 * Only allows alias overriding if bean definition overriding is allowed.
	 */
	@Override
	protected boolean allowAliasOverriding() {
		return isAllowBeanDefinitionOverriding();
	}

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		super.registerSingleton(beanName, singletonObject);
		updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
		clearByTypeCache();
	}

	@Override
	public void destroySingletons() {
		//调用父类
		super.destroySingletons();
		updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
		clearByTypeCache();
	}

	@Override
	public void destroySingleton(String beanName) {
		super.destroySingleton(beanName);
		removeManualSingletonName(beanName);
		clearByTypeCache();
	}

	private void removeManualSingletonName(String beanName) {
		updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
	}

	/**
	 * Update the factory's internal set of manual singleton names.
	 * @param action the modification action
	 * @param condition a precondition for the modification action
	 * (if this condition does not apply, the action can be skipped)
	 */
	private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			synchronized (this.beanDefinitionMap) {
				if (condition.test(this.manualSingletonNames)) {
					Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
					action.accept(updatedSingletons);
					this.manualSingletonNames = updatedSingletons;
				}
			}
		}
		else {
			// Still in startup registration phase
			if (condition.test(this.manualSingletonNames)) {
				action.accept(this.manualSingletonNames);
			}
		}
	}

	/**
	 * Remove any assumptions about by-type mappings.
	 */
	private void clearByTypeCache() {
		this.allBeanNamesByType.clear();
		this.singletonBeanNamesByType.clear();
	}


	//---------------------------------------------------------------------
	// Dependency resolution functionality
	//---------------------------------------------------------------------

	@Override
	public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);
		if (namedBean != null) {
			return namedBean;
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof AutowireCapableBeanFactory) {
			return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
		}
		throw new NoSuchBeanDefinitionException(requiredType);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

		Assert.notNull(requiredType, "Required type must not be null");
		String[] candidateNames = getBeanNamesForType(requiredType);

		if (candidateNames.length > 1) {
			List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
			for (String beanName : candidateNames) {
				if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
					autowireCandidates.add(beanName);
				}
			}
			if (!autowireCandidates.isEmpty()) {
				candidateNames = StringUtils.toStringArray(autowireCandidates);
			}
		}

		if (candidateNames.length == 1) {
			String beanName = candidateNames[0];
			return new NamedBeanHolder<>(beanName, (T) getBean(beanName, requiredType.toClass(), args));
		}
		else if (candidateNames.length > 1) {
			Map<String, Object> candidates = new LinkedHashMap<>(candidateNames.length);
			for (String beanName : candidateNames) {
				if (containsSingleton(beanName) && args == null) {
					Object beanInstance = getBean(beanName);
					candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
				}
				else {
					candidates.put(beanName, getType(beanName));
				}
			}
			String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
			if (candidateName == null) {
				candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
			}
			if (candidateName != null) {
				Object beanInstance = candidates.get(candidateName);
				if (beanInstance == null || beanInstance instanceof Class) {
					beanInstance = getBean(candidateName, requiredType.toClass(), args);
				}
				return new NamedBeanHolder<>(candidateName, (T) beanInstance);
			}
			if (!nonUniqueAsNull) {
				throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
			}
		}

		return null;
	}

	//支持Optional、延迟注入、懒加载注入、正常注入
	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
		// descriptor代表当前需要注入的那个【字段】，或者【方法的参数】，也就是【注入点】
		// ParameterNameDiscovery用于解析方法参数名称
		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		// 1. Optional<T>，主要是将依赖设置非强制依赖，即 descriptor.required=false
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		}
		// 2. ObjectFactory<T>、ObjectProvider<T>，延迟依赖注入
		else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		}
		// 3. javax.inject.Provider<T>，延迟依赖注入
		else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		}
		else {
			// 4. @Lazy，延迟依赖注入
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				// 5. 正常情况，根据类型查找依赖
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
	}

	//封装了依赖查找的各种情况
	//1、快速查找：@Autowired注解处理场景。AutowiredAnnotationBeanPostProcessor处理@Autowired注解时，
	// 如果注入的对象只有一个，会将该bean对应的名称缓存起来，下次直接通过名称查找会快很多。
	//2、注入指定值：@Value 注解处理场景。QualifierAnnotationAutowireCandidateResolver 处理 @Value 注解时，会读取 @Value 对应的值进行注入。
	// 如果是 String 要经过三个过程：①占位符处理 -> ②EL 表达式解析 -> ③类型转换，这也是一般的处理过程，
	// BeanDefinitionValueResolver 处理 String 对象也是这个过程。
	//3、集合依赖查询：直接全部委托给 resolveMultipleBeans 方法
	//4、单个依赖查询：先调用 findAutowireCandidates 查找所有可用的依赖，
	// 如果有多个依赖，则根据规则匹配： @Primary -> @Priority -> ③方法名称或字段名称。
	@Nullable
	public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
		//线程上设置当前注入点
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			// 1. 快速查找，根据名称查找。AutowiredAnnotationBeanPostProcessor用到
			Object shortcut = descriptor.resolveShortcut(this);
			if (shortcut != null) {
				return shortcut;
			}
			// 2. 注入指定值，QualifierAnnotationAutowireCandidateResolver解析@Value会用到
			//获取依赖注入的参数类型
			Class<?> type = descriptor.getDependencyType();
			//b.获取@Value所指定的值
			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
			if (value != null) {
				//如果拿到是String
				if (value instanceof String) {
					// 2.1 解析嵌入值，占位符填充(${})
					String strVal = resolveEmbeddedValue((String) value);
					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
							getMergedBeanDefinition(beanName) : null);
					// 2.2 解析Spring表达式(#{})
					//（根据Spring表达式得到的对象有可能直接就是一个Bean对象，也有可能是其他）
					value = evaluateBeanDefinitionString(strVal, bd);
				}
				//将value转换为descriptor所对应的类型
				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
				try {
					// 2.3 类型转换
					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
				}
				catch (UnsupportedOperationException ex) {
					// A custom TypeConverter which does not support TypeDescriptor resolution...
					return (descriptor.getField() != null ?
							converter.convertIfNecessary(value, type, descriptor.getField()) :
							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
				}
			}
			// 3. 注入多实例情况，
			// 如果descriptor对应的类型是数组、Map、集合这些，就拿descriptor对应的类型去匹配所有的bean对象，不用进一步做筛选了
			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
			if (multipleBeans != null) {
				return multipleBeans;
			}
			// 4. 根据类型查找所有候选Bean
			//	找到所有Bean，key是BeanName，value有可能是Bean对象，有可能是BeanClass
			//		依赖注入过程中，没必要把所有候选Bean都实例化
			//		因为后续流程最多只会注入一个，保证注入的那个实例化过就OK了
			//		而且后续判断是否匹配，有Class就够了
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			// 4.1 没有查找到依赖，判断descriptor.require
			if (matchingBeans.isEmpty()) {
				if (isRequired(descriptor)) {
					//required为true，且matchingBeans为空，则抛异常
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				//如果required不为true，则注入一个空属性
				return null;
			}

			String autowiredBeanName;
			Object instanceCandidate;
			// 4.2 如果根据类型匹配到了多个Bean，如何过滤
			if (matchingBeans.size() > 1) {
				// 4.2.1 @Primary -> @Priority -> 方法名称或字段名称匹配
				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
				// 4.2.2 根据是否必须，抛出异常。注意这里如果是集合处理，则返回null
				if (autowiredBeanName == null) {
					if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
						//如果required为true 或 type不是数组、集合、map
						//indicatesMultipleBeans：判断type是否是 数组、集合、map
						//这里直接抛异常
						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
					}
					else {
						// In case of an optional Collection/Map, silently ignore a non-unique case:
						// possibly it was meant to be an empty collection of multiple regular beans
						// (before 4.3 in particular when we didn't even look for collection beans).
						return null;
					}
				}
				//筛选出一个
				instanceCandidate = matchingBeans.get(autowiredBeanName);
			}
			else {
				// We have exactly one match.
				// 根据类型只匹配到一个
				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
				autowiredBeanName = entry.getKey();
				instanceCandidate = entry.getValue();
			}
			// 4.3 记录匹配过的BeanName
			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(autowiredBeanName);
			}
			// 4.4 有可能筛选出来的是某个bean的类型，此处就进行实例化，调用getBean
			if (instanceCandidate instanceof Class) {
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
			Object result = instanceCandidate;
			if (result instanceof NullBean) {
				if (isRequired(descriptor)) {
					//解析出来是NullBean，且required=true，则抛异常
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				result = null;
			}
			//判断找到的Bean对象类型是否是期望注入的类型
			if (!ClassUtils.isAssignableValue(type, result)) {
				throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
			}
			return result;
		}
		finally {
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	@Nullable
	private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		final Class<?> type = descriptor.getDependencyType();

		if (descriptor instanceof StreamDependencyDescriptor) {
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			Stream<Object> stream = matchingBeans.keySet().stream()
					.map(name -> descriptor.resolveCandidate(name, type, this))
					.filter(bean -> !(bean instanceof NullBean));
			if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
				stream = stream.sorted(adaptOrderComparator(matchingBeans));
			}
			return stream;
		}
		else if (type.isArray()) {
			Class<?> componentType = type.getComponentType();
			ResolvableType resolvableType = descriptor.getResolvableType();
			Class<?> resolvedArrayType = resolvableType.resolve(type);
			if (resolvedArrayType != type) {
				componentType = resolvableType.getComponentType().resolve();
			}
			if (componentType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
			if (result instanceof Object[]) {
				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
				if (comparator != null) {
					Arrays.sort((Object[]) result, comparator);
				}
			}
			return result;
		}
		else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
			if (elementType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), type);
			if (result instanceof List) {
				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
				if (comparator != null) {
					((List<?>) result).sort(comparator);
				}
			}
			return result;
		}
		else if (Map.class == type) {
			ResolvableType mapType = descriptor.getResolvableType().asMap();
			Class<?> keyType = mapType.resolveGeneric(0);
			if (String.class != keyType) {
				return null;
			}
			Class<?> valueType = mapType.resolveGeneric(1);
			if (valueType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			return matchingBeans;
		}
		else {
			return null;
		}
	}

	private boolean isRequired(DependencyDescriptor descriptor) {
		return getAutowireCandidateResolver().isRequired(descriptor);
	}

	private boolean indicatesMultipleBeans(Class<?> type) {
		return (type.isArray() || (type.isInterface() &&
				(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
	}

	@Nullable
	private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator) {
			return ((OrderComparator) comparator).withSourceProvider(
					createFactoryAwareOrderSourceProvider(matchingBeans));
		}
		else {
			return comparator;
		}
	}

	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> dependencyComparator = getDependencyComparator();
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
				(OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
		return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
	}

	private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
		IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
		beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
		return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
	}

	/**
	 * Find bean instances that match the required type.
	 * Called during autowiring for the specified bean.
	 * @param beanName the name of the bean that is about to be wired
	 * @param requiredType the actual type of bean to look for
	 * (may be an array component type or collection element type)
	 * @param descriptor the descriptor of the dependency to resolve
	 * @return a Map of candidate names and candidate instances that match
	 * the required type (never {@code null})
	 * @throws BeansException in case of errors
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	//真正在 Spring IoC 容器中进行依赖查找，依赖查找的来源有
	//1、内部对象
	//2、托管Bean
	//3、BeanDefinition
	//最后如果无法查找到依赖对象，会进行一些补偿机制，想方设法获取注入的对象，如泛型补偿，自引用补偿
	protected Map<String, Object> findAutowireCandidates(
			@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
		//1. 从BeanFactory中找出和requiredType所匹配的beanName，仅仅是beanName，这些bean不一定经过了实例化
		//descriptor.isEager()：返回此依赖项是否为“急切的”，即急切地解析潜在的目标bean以进行类型匹配。
		//处理比如FactoryBean的情况，真正的类型肯定不是FactoryBean.class
		//所以获取真正的类型，就会需要初始化FactoryBean
		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this, requiredType, true, descriptor.isEager());
		Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
		//2. 根据类型从resolvableDependencies中匹配，resolvableDependencies中存放的是 类型：bean对象
		//类似一个缓存，在Spring启动的时候会赋值
		//事先指定 类型 和 实例 的映射关系，只要是注入这个类型，就用这个类型对应的实例进行注入
		for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
			Class<?> autowiringType = classObjectEntry.getKey();
			//requiredType如果是autowiringType子类 或 本身
			if (autowiringType.isAssignableFrom(requiredType)) {
				Object autowiringValue = classObjectEntry.getValue();
				//根据给定的所需类型解析给定的自动装配值，例如ObjectFactory值到其实际对象结果。
				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
				if (requiredType.isInstance(autowiringValue)) {
					//满足条件，往结果集添加
					//这种方式bean是没有名字的，所以要利用ObjectUtils.identityToString生成
					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
					break;
				}
			}
		}
		//类型查找：本质上递归调用beanFactory#beanNamesForType
		for (String candidate : candidateNames) {
			//3. isSelfReference：判断是不是自己注入自己，说明beanName和candidate本质是同一个对象
			//4. isAutowireCandidate：判断能不能注入，进一步匹配bd.autowireCandidate、泛型、@@Qualifier等进行过滤
			if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
				//5.添加到结果集
				addCandidateEntry(result, candidate, descriptor, requiredType);
			}
		}
		// 补偿机制：如果依赖查找无法匹配，怎么办？包含泛型补偿和自身引用补偿两种
		if (result.isEmpty()) {
			// 是否是多匹配，即需要匹配的类型是不是Map、数组、集合之类的
			boolean multiple = indicatesMultipleBeans(requiredType);
			// Consider fallback matches if the first pass failed to find anything...
			// 泛型补偿，实际上是允许注入对象类型的泛型存在无法解析的情况
			// 6.泛型补偿
			DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
			for (String candidate : candidateNames) {
				//3. isSelfReference：判断是不是自己注入自己
				//4. isAutowireCandidate：判断能不能注入，回退匹配
				//补偿1：不允许自依赖，但如果是集合依赖，需要过滤非@Qualifier对象
				if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
						(!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
					//5.回退匹配成功，添加到结果集
					addCandidateEntry(result, candidate, descriptor, requiredType);
				}
			}
			//补偿2：允许自依赖，但如果是集合依赖，注入的集合依赖中需要过滤自己
			if (result.isEmpty() && !multiple) {
				// Consider self references as a final pass...
				// but in the case of a dependency collection, not the very same bean itself.
				// 将自引用视为最后一次传递……但在依赖项集合的情况下，不是同一个bean本身。
				for (String candidate : candidateNames) {
					//3. isSelfReference：判断是不是自己注入自己
					if (isSelfReference(beanName, candidate) &&
							(!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
							//4. isAutowireCandidate：判断能不能注入，自己注入自己的情况
							isAutowireCandidate(candidate, fallbackDescriptor)) {
						//5.添加到结果集
						addCandidateEntry(result, candidate, descriptor, requiredType);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Add an entry to the candidate map: a bean instance if available or just the resolved
	 * type, preventing early bean initialization ahead of primary candidate selection.
	 */
	//在大部分场景中，addCandidateEntry 方法只会以返回该 candidateName 对应的类型，而不会提前实例该对象
	private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
			DependencyDescriptor descriptor, Class<?> requiredType) {
		// 1. 集合依赖，直接调用 getName(candidateName) 实例化
		if (descriptor instanceof MultiElementDescriptor) {
			//descriptor.resolveCandidate 基本上都是直接调用 getName(beanName) 实例化 bean
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			if (!(beanInstance instanceof NullBean)) {
				candidates.put(candidateName, beanInstance);
			}
		}
		// 2. 已经实例化，直接返回实例对象
		else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
				((StreamDependencyDescriptor) descriptor).isOrdered())) {
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
		}
		// 3. 只获取candidateName的类型，真正需要注入时才实例化对象
		else {
			candidates.put(candidateName, getType(candidateName));
		}
	}

	/**
	 * Determine the autowire candidate in the given set of beans.
	 * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
	 * @param candidates a Map of candidate names and candidate instances
	 * that match the required type, as returned by {@link #findAutowireCandidates}
	 * @param descriptor the target dependency to match against
	 * @return the name of the autowire candidate, or {@code null} if none found
	 */
	@Nullable
	protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
		Class<?> requiredType = descriptor.getDependencyType();
		String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
		if (primaryCandidate != null) {
			return primaryCandidate;
		}
		String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
		if (priorityCandidate != null) {
			return priorityCandidate;
		}
		// Fallback
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateName = entry.getKey();
			Object beanInstance = entry.getValue();
			if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
					matchesBeanName(candidateName, descriptor.getDependencyName())) {
				return candidateName;
			}
		}
		return null;
	}

	/**
	 * Determine the primary candidate in the given set of beans.
	 * @param candidates a Map of candidate names and candidate instances
	 * (or candidate classes if not created yet) that match the required type
	 * @param requiredType the target dependency type to match against
	 * @return the name of the primary candidate, or {@code null} if none found
	 * @see #isPrimary(String, Object)
	 */
	@Nullable
	protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		String primaryBeanName = null;
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (isPrimary(candidateBeanName, beanInstance)) {
				if (primaryBeanName != null) {
					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
					if (candidateLocal && primaryLocal) {
						throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
								"more than one 'primary' bean found among candidates: " + candidates.keySet());
					}
					else if (candidateLocal) {
						primaryBeanName = candidateBeanName;
					}
				}
				else {
					primaryBeanName = candidateBeanName;
				}
			}
		}
		return primaryBeanName;
	}

	/**
	 * Determine the candidate with the highest priority in the given set of beans.
	 * <p>Based on {@code @javax.annotation.Priority}. As defined by the related
	 * {@link org.springframework.core.Ordered} interface, the lowest value has
	 * the highest priority.
	 * @param candidates a Map of candidate names and candidate instances
	 * (or candidate classes if not created yet) that match the required type
	 * @param requiredType the target dependency type to match against
	 * @return the name of the candidate with the highest priority,
	 * or {@code null} if none found
	 * @see #getPriority(Object)
	 */
	@Nullable
	protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		String highestPriorityBeanName = null;
		Integer highestPriority = null;
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance != null) {
				Integer candidatePriority = getPriority(beanInstance);
				if (candidatePriority != null) {
					if (highestPriorityBeanName != null) {
						if (candidatePriority.equals(highestPriority)) {
							throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
									"Multiple beans found with the same priority ('" + highestPriority +
									"') among candidates: " + candidates.keySet());
						}
						else if (candidatePriority < highestPriority) {
							highestPriorityBeanName = candidateBeanName;
							highestPriority = candidatePriority;
						}
					}
					else {
						highestPriorityBeanName = candidateBeanName;
						highestPriority = candidatePriority;
					}
				}
			}
		}
		return highestPriorityBeanName;
	}

	/**
	 * Return whether the bean definition for the given bean name has been
	 * marked as a primary bean.
	 * @param beanName the name of the bean
	 * @param beanInstance the corresponding bean instance (can be null)
	 * @return whether the given bean qualifies as primary
	 */
	protected boolean isPrimary(String beanName, Object beanInstance) {
		if (containsBeanDefinition(beanName)) {
			return getMergedLocalBeanDefinition(beanName).isPrimary();
		}
		BeanFactory parent = getParentBeanFactory();
		return (parent instanceof DefaultListableBeanFactory &&
				((DefaultListableBeanFactory) parent).isPrimary(beanName, beanInstance));
	}

	/**
	 * Return the priority assigned for the given bean instance by
	 * the {@code javax.annotation.Priority} annotation.
	 * <p>The default implementation delegates to the specified
	 * {@link #setDependencyComparator dependency comparator}, checking its
	 * {@link OrderComparator#getPriority method} if it is an extension of
	 * Spring's common {@link OrderComparator} - typically, an
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}.
	 * If no such comparator is present, this implementation returns {@code null}.
	 * @param beanInstance the bean instance to check (can be {@code null})
	 * @return the priority assigned to that bean or {@code null} if none is set
	 */
	@Nullable
	protected Integer getPriority(Object beanInstance) {
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator) {
			return ((OrderComparator) comparator).getPriority(beanInstance);
		}
		return null;
	}

	/**
	 * Determine whether the given candidate name matches the bean name or the aliases
	 * stored in this bean definition.
	 */
	protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
		return (candidateName != null &&
				(candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
	}

	/**
	 * Determine whether the given beanName/candidateName pair indicates a self reference,
	 * i.e. whether the candidate points back to the original bean or to a factory method
	 * on the original bean.
	 */
	private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
		return (beanName != null && candidateName != null &&
				(beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
						beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
	}

	/**
	 * Raise a NoSuchBeanDefinitionException or BeanNotOfRequiredTypeException
	 * for an unresolvable dependency.
	 */
	private void raiseNoMatchingBeanFound(
			Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

		checkBeanNotOfRequiredType(type, descriptor);

		throw new NoSuchBeanDefinitionException(resolvableType,
				"expected at least 1 bean which qualifies as autowire candidate. " +
				"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
	}

	/**
	 * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
	 * i.e. if the target type of the bean would match but an exposed proxy doesn't.
	 */
	private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
		for (String beanName : this.beanDefinitionNames) {
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
			Class<?> targetType = mbd.getTargetType();
			if (targetType != null && type.isAssignableFrom(targetType) &&
					isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
				// Probably a proxy interfering with target type match -> throw meaningful exception.
				Object beanInstance = getSingleton(beanName, false);
				Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
						beanInstance.getClass() : predictBeanType(beanName, mbd));
				if (beanType != null && !type.isAssignableFrom(beanType)) {
					throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
				}
			}
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
		}
	}

	/**
	 * Create an {@link Optional} wrapper for the specified dependency.
	 */
	private Optional<?> createOptionalDependency(
			DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

		DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
			@Override
			public boolean isRequired() {
				return false;
			}
			@Override
			public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
				return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
						super.resolveCandidate(beanName, requiredType, beanFactory));
			}
		};
		Object result = doResolveDependency(descriptorToUse, beanName, null, null);
		return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
		sb.append(": defining beans [");
		sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
		sb.append("]; ");
		BeanFactory parent = getParentBeanFactory();
		if (parent == null) {
			sb.append("root of factory hierarchy");
		}
		else {
			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
				"just a SerializedBeanFactoryReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.serializationId != null) {
			return new SerializedBeanFactoryReference(this.serializationId);
		}
		else {
			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
		}
	}


	/**
	 * Minimal id reference to the factory.
	 * Resolved to the actual factory instance on deserialization.
	 */
	private static class SerializedBeanFactoryReference implements Serializable {

		private final String id;

		public SerializedBeanFactoryReference(String id) {
			this.id = id;
		}

		private Object readResolve() {
			Reference<?> ref = serializableFactories.get(this.id);
			if (ref != null) {
				Object result = ref.get();
				if (result != null) {
					return result;
				}
			}
			// Lenient fallback: dummy factory in case of original factory not found...
			DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
			dummyFactory.serializationId = this.id;
			return dummyFactory;
		}
	}


	/**
	 * A dependency descriptor marker for nested elements.
	 */
	private static class NestedDependencyDescriptor extends DependencyDescriptor {

		public NestedDependencyDescriptor(DependencyDescriptor original) {
			super(original);
			increaseNestingLevel();
		}
	}


	/**
	 * A dependency descriptor for a multi-element declaration with nested elements.
	 */
	private static class MultiElementDescriptor extends NestedDependencyDescriptor {

		public MultiElementDescriptor(DependencyDescriptor original) {
			super(original);
		}
	}


	/**
	 * A dependency descriptor marker for stream access to multiple elements.
	 */
	private static class StreamDependencyDescriptor extends DependencyDescriptor {

		private final boolean ordered;

		public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
			super(original);
			this.ordered = ordered;
		}

		public boolean isOrdered() {
			return this.ordered;
		}
	}


	private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
	}


	/**
	 * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
	 */
	private class DependencyObjectProvider implements BeanObjectProvider<Object> {

		private final DependencyDescriptor descriptor;

		private final boolean optional;

		@Nullable
		private final String beanName;

		public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			this.descriptor = new NestedDependencyDescriptor(descriptor);
			this.optional = (this.descriptor.getDependencyType() == Optional.class);
			this.beanName = beanName;
		}

		@Override
		public Object getObject() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			}
			else {
				Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		public Object getObject(final Object... args) throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName, args);
			}
			else {
				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
					@Override
					public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
						return beanFactory.getBean(beanName, args);
					}
				};
				Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		@Nullable
		public Object getIfAvailable() throws BeansException {
			// 用于解决嵌套的情况，像这种：ObjectProvider<Optional<T>>
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			}
			else {
				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
					@Override
					public boolean isRequired() {
						return false;
					}
				};
				// 最终还是会调用这个方法解决依赖
				return doResolveDependency(descriptorToUse, this.beanName, null, null);
			}
		}

		@Override
		@Nullable
		public Object getIfUnique() throws BeansException {
			DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
				@Override
				public boolean isRequired() {
					return false;
				}
				@Override
				@Nullable
				public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
					return null;
				}
			};
			if (this.optional) {
				return createOptionalDependency(descriptorToUse, this.beanName);
			}
			else {
				return doResolveDependency(descriptorToUse, this.beanName, null, null);
			}
		}

		@Nullable
		protected Object getValue() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			}
			else {
				return doResolveDependency(this.descriptor, this.beanName, null, null);
			}
		}

		@Override
		public Stream<Object> stream() {
			return resolveStream(false);
		}

		@Override
		public Stream<Object> orderedStream() {
			return resolveStream(true);
		}

		@SuppressWarnings("unchecked")
		private Stream<Object> resolveStream(boolean ordered) {
			DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
			Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
			return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
		}
	}


	/**
	 * Separate inner class for avoiding a hard dependency on the {@code javax.inject} API.
	 * Actual {@code javax.inject.Provider} implementation is nested here in order to make it
	 * invisible for Graal's introspection of DefaultListableBeanFactory's nested classes.
	 */
	private class Jsr330Factory implements Serializable {

		public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			return new Jsr330Provider(descriptor, beanName);
		}

		private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

			public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
				super(descriptor, beanName);
			}

			@Override
			@Nullable
			public Object get() throws BeansException {
				return getValue();
			}
		}
	}


	/**
	 * An {@link org.springframework.core.OrderComparator.OrderSourceProvider} implementation
	 * that is aware of the bean metadata of the instances to sort.
	 * <p>Lookup for the method factory of an instance to sort, if any, and let the
	 * comparator retrieve the {@link org.springframework.core.annotation.Order}
	 * value defined on it. This essentially allows for the following construct:
	 */
	private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

		private final Map<Object, String> instancesToBeanNames;

		public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
			this.instancesToBeanNames = instancesToBeanNames;
		}

		@Override
		@Nullable
		public Object getOrderSource(Object obj) {
			String beanName = this.instancesToBeanNames.get(obj);
			if (beanName == null || !containsBeanDefinition(beanName)) {
				return null;
			}
			RootBeanDefinition beanDefinition = getMergedLocalBeanDefinition(beanName);
			List<Object> sources = new ArrayList<>(2);
			Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				sources.add(factoryMethod);
			}
			Class<?> targetType = beanDefinition.getTargetType();
			if (targetType != null && targetType != obj.getClass()) {
				sources.add(targetType);
			}
			return sources.toArray();
		}
	}

}
