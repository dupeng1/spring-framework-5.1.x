1、
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
UserService userService = (UserService)context.getBean("userService");
userService.test();
————————————————
但它已经过时了，在新版Spring MVC和Spring Boot中，底层主要用的都是AnnotationConfigApplicationContext，比如：
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService userService = (UserService)context.getBean("userService");
userService.test();
————————————————
两者非常类似，只不过一个传入的是class，一个是xml文件，AppConfig.class基本等价于spring.xml
spring.xml中的内容为：
<context:component-scan base-package="com.yth"/>
AppConfig中的内容为：
@ComponentScan("com.yth")
public class AppConfig {
...
}
————————————————
目前，我们基本很少直接使用上面的方式来用Spring，而是使用Spring MVC，或者Spring Boot，
但它们底层都是基于上面这种方式需要在内部去创建一个ApplicationContext的，只不过：
Spring MVC创建的是XmlWebApplicationContext，和ClassPathXmlApplicationContext类似，都是基于XML配置的
Spring Boot创建的是AnnotationConfigApplicationContext
————————————————
Spring中是如何创建一个对象:
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService userService = (UserService)context.getBean("userService");
userService.test();
那getBean方法内部怎么知道"userService"对应的是UserService类呢？由此我们可以分析出来，
在调用AnnotationConfigApplicationContext的构造方法时，会去做一些事情：
-解析AppConfig.class，得到扫描路径
-遍历扫描路径下的所有Java类，如果发现某个类上存在@Component、@Service等注解，那么Spring就把这个类记录下来，存在一个Map中，比如Map<String,Class>。
Spring会根据某个规则生成当前类对应的beanName，作为key存入Map，当前类作为value
-这样，但调用context.getBean(“userService”)时，就可以根据"userService"找到UserService类，从而就可以去创建对象了
————————————————
Bean的创建过程
-利用该类的构造方法来实例化得到一个对象（但是如果一个类中有多个构造方法，Spring则会进行选择，这个叫做推断构造方法）
-得到一个对象后，Spring会判断该对象中是否存在被@Autowired注解了的属性，把这些属性找出来并由Spring进行赋值（依赖注入）
-依赖注入后，Spring会判断该对象是否实现了BeanNameAware接口、BeanClassLoaderAware接口、BeanFactoryAware接口，
如果实现了，就表示当前对象必须实现该接口中所定义的setBeanName()、setBeanClassLoader()、setBeanFactory()方法，
那Spring就会调用这些方法并传入相应的参数（Aware回调）
-Aware回调后，Spring会判断该对象中是否存在某个方法被@PostConstruct注解了，如果存在，Spring会调用当前对象的此方法（初始化前）
-紧接着，Spring会判断该对象是否实现了InitializingBean接口，如果实现了，就表示当前对象必须实现该接口中的afterPropertiesSet()方法，
那Spring就会调用当前对象中的afterPropertiesSet()方法（初始化）
-最后，Spring会判断当前对象需不需要进行AOP，如果不需要那么Bean就创建完了，如果需要进行AOP，
则会进行动态代理并生成一个代理对象做为Bean（初始化后）

通过最后一步，我们可以发现，当Spring根据UserService类来创建一个Bean时：
-如果不用进行AOP，那么Bean就是UserService类的构造方法所得到的对象。
-如果需要进行AOP，那么Bean就是UserService的代理类所实例化得到的对象，而不是UserService本身所得到的对象。

Bean对象创建出来后：
-如果当前Bean是单例Bean，那么会把该Bean对象存入一个Map<String,Object>，Map的key为beanName，value为Bean对象。这样下次getBean时就可以直接从Map中拿到对应的Bean对象了。（实际上，在Spring源码中，这个Map就是单例池）
-如果当前Bean是原型Bean，那么后续没有其他动作，不会存入一个Map，下次getBean时会再次执行上述创建过程，得到一个新的Bean对象。

推断构造方法
Spring在基于某个类生成Bean的过程中，需要利用该类的构造方法来实例化得到一个对象，但是如果一个类存在多个构造方法，Spring会使用哪个呢
如果一个类只存在一个构造方法，不管该构造方法是无参构造方法，还是有参构造方法，Spring都会用这个构造方法
如果一个类存在多个构造方法
a. 这些构造方法中，存在一个无参的构造方法，那么Spring就会用这个无参的构造方法
b. 这些构造方法中，不存在一个无参的构造方法，那么Spring就会报错
需要重视的是，如果Spring选择了一个有参的构造方法，Spring在调用这个有参构造方法时，需要传入参数，那这个参数是怎么来的呢？
Spring会根据入参的类型和入参的名字去Spring中找Bean对象（以单例Bean为例，Spring会从单例池那个Map中去找）：
先根据入参类型找，如果只找到一个，那就直接用来作为入参
如果根据类型找到多个，则再根据入参名字来确定唯一一个
最终如果没有找到，则会报错，无法创建当前Bean对象
————————————————
AOP大致流程
AOP就是进行动态代理，在创建一个Bean的过程中，Spring在最后一步会去判断当前正在创建的这个Bean是不是需要进行AOP，如果需要则会进行动态代理。
如何判断当前Bean对象需不需要进行AOP：

找出所有的切面Bean
遍历切面中的每个方法，看是否写了@Before、@After等注解
如果写了，则判断所对应的Pointcut是否和当前Bean对象的类是否匹配
如果匹配则表示当前Bean对象有匹配的的Pointcut，表示需要进行AOP
一方面会判断当前Bean是否需要进行AOP，同时会缓存当前Bean会用到的通知（切面的方法），代理对象生成和执行的时候直接用

利用cglib进行AOP的大致流程：

生成代理类UserServiceProxy，代理类继承UserService
代理类中重写了父类的方法，比如UserService中的test()方法
代理类中还会有一个target属性，该属性的值为被代理对象（也就是通过UserService类推断构造方法实例化出来的对象，进行了依赖注入、初始化等步骤的对象）
代理类中的test()方法被执行时的逻辑如下：
a. 执行切面逻辑（比如@Before，先不考虑环绕通知情况）
b. 调用target.test()
————————————————
Spring事务
当我们在某个方法上加了@Transactional注解后，就表示该方法在调用时会开启Spring事务，而这个方法所在类所对应的Bean对象会是该类的代理对象。
Spring事务的代理对象执行某个方法时的步骤：

判断当前执行的方法是否存在@Transactional注解
如果存在，则利用事务管理器（TransactionMananger）新建一个数据库连接
修改数据库连接的autocommit为false
执行target.test()，执行程序员所写的业务逻辑代码，也就是执行sql
执行完了之后如果没有出现异常，则提交，否则回滚
Spring事务是否会失效的判断标准：某个加了@Transactional注解的方法被调用时，要判断到底是不是直接被代理对象调用的，
如果是则事务会生效，如果不是则失效。
————————————————
https://qhao1024.blog.csdn.net/?type=blog





