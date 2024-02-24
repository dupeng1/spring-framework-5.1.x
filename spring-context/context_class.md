1、BeanDefinition表示Bean定义，BeanDefinition中存在很多属性用来描述一个Bean的特点
class，表示Bean类型
scope，表示Bean作用域，单例或原型等
lazyInit：表示Bean是否是懒加载
initMethodName：表示Bean初始化时要执行的方法
destroyMethodName：表示Bean销毁时要执行的方法
在Spring中，我们经常会通过以下几种方式来定义Bean：
xml中的<bean/>标签
@Bean注解
@Component（@Service，@Controller）注解

但这些，都属于申明式定义Bean。
我们还可以编程式定义Bean，那就是直接通过new BeanDefinition对象，比如：

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

// 生成一个BeanDefinition对象，并设置beanClass为User.class，并注册到ApplicationContext中
AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition().getBeanDefinition();
beanDefinition.setBeanClass(User.class);
context.registerBeanDefinition("user", beanDefinition);
System.out.println(context.getBean("user"));
————————————————
AnnotatedBeanDefinitionReader：
它可以直接把某个类转换为BeanDefinition（就算没有@Component注解），并且会解析该类上的注解，比如

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
// 构造中需要传入context
AnnotatedBeanDefinitionReader annotatedBeanDefinitionReader = new AnnotatedBeanDefinitionReader(context);
// 将User.class解析为BeanDefinition
annotatedBeanDefinitionReader.register(User.class);
System.out.println(context.getBean("user"));

它能解析的注解是：@Conditional、@Scope、@Lazy、@Primary、@DependsOn、@Role、@Description
————————————————
XmlBeanDefinitionReader：
可以解析xml文件中的<bean/>标签

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(context);
//返回的数量i，代表解析出几个BeanDefinition
int i = xmlBeanDefinitionReader.loadBeanDefinitions("spring.xml");
System.out.println(context.getBean("user"));

会解析注册xml中所有的BeanDefinition，但不会去扫描。
————————————————
ClassPathBeanDefinitionScanner：
ClassPathBeanDefinitionScanner是扫描器，但是它的作用和BeanDefinitionReader类似，它可以进行扫描，扫描某个包路径，
对扫描到的类进行解析，比如，扫描到的类上如果存在@Component注解，那么就会把这个类解析为一个BeanDefinition，比如：

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
context.refresh();
ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
// sacn代表扫描到的BeanDefinition个数
int scan = scanner.scan("com.yth");
System.out.println(scan);
System.out.println(context.getBean("orderService"));
————————————————
BeanFactory：
BeanFactory表示Bean工厂，所以很明显，BeanFactory会负责创建Bean，并且提供获取Bean的API。
在Spring的源码实现中，当我们new一个ApplicationContext时，其底层会new一个BeanFactory出来，
当使用ApplicationContext的某些方法时，比如getBean()，底层调用的是BeanFactory的getBean()方法。

AliasRegistry：支持别名功能，一个名字可以对应多个别名
BeanDefinitionRegistry：可以注册、保存、移除、获取某个BeanDefinition
BeanFactory：Bean工厂，可以根据某个bean的名字、或类型、或别名获取某个Bean对象
SingletonBeanRegistry：可以直接注册、获取某个单例Bean
SimpleAliasRegistry：它是一个类，实现了AliasRegistry接口中所定义的功能，支持别名功能
ListableBeanFactory：在BeanFactory的基础上，增加了其他功能，可以获取所有BeanDefinition的beanNames，可以根据某个类型获取对应的beanNames，可以根据某个类型获取 { 类型：对应的Bean } 的映射关系
HierarchicalBeanFactory：在BeanFactory的基础上，添加了获取父BeanFactory的功能
DefaultSingletonBeanRegistry：它是一个类，实现了SingletonBeanRegistry接口，拥有了直接注册、获取某个单例Bean的功能
ConfigurableBeanFactory：在HierarchicalBeanFactory和SingletonBeanRegistry的基础上，添加了设置父BeanFactory、类加载器（表示可以指定某个类加载器进行类的加载）、设置Spring EL表达式解析器（表示该BeanFactory可以解析EL表达式）、设置类型转化服务（表示该BeanFactory可以进行类型转化）、可以添加BeanPostProcessor（表示该BeanFactory支持Bean的后置处理器），可以合并BeanDefinition，可以销毁某个Bean等等功能
FactoryBeanRegistrySupport：支持了FactoryBean的功能
AutowireCapableBeanFactory：是直接继承了BeanFactory，在BeanFactory的基础上，支持在创建Bean的过程中能对Bean进行自动装配
AbstractBeanFactory：实现了ConfigurableBeanFactory接口，继承了FactoryBeanRegistrySupport，这个BeanFactory的功能已经很全面了，但是不能自动装配和获取beanNames
ConfigurableListableBeanFactory：继承了ListableBeanFactory、AutowireCapableBeanFactory、ConfigurableBeanFactory
AbstractAutowireCapableBeanFactory：继承了AbstractBeanFactory，实现了AutowireCapableBeanFactory，拥有了自动装配的功能
DefaultListableBeanFactory：继承了AbstractAutowireCapableBeanFactory，实现了ConfigurableListableBeanFactory接口和BeanDefinitionRegistry接口，所以DefaultListableBeanFactory的功能很强大
————————————————
ApplicationContext
上面有分析到，ApplicationContext是个接口，实际上也是一个BeanFactory，不过比BeanFactory更加强大，比如：

HierarchicalBeanFactory：拥有获取父BeanFactory的功能
ListableBeanFactory：拥有获取beanNames的功能
ResourcePatternResolver：资源加载器，可以一次性获取多个资源（文件资源等等）
EnvironmentCapable：可以获取运行时环境（没有设置运行时环境功能）
ApplicationEventPublisher：拥有广播事件的功能（没有添加事件监听器的功能）
MessageSource：拥有国际化功能
————————————————
ConfigurableApplicationContext：继承了ApplicationContext接口，
增加了，添加事件监听器、添加BeanFactoryPostProcessor、 设置Environment，获取ConfigurableListableBeanFactory等功能

比如ApplicationContext可以发布事件，但是不能设置事件监听器，这里接口隔离将“功能”和“配置”拆开了
————————————————
AbstractApplicationContext：实现了ConfigurableApplicationContext接口
————————————————
GenericApplicationContext：继承了AbstractApplicationContext，实现了BeanDefinitionRegistry接口，
拥有了所有ApplicationContext的功能， 并且可以注册BeanDefinition，注意这个类中有一个属性(DefaultListableBeanFactory beanFactory)
————————————————
AnnotationConfigRegistry：可以单独注册某个为类为BeanDefinition（可以处理该类上的@Configuration注解，已经可以处理@Bean注解），同时可以扫描
————————————————
AnnotationConfigApplicationContext：继承了GenericApplicationContext，实现了AnnotationConfigRegistry接口，拥有了以上所有的功能
————————————————
ClassPathXmlApplicationContext：也是继承了AbstractApplicationContext，但是相对于AnnotationConfigApplicationContext而言，
功能没有AnnotationConfigApplicationContext强大，比如不能注册BeanDefinition
————————————————
然后Spring容器中注册一个MessageSource

public MessageSource messageSource() {
ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
//basename就是xxx_cn.properties中的xxx
messageSource.setBasename("messages");
messageSource.setDefaultEncoding("UTF-8");
return messageSource;
}

有了这个Bean，你可以在你任意想要进行国际化的地方使用该MessageSource实例。
同时，因为ApplicationContext也拥有国际化的功能，所以可以直接这么用：
————————————————
资源加载 (ResourcePatternResolver)
ApplicationContext还拥有资源加载的功能，比如，可以直接利用ApplicationContext获取某个文件的内容：
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
Resource resource = context.getResource("file:///Users/yangtianhao/develop/workspace/frame-learning/spring/src/main/java/com/yth/AppConfig.java");
System.out.println(resource.contentLength());

AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

Resource resource = context.getResource("file:///Users/yangtianhao/develop/workspace/frame-learning/spring/src/main/java/com/yth/AppConfig.java");
System.out.println(resource.contentLength());
System.out.println(resource.getFilename());

Resource resource1 = context.getResource("https://www.baidu.com");
System.out.println(resource1.contentLength());
System.out.println(resource1.getURL());

Resource resource2 = context.getResource("classpath:spring.xml");
System.out.println(resource2.contentLength());
System.out.println(resource2.getURL());
————————————————
获取运行时环境 (EnvironmentCapable)：
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

//操作系统层面的环境变量
Map<String, Object> systemEnvironment = context.getEnvironment().getSystemEnvironment();
System.out.println(systemEnvironment);

System.out.println("=======");

//jvm层面的环境变量
Map<String, Object> systemProperties = context.getEnvironment().getSystemProperties();
System.out.println(systemProperties);

System.out.println("=======");

//获取所有properties文件资源
//	注意需要用@PropertySource注解导入properties文件
//	注意这里返回的内容也包含上面两种配置!
MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
System.out.println(propertySources);

System.out.println("=======");

//一般获取环境变量，用下面这种api方式就行了
//操作系统环境变量里的
System.out.println(context.getEnvironment().getProperty("NO_PROXY"));
//jvm环境变量里的
System.out.println(context.getEnvironment().getProperty("sun.jnu.encoding"));
//我们自己定义的properties文件里的
System.out.println(context.getEnvironment().getProperty("zhouyu"));

可以利用
@PropertySource("classpath:spring.properties")
来使得某个properties文件中的参数添加到运行时环境中
————————————————
事件发布 (ApplicationEventPublisher)：
先定义一个事件监听器
@Bean
public ApplicationListener applicationListener() {
return new ApplicationListener() {
@Override
public void onApplicationEvent(ApplicationEvent event) {
System.out.println("接收到了一个事件" + event);
}
};
}

然后发布一个事件：
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
context.publishEvent("kkk");
————————————————
PropertyEditor
这其实是JDK中提供的类型转化工具类
public class StringToUserPropertyEditor extends PropertyEditorSupport implements PropertyEditor {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
        //根据String转成自己想转的类型
		User user = new User();
		user.setName(text);
		this.setValue(user);
	}
}

StringToUserPropertyEditor propertyEditor = new StringToUserPropertyEditor();
propertyEditor.setAsText("1");
User value = (User) propertyEditor.getValue();
System.out.println(value);
————————————————
ConversionService
Spring中提供的类型转化服务，它比PropertyEditor更强大
待转换类型不是只能String
支持自定义适配场景
//条件通用转换器
public class StringToUserConverter implements ConditionalGenericConverter {

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        //支持自定义适配场景
        //sourceType待转换的类型
        //targetType目标类型
		return sourceType.getType().equals(String.class) && targetType.getType().equals(User.class);
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, User.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		User user = new User();
		user.setName((String)source);
		return user;
	}
}

DefaultConversionService conversionService = new DefaultConversionService();
conversionService.addConverter(new StringToUserConverter());
User value = conversionService.convert("1", User.class);
System.out.println(value);
————————————————
TypeConverter
整合了PropertyEditor和ConversionService的功能，是Spring内部用的（其实就是一个适配器）

SimpleTypeConverter typeConverter = new SimpleTypeConverter();
//既支持注册jdk的PropertyEditor
typeConverter.registerCustomEditor(User.class, new StringToUserPropertyEditor());
//也支持注册ConversionService
//typeConverter.setConversionService(conversionService);
User value = typeConverter.convertIfNecessary("1", User.class);
System.out.println(value);
————————————————
OrderComparator
OrderComparator是Spring所提供的一种比较器，可以用来根据@Order注解或实现Ordered接口来执行值进行比较，从而可以进行排序。
public class A implements Ordered {

	@Override
	public int getOrder() {
		return 3;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

public class B implements Ordered {

	@Override
	public int getOrder() {
		return 2;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
public class Main {

	public static void main(String[] args) {
		A a = new A(); // order=3
		B b = new B(); // order=2

		OrderComparator comparator = new OrderComparator();
		System.out.println(comparator.compare(a, b));  // 1

		List list = new ArrayList<>();
		list.add(a);
		list.add(b);

		// 按order值升序排序
		list.sort(comparator);

		System.out.println(list);  // B，A
	}
}

另外，Spring中还提供了一个OrderComparator的子类：AnnotationAwareOrderComparator，它支持用@Order来指定order值。比如：
@Order(3)
public class A {

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
@Order(2)
public class B {

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}

public class Main {

	public static void main(String[] args) {
		A a = new A(); // order=3
		B b = new B(); // order=2

		AnnotationAwareOrderComparator comparator = new AnnotationAwareOrderComparator();
		System.out.println(comparator.compare(a, b)); // 1

		List list = new ArrayList<>();
		list.add(a);
		list.add(b);

		// 按order值升序排序
		list.sort(comparator);

		System.out.println(list); // B，A
	}
}
Ordered接口比@Order注解优先级更高
————————————————
BeanPostProcessor
BeanPostProcess表示Bena的后置处理器，我们可以定义一个或多个BeanPostProcessor，比如通过以下代码定义一个BeanPostProcessor：
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("userService".equals(beanName)) {
			System.out.println("初始化前");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("userService".equals(beanName)) {
			System.out.println("初始化后");
		}
		return bean;
	}
}

一个BeanPostProcessor可以在任意一个Bean的初始化之前以及初始化之后去额外的做一些用户自定义的逻辑，当然，我们可以通过判断beanName来进行针对性处理（针对某个Bean，或某部分Bean）。
我们可以通过定义BeanPostProcessor来干涉Spring创建Bean的过程。
————————————————
BeanFactoryPostProcessor
BeanFactoryPostProcessor表示Bean工厂的后置处理器，其实和BeanPostProcessor类似，BeanPostProcessor是干涉Bean的创建过程，BeanFactoryPostProcessor是干涉BeanFactory的创建过程。比如，我们可以这样定义一个BeanFactoryPostProcessor：

@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("加工beanFactory");
	}
}
我们可以在postProcessBeanFactory()方法中对BeanFactory进行加工。
————————————————
FactoryBean
上面提到，我们可以通过BeanPostPorcessor来干涉Spring创建Bean的过程，但是如果我们想一个Bean完完全全由我们来创造，也是可以的，比如通过FactoryBean：
@Component
public class MyFactoryBean implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        return new OrderService();
    }

    @Override
    public Class<?> getObjectType() {
        return OrderService.class;
    }
}
Spring一开始扫描的时候，MyFactoryBean只会把它当作一个普通Bean，此时单例池中myFactoryBean对应的对象类型还是MyFactoryBean.class
但是在getBean方法中会去判断这个Bean是否是FactoryBean，如果是会调用getObject获取对象，然后放到另外一个Map
factoryBeanObjectCache就是专门存FactoryBean生成的对象的。
因此一个FactoryBean底层实际上是生成了两个对象，beanName前面加上&符号(个数随便)可以拿到FactoryBean本身
通过上面这段代码，我们自己创造了一个UserService对象，并且它将成为Bean。但是通过这种方式创造出来的UserService的Bean，
只会经过初始化后，其他Spring的生命周期步骤是不会经过的，比如依赖注入。
为什么还是需要初始化后呢？因为FactoryBean不能影响AOP功能！！AOP就是通过初始化后这一步实现的!!!
————————————————
ExcludeFilter和IncludeFilter
这两个Filter是Spring扫描过程中用来过滤的。ExcludeFilter表示排除过滤器，IncludeFilter表示包含过滤器。

比如以下配置，表示扫描com.yth这个包下面的所有类，但是排除UserService类，也就是就算它上面有@Component注解也不会成为Bean。

@ComponentScan(value = "com.yth",
excludeFilters = {@ComponentScan.Filter(
type = FilterType.ASSIGNABLE_TYPE,
classes = UserService.class)})
public class AppConfig {
}
再比如以下配置，就算UserService类上没有@Component注解，它也会被扫描成为一个Bean。

@ComponentScan(value = "com.yth",
includeFilters = {@ComponentScan.Filter(
type = FilterType.ASSIGNABLE_TYPE,
classes = UserService.class)})
public class AppConfig {
}
————————————————
MetadataReader、ClassMetadata、AnnotationMetadata
在Spring中需要去解析类的信息，比如类名、类中的方法、类上的注解，这些都可以称之为类的元数据，所以Spring中对类的元数据做了抽象，并提供了一些工具类。

MetadataReader表示类的元数据读取器，默认实现类为SimpleMetadataReader。比如：
public class Test {

	public static void main(String[] args) throws IOException {
		SimpleMetadataReaderFactory simpleMetadataReaderFactory = new SimpleMetadataReaderFactory();
		
        // 构造一个MetadataReader
        MetadataReader metadataReader = simpleMetadataReaderFactory.getMetadataReader("com.yth.service.UserService");
		
        // 得到一个ClassMetadata，并获取了类名
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
	
        System.out.println(classMetadata.getClassName());
        
        // 获取一个AnnotationMetadata，并获取类上的注解信息
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
		for (String annotationType : annotationMetadata.getAnnotationTypes()) {
			System.out.println(annotationType);
		}

	}
}
需要注意的是，SimpleMetadataReader去解析类时，使用的ASM技术。
为什么要使用ASM技术，Spring启动的时候需要去扫描，如果指定的包路径比较宽泛，那么扫描的类是非常多的，很多不需要用到的类也会被扫到，
那如果在Spring启动时就把这些类全部加载进JVM了，不太合适，所以使用了ASM技术。
ASM技术是直接解析class文件字节流的(class文件是有格式的)，不用把所有class文件解析成java class对象加载到jvm
————————————————








