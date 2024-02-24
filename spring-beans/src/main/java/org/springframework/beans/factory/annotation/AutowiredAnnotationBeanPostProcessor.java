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

package org.springframework.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that autowires annotated fields, setter methods and arbitrary config methods.
 * Such members to be injected are detected through a Java 5 annotation: by default,
 * Spring's {@link Autowired @Autowired} and {@link Value @Value} annotations.
 *
 * <p>Also supports JSR-330's {@link javax.inject.Inject @Inject} annotation,
 * if available, as a direct alternative to Spring's own {@code @Autowired}.
 *
 * <p>Only one constructor (at max) of any given bean class may declare this annotation
 * with the 'required' parameter set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Spring bean. If multiple <i>non-required</i> constructors
 * declare the annotation, they will be considered as candidates for autowiring.
 * The constructor with the greatest number of dependencies that can be satisfied by
 * matching beans in the Spring container will be chosen. If none of the candidates
 * can be satisfied, then a primary/default constructor (if present) will be used.
 * If a class only declares a single constructor to begin with, it will always be used,
 * even if not annotated. An annotated constructor does not have to be public.
 *
 * <p>Fields are injected right after construction of a bean, before any
 * config methods are invoked. Such a config field does not have to be public.
 *
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Spring container.
 * Bean property setter methods are effectively just a special case of such a
 * general config method. Config methods do not have to be public.
 *
 * <p>Note: A default AutowiredAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom AutowiredAnnotationBeanPostProcessor bean definition.
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
 * thus the latter configuration will override the former for properties wired through
 * both approaches.
 *
 * <p>In addition to regular injection points as discussed above, this post-processor
 * also handles Spring's {@link Lookup @Lookup} annotation which identifies lookup
 * methods to be replaced by the container at runtime. This is essentially a type-safe
 * version of {@code getBean(Class, args)} and {@code getBean(String, args)},
 * See {@link Lookup @Lookup's javadoc} for details.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 2.5
 * @see #setAutowiredAnnotationType
 * @see Autowired
 * @see Value
 */
/**
 * 1、@Autowired注解实现类
 * 实现了BeanPostProcessor接口，它自动绑定注解的field，setter方法和任意配置方法。AutowiredAnnotationBeanPostProcessor
 * 间接继承了BeanPostProcessor，它自动绑定注解的field，setter方法和任意的配置方法。当检测到5个java注解时这些成员被注入其中
 * 2、Spring的IOC容器，我们只需要声明Bean，Spring会自动帮我们进行依赖注入，这个活就是AutowiredAnnotationBeanPostProcessor干的。
 * 它实现了BeanPostProcessor接口，代表要针对Bean进行扩展，依赖注入也属于是针对Bean的一个扩展。
 * 3、https://blog.csdn.net/qq_35512802/article/details/132083187
 */
public class AutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	//保存了该类会处理的注解
	//添加了三个注解@Autowired、@Value、和通过反射得到的javax.inject.Inject
	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

	private String requiredParameterName = "required";

	private boolean requiredParameterValue = true;

	private int order = Ordered.LOWEST_PRECEDENCE - 2;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

	private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * Create a new {@code AutowiredAnnotationBeanPostProcessor} for Spring's
	 * standard {@link Autowired @Autowired} annotation.
	 * <p>Also supports JSR-330's {@link javax.inject.Inject @Inject} annotation,
	 * if available.
	 */
	@SuppressWarnings("unchecked")
	public AutowiredAnnotationBeanPostProcessor() {
		this.autowiredAnnotationTypes.add(Autowired.class);
		this.autowiredAnnotationTypes.add(Value.class);
		try {
			this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
			logger.trace("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}


	/**
	 * Set the 'autowired' annotation type, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided {@link Autowired}
	 * annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a member is supposed
	 * to be autowired.
	 */
	public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
		Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.add(autowiredAnnotationType);
	}

	/**
	 * Set the 'autowired' annotation types, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided {@link Autowired}
	 * annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation types to indicate that a member is supposed
	 * to be autowired.
	 */
	public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
		Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
	}

	/**
	 * Set the name of a parameter of the annotation that specifies whether it is required.
	 * @see #setRequiredParameterValue(boolean)
	 */
	public void setRequiredParameterName(String requiredParameterName) {
		this.requiredParameterName = requiredParameterName;
	}

	/**
	 * Set the boolean value that marks a dependency as required
	 * <p>For example if using 'required=true' (the default), this value should be
	 * {@code true}; but if using 'optional=false', this value should be {@code false}.
	 * @see #setRequiredParameterName(String)
	 */
	public void setRequiredParameterValue(boolean requiredParameterValue) {
		this.requiredParameterValue = requiredParameterValue;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}


	//查询@Autowired @Value属性
	//实例化之后会调用所有MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition方法
	//找到所有的注入点，其实就是被@Autowired注解修饰的方法以及字段，同时静态的方法以及字段也会被排除
	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		//寻找bean中所有被@Autowired @value方法method或属性field，并将属性封装成InjectedElement类型
		//找出所有需要完成注入的点，@Autowired @value注解方法或者属性
		InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
		//injectedElements做了一个复制
		//且把将Field或Method记录到BeanDefinition中的externallyManagedConfigMembers中，表示该Field或Method是BeanFactory外部管理的
		metadata.checkConfigMembers(beanDefinition);
	}

	@Override
	public void resetBeanDefinition(String beanName) {
		this.lookupMethodsChecked.remove(beanName);
		this.injectionMetadataCache.remove(beanName);
	}

	//筛选候选构造函数
	//触发时机：bean实例化时候，获取bean的构造函数
	//其作用是从注入bean的所有构造函数中过滤出可以作为构造注入的构造函数列表
	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
			throws BeanCreationException {

		// Let's check for lookup methods here...
		// 在这里首先处理了@Lookup注解
		// 判断是否已经解析过 。lookupMethodsChecked 作为一个缓存集合，保存已经处理过的bean
		if (!this.lookupMethodsChecked.contains(beanName)) {
			try {
				// 遍历bean中的每一个方法
				ReflectionUtils.doWithMethods(beanClass, method -> {
					// 判断 方法是否被 @Lookup 修饰
					Lookup lookup = method.getAnnotation(Lookup.class);
					if (lookup != null) {
						Assert.state(this.beanFactory != null, "No BeanFactory available");
						// 如果被@Lookup 修饰，则封装后保存到RootBeanDefinition 的methodOverrides 属性中，
						// 在 SimpleInstantiationStrategy#instantiate(RootBeanDefinition, String, BeanFactory)
						// 进行了cglib的动态代理。
						LookupOverride override = new LookupOverride(method, lookup.value());
						try {
							RootBeanDefinition mbd = (RootBeanDefinition)
									this.beanFactory.getMergedBeanDefinition(beanName);
							mbd.getMethodOverrides().addOverride(override);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(beanName,
									"Cannot apply @Lookup to beans without corresponding bean definition");
						}
					}
				});
			}
			catch (IllegalStateException ex) {
				throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
			}
			// 将已经解析好的beanName 添加到缓存中
			this.lookupMethodsChecked.add(beanName);
		}

		// Quick check on the concurrent map first, with minimal locking.
		// 这里开始处理构造函数
		// 获取bean的所有候选构造函数

		//1、解析@Lookup 注解的方法，保存到 RootBeanDefinition 中
		//2、从缓存中获取筛选好的构造函数列表，若有直接返回，没有则进行下一步
		//3、通过反射获取bean 的所有构造函数，并进行构造函数遍历
		//筛选每个构造函数是否被 @Autowired @Inject注解修饰
		//4、当前构造函数没有被修饰，则判断当前bean是否Cglib动态代理类
		//如果是，则获取原始类的构造函数
		//再判断 构造函数是否被 @Autowired、@Inject 注解修饰
		//5、如果筛选出候选构造函数
		//	如果有一个必须注入的构造函数(@Autowired(required =true)或者 @Inject )
		//		则不允许有其他候选构造函数出现
		//		有且只能筛选出一个必须注入的构造函数
		//	如果不存在必须注入的构造含函数 （@Autowired(required =false) 或者 @Inject)
		//		则允许多个候选注入构造函数出现（@Autowired(required = false) 修饰的构造函数）
		//		并且将这个几个候选构造函数返回
		//	如果bean有且只有一个构造函数
		//		即使没有被注解修饰，也会调用该构造函数作为bean创建的构造函使用

		Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
		if (candidateConstructors == null) {
			// Fully synchronized resolution now...
			synchronized (this.candidateConstructorsCache) {
				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
				// 如果候选构造构造函数为空
				if (candidateConstructors == null) {
					Constructor<?>[] rawCandidates;
					try {
						// 获取所有访问权限的构造函数
						rawCandidates = beanClass.getDeclaredConstructors();
					}
					catch (Throwable ex) {
						throw new BeanCreationException(beanName,
								"Resolution of declared constructors on bean Class [" + beanClass.getName() +
								"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
					}
					// 根据所有构造函数数量创建候选集合
					List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
					// @Autowired(required = true) 的构造函数,有且只能有一个
					Constructor<?> requiredConstructor = null;
					// 默认的无参构造函数
					Constructor<?> defaultConstructor = null;
					// 针对 Kotlin 语言的构造函数，不太明白，一般为null
					Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
					int nonSyntheticConstructors = 0;
					for (Constructor<?> candidate : rawCandidates) {
						// 构造函数是否是非合成
						// 一般我们自己创建的都是非合成的
						// Java在编译过程中可能会出现一些合成的构造函数
						if (!candidate.isSynthetic()) {
							nonSyntheticConstructors++;
						}
						else if (primaryConstructor != null) {
							continue;
						}
						// 遍历autowiredAnnotationTypes集合
						// 判断当前构造函数是否被autowiredAnnotationTypes集合中的注解修饰,若未被修饰，则返回null
						// autowiredAnnotationTypes 集合中的注解在一开始就说了是 @Autowired、@Value 和 @Inject 三个。
						AnnotationAttributes ann = findAutowiredAnnotation(candidate);
						if (ann == null) {
							// 如果未被修饰，这里判断是否是 Cglib 的代理类，如果是则获取原始类，否则直接返回beanClass
							Class<?> userClass = ClassUtils.getUserClass(beanClass);
							// 如果这里不相等，肯定是通过 cglib的代理类，这里的userClass 就是原始类
							// 再次判断构造函数是否包含指定注解
							if (userClass != beanClass) {
								try {
									Constructor<?> superCtor =
											userClass.getDeclaredConstructor(candidate.getParameterTypes());
									ann = findAutowiredAnnotation(superCtor);
								}
								catch (NoSuchMethodException ex) {
									// Simply proceed, no equivalent superclass constructor found...
								}
							}
						}
						if (ann != null) {
							// 如果已经找到了必须装配的构造函数(requiredConstructor  != null)
							// 那么当前这个就是多余的，则抛出异常
							if (requiredConstructor != null) {
								throw new BeanCreationException(beanName,
										"Invalid autowire-marked constructor: " + candidate +
										". Found constructor with 'required' Autowired annotation already: " +
										requiredConstructor);
							}
							// 确定是否是必须的，@Autowired 和 @Inject 默认为true
							// @Autowired 可以通过 required 修改
							boolean required = determineRequiredStatus(ann);
							if (required) {
								// 如果当前构造函数为必须注入，但是候选列表不为空，则说明已经有构造函数适配，则抛出异常
								// 就是只要有required = true的构造函数就不允许存在其他可注入的构造函数
								if (!candidates.isEmpty()) {
									throw new BeanCreationException(beanName,
											"Invalid autowire-marked constructors: " + candidates +
											". Found constructor with 'required' Autowired annotation: " +
											candidate);
								}
								// 到这一步，说明当前构造函数是必须的，且目前没有其他构造函数候选
								// 直接将当前构造函数作为必须构造函数
								requiredConstructor = candidate;
							}
							// 添加到候选列表
							candidates.add(candidate);
						}
						// 如果 构造函数参数数量为0，则是默认构造函数，使用默认构造函数
						else if (candidate.getParameterCount() == 0) {
							defaultConstructor = candidate;
						}
					}
					// 如果候选构造函数不为空
					if (!candidates.isEmpty()) {
						// Add default constructor to list of optional constructors, as fallback.
						// 将默认构造函数添加到可选构造函数列表中，作为回退
						if (requiredConstructor == null) {
							if (defaultConstructor != null) {
								candidates.add(defaultConstructor);
							}
							else if (candidates.size() == 1 && logger.isInfoEnabled()) {
								logger.info("Inconsistent constructor declaration on bean with name '" + beanName +
										"': single autowire-marked constructor flagged as optional - " +
										"this constructor is effectively required since there is no " +
										"default constructor to fall back to: " + candidates.get(0));
							}
						}
						candidateConstructors = candidates.toArray(new Constructor<?>[0]);
					}
					// 如果 当前bean只有一个有参构造函数，那么将此构造函数作为候选列表返回
					// (这就代表，如果bean中只有一个有参构造函数并不需要使用特殊注解，也会作为构造函数进行注入)
					else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
						candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
					}
					else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
							defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
					}
					else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor};
					}
					else {
						candidateConstructors = new Constructor<?>[0];
					}
					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
				}
			}
		}
		return (candidateConstructors.length > 0 ? candidateConstructors : null);
	}

	//把缓存中已经被解析的@Autowired、@Value、@Resource的属性和方法拿出来，属性赋值进bean实例中
	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		//再次调用findAutowiringMetadata方法，此时为第二次调用将会直接从this.injectionMetadataCache中获取Bean所需要的依赖注入Bean
		InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
		try {
			//执行Bean配置的注解依赖注入
			metadata.inject(bean, beanName, pvs);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
		}
		return pvs;
	}

	@Deprecated
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return postProcessProperties(pvs, bean, beanName);
	}

	/**
	 * 'Native' processing method for direct calls with an arbitrary target instance,
	 * resolving all of its fields and methods which are annotated with {@code @Autowired}.
	 * @param bean the target instance to process
	 * @throws BeanCreationException if autowiring failed
	 */
	public void processInjection(Object bean) throws BeanCreationException {
		Class<?> clazz = bean.getClass();
		InjectionMetadata metadata = findAutowiringMetadata(clazz.getName(), clazz, null);
		try {
			metadata.inject(bean, null, null);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					"Injection of autowired dependencies failed for class [" + clazz + "]", ex);
		}
	}


	private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		// 1.设置cacheKey的值（beanName 或者 className）
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		// 2.检查beanName对应的InjectionMetadata是否已经存在于缓存中

		// 从缓存中获取metadata，第一次执行到这里，肯定没有，继续往下解析
		//当第二次调用的时候，将会是待处理Bean实例化完成开始组装的时候，通过这里将会直接获取到想要依赖注入的Bean的信息
		//这里获取的缓存就是下面this.injectionMetadataCache.put(cacheKey, metadata)添加的
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		// 3.检查InjectionMetadata是否需要刷新（为空或者class变了）
		//判断当前Bean是否需要依赖注入元数据解析
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				// 4.加锁后，再次从缓存中获取beanName对应的InjectionMetadata
				metadata = this.injectionMetadataCache.get(cacheKey);
				// 5.加锁后，再次检查InjectionMetadata是否需要刷新
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						// 6.如果需要刷新，并且metadata不为空，则先移除
						metadata.clear(pvs);
					}
					// 7.解析@Autowired注解的信息，生成元数据（包含clazz和clazz里解析到的注入的元素，
					// 这里的元素包括AutowiredFieldElement和AutowiredMethodElement）
					metadata = buildAutowiringMetadata(clazz);
					// 8.将解析的元数据放到injectionMetadataCache缓存，以备复用，每一个类只解析一次
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	//1、遍历当前bean中的所有属性和方法，过滤静态属性和方法
	//2、属性：将field、required封装成AutowiredFieldElement
	//3、方法：将method、required、pd(获取method的属性描述器)封装成AutowiredMethodElement
	//4、将解析的元数据放到injectionMetadataCache缓存，以后统一处理
	private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
		// 1.用于存放所有解析到的注入的元素的变量
		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;
		// 2.循环遍历
		do {
			// 2.1 定义存放当前循环的Class注入的元素(有序)
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
			// 2.2 如果targetClass的属性上有@Autowired @Value注解，则用工具类获取注解信息
			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				// 2.2.1 获取field上的@Autowired注解信息
				AnnotationAttributes ann = findAutowiredAnnotation(field);
				if (ann != null) {
					// 2.2.2 校验field是否被static修饰
					// 如果是则直接返回，因为@Autowired注解不支持static修饰的field
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static fields: " + field);
						}
						return;
					}
					// 2.2.3 获取@Autowired注解的required的属性值
					//（required：值为true时，如果没有找到bean时，自动装配应该失败；false则不会）
					boolean required = determineRequiredStatus(ann);
					// 2.2.4 将field、required封装成AutowiredFieldElement，添加到currElements
					currElements.add(new AutowiredFieldElement(field, required));
				}
			});
			// 2.3 如果targetClass的方法上有@Autowired注解，则用工具类获取注解信息
			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				// 2.3.1 找到桥接方法
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				// 2.3.2 判断方法的可见性，如果不可见则直接返回
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				// 2.3.3 获取method上的@Autowired注解信息
				AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
				if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					// 2.3.4 校验method是否被static修饰，如果是则直接返回
					// 因为@Autowired注解不支持static修饰的method
					if (Modifier.isStatic(method.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static methods: " + method);
						}
						return;
					}
					// 2.3.5 @Autowired注解标识在方法上的目的就是将容器内的Bean注入到方法的参数中，没有参数就违背了初衷
					if (method.getParameterCount() == 0) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation should only be used on methods with parameters: " +
									method);
						}
					}
					// 2.3.6 获取@Autowired注解的required的属性值
					boolean required = determineRequiredStatus(ann);
					// 2.3.7  获取method的属性描述器
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
					// 2.3.8 将method、required、pd封装成AutowiredMethodElement，添加到currElements
					currElements.add(new AutowiredMethodElement(method, required, pd));
				}
			});
			// 2.4 将本次循环获取到的注解信息添加到elements
			elements.addAll(0, currElements);
			// 2.5 在解析完targetClass之后，递归解析父类
			// 将所有的@Autowired的属性和方法收集起来，且类的层级越高其属性会被越优先注入
			targetClass = targetClass.getSuperclass();
		}
		// 2.6 递归解析targetClass父类(直至父类为Object结束)
		while (targetClass != null && targetClass != Object.class);
		// 2.7 将clazz和解析到的注入的元素封装成InjectionMetadata
		return new InjectionMetadata(clazz, elements);
	}

	@Nullable
	private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao) {
		if (ao.getAnnotations().length > 0) {  // autowiring annotations have to be local
			for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
				AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
				if (attributes != null) {
					return attributes;
				}
			}
		}
		return null;
	}

	/**
	 * Determine if the annotated field or method requires its dependency.
	 * <p>A 'required' dependency means that autowiring should fail when no beans
	 * are found. Otherwise, the autowiring process will simply bypass the field
	 * or method when no beans are found.
	 * @param ann the Autowired annotation
	 * @return whether the annotation indicates that a dependency is required
	 */
	protected boolean determineRequiredStatus(AnnotationAttributes ann) {
		return (!ann.containsKey(this.requiredParameterName) ||
				this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
	}

	/**
	 * Obtain all beans of the given type as autowire candidates.
	 * @param type the type of the bean
	 * @return the target beans, or an empty Collection if no bean of this type is found
	 * @throws BeansException if bean retrieval failed
	 */
	protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
		if (this.beanFactory == null) {
			throw new IllegalStateException("No BeanFactory configured - " +
					"override the getBeanOfType method or specify the 'beanFactory' property");
		}
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
	}

	/**
	 * Register the specified bean as dependent on the autowired beans.
	 */
	private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
		if (beanName != null) {
			for (String autowiredBeanName : autowiredBeanNames) {
				if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Autowiring by type from bean name '" + beanName +
							"' to bean named '" + autowiredBeanName + "'");
				}
			}
		}
	}

	/**
	 * Resolve the specified cached method argument or field value.
	 */
	@Nullable
	private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
		if (cachedArgument instanceof DependencyDescriptor) {
			DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
		}
		else {
			return cachedArgument;
		}
	}


	/**
	 * Class representing injection information about an annotated field.
	 */
	private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached = false;

		@Nullable
		private volatile Object cachedFieldValue;

		public AutowiredFieldElement(Field field, boolean required) {
			super(field, null);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			// 1.拿到该元数据的属性值
			Field field = (Field) this.member;
			Object value;
			// 2.如果缓存中已经存在，则直接从缓存中解析属性（原型Bean）
			if (this.cached) {
				// 对于原型Bean，第一次创建的时候，也找注入点，然后进行注入，此时cached为false，注入完了之后cached为true
				// 第二次创建的时候，先找注入点（此时会拿到缓存好的注入点），也就是AutowiredFieldElement对象，此时cache为true，也就进到此处了
				// 注入点内并没有缓存被注入的具体Bean对象，而是beanName，这样就能保证注入到不同的原型Bean对象
				value = resolvedCachedArgument(beanName, this.cachedFieldValue);
			}
			else {
				// 3.把field和required属性，包装成desc描述类
				DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
				desc.setContainingClass(bean.getClass());
				Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
				Assert.state(beanFactory != null, "No BeanFactory available");
				TypeConverter typeConverter = beanFactory.getTypeConverter();
				try {
					// 4.核心逻辑: 进行依赖查找，找到当前字段所匹配的Bean对象
					value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
				}
				synchronized (this) {
					if (!this.cached) {
						// 5.value不为空或者required为true
						if (value != null || this.required) {
							// 6.如果属性依赖注入的bean不止一个（Array,Collection,Map），
							// 缓存cachedFieldValue放的是DependencyDescriptor
							this.cachedFieldValue = desc;
							// 7.注册依赖关系到缓存（beanName 依赖 autowiredBeanNames）
							registerDependentBeans(beanName, autowiredBeanNames);
							// 8.如果属性依赖注入的bean只有一个（正常都是一个）
							if (autowiredBeanNames.size() == 1) {
								String autowiredBeanName = autowiredBeanNames.iterator().next();
								// @Autowired标识属性类型和Bean的类型要匹配
								// 因此Array,Collection,Map类型的属性不支持缓存属性Bean名称
								// 9.检查autowiredBeanName对应的bean的类型是否为field的类型
								if (beanFactory.containsBean(autowiredBeanName) &&
										beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
									// 10.将该属性解析到的bean的信息封装成ShortcutDependencyDescriptor，
									// 以便之后可以通过getBean方法来快速拿到bean实例
									this.cachedFieldValue = new ShortcutDependencyDescriptor(
											desc, autowiredBeanName, field.getType());
								}
							}
						}
						else {
							this.cachedFieldValue = null;
						}
						// 11.缓存标识设为true

						this.cached = true;
					}
				}
			}
			if (value != null) {
				// 12.设置字段访问性
				ReflectionUtils.makeAccessible(field);
				// 13.通过反射为属性赋值，将解析出来的bean实例赋值给field
				field.set(bean, value);
			}
		}
	}


	/**
	 * Class representing injection information about an annotated method.
	 */
	private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached = false;

		@Nullable
		private volatile Object[] cachedMethodArguments;

		public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
			super(method, pd);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			if (checkPropertySkipping(pvs)) {
				return;
			}
			Method method = (Method) this.member;
			Object[] arguments;
			if (this.cached) {
				// Shortcut for avoiding synchronization...
				arguments = resolveCachedArguments(beanName);
			}
			else {
				// 通过方法参数类型构造依赖描述符
				// 逻辑基本一样的，最终也是调用beanFactory.resolveDependency方法
				Class<?>[] paramTypes = method.getParameterTypes();
				arguments = new Object[paramTypes.length];
				DependencyDescriptor[] descriptors = new DependencyDescriptor[paramTypes.length];
				Set<String> autowiredBeans = new LinkedHashSet<>(paramTypes.length);
				Assert.state(beanFactory != null, "No BeanFactory available");
				TypeConverter typeConverter = beanFactory.getTypeConverter();
				// 遍历方法的每个参数
				for (int i = 0; i < arguments.length; i++) {
					MethodParameter methodParam = new MethodParameter(method, i);
					DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
					currDesc.setContainingClass(bean.getClass());
					descriptors[i] = currDesc;
					try {
						// 还是要调用这个方法
						Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
						if (arg == null && !this.required) {
							arguments = null;
							break;
						}
						arguments[i] = arg;
					}
					catch (BeansException ex) {
						throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
					}
				}
				synchronized (this) {
					if (!this.cached) {
						if (arguments != null) {
							Object[] cachedMethodArguments = new Object[paramTypes.length];
							System.arraycopy(descriptors, 0, cachedMethodArguments, 0, arguments.length);
							// 注册bean之间的依赖关系
							registerDependentBeans(beanName, autowiredBeans);
							// 跟字段注入差不多，存在@Value注解，不进行缓存
							if (autowiredBeans.size() == paramTypes.length) {
								Iterator<String> it = autowiredBeans.iterator();
								for (int i = 0; i < paramTypes.length; i++) {
									String autowiredBeanName = it.next();
									if (beanFactory.containsBean(autowiredBeanName) &&
											beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
										cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
												descriptors[i], autowiredBeanName, paramTypes[i]);
									}
								}
							}
							this.cachedMethodArguments = cachedMethodArguments;
						}
						else {
							this.cachedMethodArguments = null;
						}
						this.cached = true;
					}
				}
			}
			if (arguments != null) {
				try {
					// 反射调用方法
					// 像我们的setter方法就是在这里调用的
					ReflectionUtils.makeAccessible(method);
					method.invoke(bean, arguments);
				}
				catch (InvocationTargetException ex) {
					throw ex.getTargetException();
				}
			}
		}

		@Nullable
		private Object[] resolveCachedArguments(@Nullable String beanName) {
			Object[] cachedMethodArguments = this.cachedMethodArguments;
			if (cachedMethodArguments == null) {
				return null;
			}
			Object[] arguments = new Object[cachedMethodArguments.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = resolvedCachedArgument(beanName, cachedMethodArguments[i]);
			}
			return arguments;
		}
	}


	/**
	 * DependencyDescriptor variant with a pre-resolved target bean name.
	 */
	@SuppressWarnings("serial")
	private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

		private final String shortcut;

		private final Class<?> requiredType;

		public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
			super(original);
			this.shortcut = shortcut;
			this.requiredType = requiredType;
		}

		@Override
		public Object resolveShortcut(BeanFactory beanFactory) {
			return beanFactory.getBean(this.shortcut, this.requiredType);
		}
	}

}
