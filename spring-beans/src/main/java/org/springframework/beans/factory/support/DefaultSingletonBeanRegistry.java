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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
//AliasRegistry：提供别名注册的接口
//SimpleAliasRegistry：它简单实现了AliasRegistry接口
//SingletonBeanRegistry：提供单例bean注册的接口
//DefaultSingletonBeanRegistry：SingletionBean注册器的默认实现，同时继承SimpleAliasRegistry，因此这个类有别名注册的功能和单例bean注册的功能
//并且支持注册DisposableBean实例，它依赖ObjectFactory接口和DisposableBean接口（关闭注册表时调用了destroy方法）
//ObjectFactory：这个接口通常用于封装一个通用的工厂，它只有一个方法getObject()，返回一个新的实例
//DisposableBean：接口实现为bean要破坏释放资源，它只有一个destroy，由一个破坏一个singleton的BeanFactory调用


/**
 * 1、共享bean实例的注册表，实现了SingletonBeanRegistry，允许注册表中注册的单例被所有调用者共享，通过bean名称获得
 * 可以登记DisposableBean实例，关闭注册表是destroy
 * 可以注册bean之间的依赖关系，执行适当的关闭顺序
 * 2、作用
 * 存储单例bean
 * 存储bean之间的依赖关系
 * 存储bean的包含关系（外部类包含内部类）
 * bean所处的状态
 * 负责单例bean的销毁
 * 3、解决setter循环依赖
 * bean创建会经过三个过程
 * （1）createBeanInstance：调用对象的构造方法实例化对象
 * （2）populateBean：填充属性，包括依赖注入
 * （3）initializeBean：调用init 方法
 * 为了方便说明，这里假设有两个bean，a与b，a与b相互通过setter函数依赖
 * --a先通过createBeanInstance函数初始化，a是不完全的，某些依赖（b）还没有注入，会调用DefaultSingletonBeanRegistry的addSingletonFactory函数
 * a被封装成ObjectFactory，保存进singletonFactories中，
 * --a发现自己依赖于b，调用getSingleton(String beanName)函数，该函数返回值为null，a发现b还没有初始化，
 * 此时会使用createBeanInstance函数初始化b
 * --b发现自己依赖于a，调用getSingleton(String beanName)函数，这个函数首先会查找singletonObjects缓存（没有查到a），
 * 接着查找earlySingletonObjects（没有查找到A），接着会查找singletonFactories，此时会查到之前封装了a的ObjectFactory，调用其getObject方法，获得a，
 * 接着将a放入earlySingletonObjects，此时b获得了a实例，完成初始化，调用addSingleton(String beanName, Object singletonObject)方法，将自己放入
 * singletonObjects缓存中，b初始化完成后，a也能完成初始化，调用addSingleton(String beanName, Object singletonObject)方法，
 * 从earlySingletonObjects缓存进入singletonObjects缓存，由于a可以率先初始化，所以上述过程可以完成，如果a的构造函数依赖于b，b的构造函数又依赖a，这样两者
 * 都无法率先初始化，两者都无法率先进入singletonFactories缓存
 * （1）singletonObjects：存储完全实例化的Bean
 * （2）earlySingletonObjects：存储具有循环依赖的Bean
 * （3）singletonFactories：存储未完全实例化完毕的Bean（某些依赖未注入）
 * earlySingletonObjects中有元素意味着有setter循环依赖存在
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Cache of singleton objects: bean name to bean instance. */
	//缓存完全实例化的bean:从beanname到bean实例，1级缓存
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	//缓存未完全实例化的bean，缓存单实例工厂:从beanname到ObjectFactory，ObjectFactory通过getObject方法取得了earlySingletonBean，然后再由earlySingletonBean成为bean实例，3级缓存
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	//缓存具有setter循环依赖的未完全实例化的bean，缓存早期的单实例对象:从beanname到bean实例，2级缓存
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

	/** Set of registered singletons, containing the bean names in registration order. */
	//缓存已经实例化的bean的名称，按注册顺序排列的beanname
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/** Names of beans that are currently in creation. */
	//目前正在创建的bean的名称（构造函数依赖于其他未初始化的bean）
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** Names of beans currently excluded from in creation checks. */
	// 当前从创建检查中排除的beanName
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** List of suppressed Exceptions, available for associating related causes. */
	// 当从ObjectFactory获得对象时出现异常，把suppressedExceptions的异常一并抛出，作用不大
	@Nullable
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons. */
	// 当前是否处于销毁bean的状态，若为true，则无法获得bean
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name to disposable instance. */
	// 所有实现了DisposableBean接口的bean，在销毁bean时，会回调该缓存中bean的destroy方法
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
	// 缓存所有外部类与内部类的关系，被包含关系（key被value所包含），key是被包含的bean，value则是包含该bean的所有bean
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** Map between dependent bean names: bean name to Set of dependent bean names. */
	// 缓存bean名称和所有依赖于bean名称的集合，依赖关系（key被value所依赖），key是被依赖的bean，value则是依赖该bean的所有bean，比如bcd依赖a，那么就是key为a，bcd为value
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
	// 缓存bean名称和bean所依赖的所有名称的集合，依赖关系（key依赖于value），key表示的bean依赖于value表示的bean，比如a依赖bcd，那么就是key为a，bcd为value
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

	//登记完全实例化的bean
	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (this.singletonObjects) {
			// 确保bean已经完全初始化（实例化+注入全部依赖）
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			// 缓存没有，开始注册
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	//完全实例化的bean加入到缓存中
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			// 加入完全实例化的bean的缓存
			this.singletonObjects.put(beanName, singletonObject);
			// 既然加入完全实例化的bean的缓存，那singletonFactories和earlySingletonObjects就不再持有
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			// 登记已经完全初始化的bean
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	//添加给定的单例工厂以生成指定的单例对象
	//将未完全实例化的bean（有些依赖未注入）封装成ObjectFactory类型后存储到singletonFactories缓存中
	//目的：为了解决setter循环依赖
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			//确保bean未完全初始化
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		// 允许早期依赖
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	/**
	 * 这个函数一般用于获得依赖bean或是完全初始化的bean，按以下顺序获取
	 * 1、从singletonObjects缓存中获取，意味着依赖Bean已经完全初始化（实例化+注入全部依赖）
	 * 2、从earlySingletonObjects缓存中获取，意味着依赖Bean出现了setter循环依赖，这里获得的是引用
	 * 3、从singletonFactories缓存中获取，意味着依赖Bean也还没有完全初始化，这里获得的是引用
	 * @param beanName
	 * @param allowEarlyReference
	 * @return
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//如果完全实例化的bean有直接返回，1级缓存
		Object singletonObject = this.singletonObjects.get(beanName);
		//isSingletonCurrentlyInCreation函数用于确保Bean正在创建
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//缓存没有的情况但正在创建
			synchronized (this.singletonObjects) {
				//如果早期缓存中有，说明正在加载，则不处理直接返回，2级缓存
				singletonObject = this.earlySingletonObjects.get(beanName);
				//allowEarlyReference允许是否从singletonFactories读取
				if (singletonObject == null && allowEarlyReference) {
					// 某些方法提前初始化的时候会调用addSingletonFactory，ObjectFactory缓存在singletonFactories中，3级缓存
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						//如果singletonFactories有，调用getObject方法返回，得到对象，放入二级缓存，清空三级缓存
						singletonObject = singletonFactory.getObject();
						// singletonFactories产生的对象放入earlySingletonObjects中
						this.earlySingletonObjects.put(beanName, singletonObject);
						// 因为单例bean只会被创建一次，所有两者是互斥的，已经产生过一次对象了，所以就不能再用了，后面直接用earlySingletonObjects获取
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	//这个函数直接从ObjectFactory获得bean，直接存储到singletonObjects缓存中，用于创建单例bean
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			// 从缓存中检查一遍
			// 因为 singleton 模式其实就是复用已经创建的 bean 所以这步骤必须检查
			// 完全实例化的bean缓存有直接返回
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				// 当前是否处于Bean销毁状态，若处于，则抛出异常，此时不能获得Bean
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				// 加载前置处理，记录加载单例 bean 之前的加载状态
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				// 如果当前没有异常，初始化异常集合
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				//获取不到的时候，从对象工厂获取
				try {
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					// 有可能是其他方式创建的bean
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 后置处理
					afterSingletonCreation(beanName);
				}
				//如果创建一个单例，将单例加到单例对象集中
				if (newSingleton) {
					//加到一级缓存，清除2、3级缓存
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	//注册过程中发生的异常，加入到异常集合
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	//移除特定的Bean的缓存
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		// 是否已经缓存过
		return this.singletonObjects.containsKey(beanName);
	}

	//获取注册表中单实例对象的beanNames
	@Override
	public String[] getSingletonNames() {
		//获取已经注册过的bean
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	//获取注册表中的单实例数量
	@Override
	public int getSingletonCount() {
		// 获取单例的个数
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	//设置这个bean当前时刻是否在创建中
	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	//这三个函数用于判断单例bean是否正在被创建
	//返回false有两种可能
	//1、beanName不是单例bean
	//2、单例bean未处于创建状态
	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		// 如果这个beanName在不检查集合里，返回false，说明当前没有创建
		// 如果这个beanName要检查，那就要返回是否是正在创建的bean
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	//指定的单例bean当前是否正在创建
	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	//当前是否处于Bean创建状态
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	//在单实例对象创建之前调用
	//判断bean是否处于初始化阶段，如果bean已经处于初始化阶段，则抛出异常，这个函数用于确保单例唯一
	protected void beforeSingletonCreation(String beanName) {
		// 首先判断是否包含在被排除的bean名称集合中
		// 首先不包含，然后是添加成功到当前正在创建bean集合
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	//在单实例对象创建完成后调用
	//表明单例bean初始化完毕
	protected void afterSingletonCreation(String beanName) {
		// 首先判定是否包含在被排除的bean名称集合中
		// 然后从正在创建单实例集合中删除beanName
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	//注册一次性bean实例，注册实现了DisposableBean接口的类
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	//记录具有包含关系的bean（内部类与外部类）
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			// 如果没有key为containingBeanName的value，说明内部bean集合为空，则初始化一个
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			//注册给定的bean
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		//注册给定bean的依赖bean
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	//记录dependentBeanName依赖于beanName，分为两步，
	// 第一步将dependentBeanName加入到依赖于beanName的bean的记录项中
	// 第二步将beanName加入到dependentBeanName的依赖记录项中
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			//注册beanName依赖的dependentBeanName
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			//注册被beanName依赖的bean
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	//这个函数用于判断dependentBeanName是否直接或间接依赖于beanName
	//bean与bean之间的依赖关系可以构成一张图或是一颗树，这里采用深度优先算法遍历beanName的依赖树，这个方法在初始化时，会检查依据depends-on属性形成的依赖图中是否有循环依赖
	protected boolean isDependent(String beanName, String dependentBeanName) {
		//锁定当前bean依赖的bean名称集合，beanName当前bean,dependentBeanName指依赖的bean
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		// alreadySeen 已经检测的依赖 bean
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		//将bean的别名解析为规范名称，获取原始 beanName
		String canonicalName = canonicalName(beanName);
		//获取当前beanName的依赖集合
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		//不存在，则返回false
		if (dependentBeans == null) {
			return false;
		}
		//如果依赖集合包含指定的依赖bean，则返回true
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		//循环递归判定给定bean所依赖的bean集合中的所有bean中是否有依赖指定dependentBeanName名称的bean
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			//递归判定指定bean所依赖的bean的集合是否依赖指定dependentBeanName名称的bean(即依赖传递性)
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	//判断是否有bean依赖于beanName对应的bean
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	//返回给定beanName依赖的bean集合
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	//返回给定beanName被依赖的集合
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	//销毁当前实例对象
	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		//标记当前正在销毁实例
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			//将需要销毁的实例对象封装为数组
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		//循环销毁实例对象
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}
		//清除所有存储bean之间关系的缓存
		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	//清楚所有的bean缓存
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	//销毁指定的实例对象
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		// 先从单例池中移除掉
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			//删除注册表中销毁单实例对象
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		//执行销毁逻辑
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	//销毁给定beanName的单实例对象，并且要先销毁依赖它的bean
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			// 在同步环境中删除依赖的bean
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			//依次删除依赖于beanName的bean
			for (String dependentBeanName : dependencies) {
				//销毁单实例bean
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		// 真正的销毁bean
		if (bean != null) {
			try {
				//执行destroy方法
				//这个bean就是DisposableBeanAdapter
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		// 销毁包含关系的bean，由此可以确定，A依赖于B（A、B均实现了DisposableBean接口），则A的destroy方法比B的先调用
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		//清除记录beanName的依赖关系的缓存
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	//单实例互斥体集合，向子类或外部协作者公开单例互斥体，如果子类执行任何类型的扩展单例创建阶段，那么它们应该在给定对象上加同步锁
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
