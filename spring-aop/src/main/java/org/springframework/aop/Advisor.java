/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a joinpoint)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by Spring users, but to
 * allow for commonality in support for different types of advice.</i>
 *
 * <p>Spring AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */

/**
 * 1、Aspect（切面）
 * 		切面是由Pointcut（切点）和Advice（通知）组成的，它包括了对横切关注功能的定义，也包括了对连接点的定义。
 * 2、Advisor(顾问/增强器)
 * 		Advisor是切面的另一种实现，绑定【切点】和【通知】。没有指定【切点】的【通知】是没有意义的，Advisor可以说就是一个绑定在指定【切点】上的【通知】。
 * 		它能够将通知以更为复杂的方式织入到目标对象中，是将通知包装为更复杂切面的装配器。
 *
 * 	3、封装了spring aop中的切点和通知。就是我们常用的@Aspect 注解标记的类。
 * 		切点（pointcut）包含了连接点的描述信息。
 * 		通知（advice）中包含了增强的横切代码，
 *
 * 	4、实现
 * 		StaticMethodMatcherPointcut：静态方法切面。定义了一个classFilter，通过重写getClassFilter()方法来指定切面规则。另外实现了StaticMethodMatcher接口，通过重写matches来指定方法匹配规则。
 * 		StaticMethodMatcherPointcutAdvisor：静态方法匹配切面顾问。扩展了切面排序方法。
 * 		NameMatchMethodPointcut：名称匹配切面。通过指定方法集合变量mappedNames，模糊匹配。
 * 		NameMatchMethodPointcutAdvisor：方法名称切面顾问。内部封装了NameMatchMethodPointcut，通过设置方法名称模糊匹配规则和通知来实现切面功能。
 * 		RegexpMethodPointcutAdvisor：正则表达式切面顾问。可设置多个正则表达式规则，通过内部封装的JdkRegexpMethodPointcut解析正则表达式。
 * 		DefaultPointcutAdvisor：默认切面顾问。比较灵活，可自由组合切面和通知。
 * 		InstantiationModelAwarePointcutAdvisorImpl：springboot自动装配的顾问类型。是最常用的一种顾问实现。在注解实现的切面中，所有@Aspect类，都会被解析成该对象。
 *
 * Advisor是继承体系主要有PointcutAdvisor和IntroductionAdvisor
 * 		PointcutAdvisor是方法级别，需要用到Pointcut和Advisor。注意Pointcut可以使用任何类型的Pointcut，Advice也可以任何类型的Advice
 * 		IntroductionAdvisor是类级别, 只能使用IntroductionAdvice类型的Advice。
 * */
public interface Advisor {

	/**
	 * Common placeholder for an empty {@code Advice} to be returned from
	 * {@link #getAdvice()} if no proper advice has been configured (yet).
	 * @since 5.0
	 */
	// 空通知
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * Return the advice part of this aspect. An advice may be an
	 * interceptor, a before advice, a throws advice, etc.
	 * @return the advice that should apply if the pointcut matches
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	// 获取通知，可以是拦截器、前置通知、抛出异常通知等
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * @return whether this advice is associated with a particular target instance
	 */
	// 是否和每个被通知的实例相关
	// 当前框架还未用到该方法，可以通过单例/多例，或者恰当的代理创建来确保Advisor的生命周期正确
	boolean isPerInstance();

}
