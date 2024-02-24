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

package org.springframework.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@link BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */

/**
 * 1、父类是BeanDefinitionRegistryPostProcessor（bean定义后置处理器）
 * 2、BeanDefinitionRegistryPostProcessor父类是BeanFactoryPostProcessor（bean工厂后置处理器）
 * 3、根本作用就是解析注解给bean容器添加bean定义，@Configuration，@Import，@ComponentScan，@Bean等注解
 * 4、postProcessBeanFactory()：BeanFactoryPostProcessor接口方法
 * 5、postProcessBeanDefinitionRegistry()：BeanDefinitionRegistryPostProcessor接口方法
 *
 * 1、这个类实现了BeanFactoryPostProcessor接口，用来对BeanFactory进行扩展用的，
 * 在AnnotationConfigApplicationContext的refresh()方法中，会调用invokeBeanFactoryPostProcessors()方法调用所有的BeanFactoryPostProcessor扩展，
 * 其中就包含这里的ConfigurationClassPostProcessor
 * 2、它会完成对@ComponentScan路径下Bean的扫描和注册过程，也就是说，我们自定义的Bean将由这个类完成扫描并注册到BeanFactory中
 * 3、用于引导处理@Configuration配置类的后置处理器。注意：它只是引导处理，并不是实际处理
 * 4、https://blog.csdn.net/qq_35512802/article/details/132008256
 * 5、https://zhuanlan.zhihu.com/p/619081906
 * */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	@Nullable
	private Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<>();

	@Nullable
	private ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* Using short class names as default bean names */
	private BeanNameGenerator componentScanBeanNameGenerator = new AnnotationBeanNameGenerator();

	/* Using fully qualified class names as default bean names */
	private BeanNameGenerator importBeanNameGenerator = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			String beanClassName = definition.getBeanClassName();
			Assert.state(beanClassName != null, "No bean class name set");
			return beanClassName;
		}
	};


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as
	 * a standalone bean definition in XML, e.g. not using the dedicated
	 * {@code AnnotationConfig*} application contexts or the {@code
	 * <context:annotation-config>} element. Any bean name generator specified against
	 * the application context will take precedence over any value set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}


	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	//添加bean定义
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);

		processConfigBeanDefinitions(registry);
	}

	/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	//方法通过cglib代理配置类，来拦截 @Bean修饰的方法
	//目的是为了在配置类中多次调用@Bean方法返回的是同一个结果
	//避免了单例模式下的多例创建

	//对配置类进行增强
	//创建ImportAwareBeanPostProcessor 来支持ImportAware接口

	//准备配置类，以便在运行时为Bean请求提供服务，方法是将它们替换为CGLIB增强的子类
	//完成对@Bean方法的代理 (目的是为了在配置类中多次调用@Bean方法返回的是同一个结果)
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			// 这里还是解析配置类，只有还没有解析的时候才会调用下面的
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}
		// 增强配置类（核心方法）
		enhanceConfigurationClasses(beanFactory);
		// 添加实例化后置处理器
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 */
	//主要处理、解析配置类，根据解析判断不是配置类就直接返回，不再做后续相关注解解析
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		//1.初始化BeanDefinitionHolder集合
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		//2.所有已经注册的bean
		String[] candidateNames = registry.getBeanDefinitionNames();
		//3.遍历已注册的bean数组
		for (String beanName : candidateNames) {
			// 获取BeanDefinition
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			//判断：full configuration 和 lite configuration，配置类已经被处理了
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
					ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			//带有@Configuration注解的bean，beanDef的属性设置为full，beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, "full");
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				//带有@Configuration注解的bean添加到集合中
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		// 如果未找到@Configuration类，配置类集合为空，直接返回，不再做后续的相关注解解析
		if (configCandidates.isEmpty()) {
			return;
		}

		// Sort by previously determined @Order value, if applicable
		// 对configCandidates 进行 排序,按照@Order 配置的值进行排序
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		// 检测是否有自定义bean名称生成器
		// 判断如果是registry是SingletonBeanRegistry类型，则从中获取 beanName 生成器(BeanNameGenerator )
		// 实际上这里是 register 类型是 DefaultListableBeanFactory。是SingletonBeanRegistry的子类
		SingletonBeanRegistry sbr = null;
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) {
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
				if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}
		// 如果环境变量为空则指定一个标准环境
		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		// Parse each @Configuration class
		//初始化一个ConfigurationClassParser解析器，可以解析@Congiguration配置类
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
		// 用来保存尚未解析的配置类
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		// 用来保存已经解析的配置类
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
		//循环解析，直到candidates为空
		do {
			// 核心：解析配置类，【根据bean定义生成一个ConfigurationClass对象传入processConfigurationClass方法】
			//ConfigurationClassParser
			parser.parse(candidates);
			//主要校验配置类不能使用final修饰符（CGLIB代理是生成一个子类，因此原先的类不能使用final修饰）
			// 这里的校验规则是如果是被@Configuration修饰且proxyBeanMethods属性为true,则类不能为final
			// 如果@Bean修饰的方法，则必须是可覆盖的
			// 因为@Configuration(proxyBeanMethods = true) 是需要cglib代理的，所以不能为终态， @Bean所修饰的方法也有一套约束规则
			// 是否需要代理是根据 类或方法上的 @Scope 注解指定的，默认都是不代理
			parser.validate();
			// 保存这次解析出的配置类。此时这些ConfigurationClass 中保存了解析出来的各种属性值，等待最后构建 BeanDefinition
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			// 去除已经解析过的配置类
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			// 构造一个bean定义读取器
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
			//2.加载bean定义信息，主要实现将@Configuration @Import @ImportResource @ImportRegistrar注册为bean
			//ConfigurationClassBeanDefinitionReader
			this.reader.loadBeanDefinitions(configClasses);
			//将configClasses加入到已解析alreadyParsed中
			alreadyParsed.addAll(configClasses);
			// 清空候选配置类定义列表，如果没有新增bean，则会跳出do while循环
			candidates.clear();
			//再次获取容器中bean定义数量  如果大于 之前获取的bean定义数量，则说明有新的bean注册到容器中，需要再次解析
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				//容器中新的所有已注册的bean（包括老的）
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				//容器中老的已注册的bean（已经解析了）
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				//用来存储已经解析的类
				Set<String> alreadyParsedClasses = new HashSet<>();
				//循环遍历把已解析的类放到alreadyParsedClasses中
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				//循环遍历新的所有已注册的bean
				for (String candidateName : newCandidateNames) {
					// 排除老的已解析的
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						// 过滤是否是配置类带有@Configuration，并且没有解析过
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				// 替换一下,继续遍历解析
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		// 到这里已经把配置类解析完毕了。
		// 将ImportRegistry  注册为 bean，以支持ImportAware @Configuration 类
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}
		// 清除缓存
		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	//Spring会对Full Configuration （即被@Configuration修饰的配置类）进行代理，拦截@Bean方法，以确保正确处理@Bean语义
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			// 获取 CONFIGURATION_CLASS_ATTRIBUTE属性，如果不为null，则是配置类，在上篇中有过交代(可能是full或者lite类型)
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.info("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				// 保存下来，准备代理
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		// 如果没有找到 full 配置类，则说明不需要代理增强，则直接返回。
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}
		// 创建增强对象
		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		// 进行配置类增强。这里的增强实际上是通过cglib对配置类进行了代理。
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			// 设置 ：如果配置类被代理，则该 bean也需要一直代理
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			try {
				// Set enhanced subclass of the user-specified bean class
				// 获取bean的 Class 类
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				if (configClass != null) {
					// 生成代理类
					Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
					if (configClass != enhancedClass) {
						if (logger.isTraceEnabled()) {
							logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
									"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
						}
						//将BeanClass设置为增强后的类
						beanDef.setBeanClass(enhancedClass);
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}


	private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessProperties method attempts to autowire other configuration beans.
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ImportAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}

}
