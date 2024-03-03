/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Superinterface for all Advisors that are driven by a pointcut.
 * This covers nearly all advisors except introduction advisors,
 * for which method-level matching doesn't apply.
 *
 * @author Rod Johnson
 */

/**
 * 由Pointcut驱动的Advisor的基础接口；
 * 含盖了除引介Advisor外几乎所有的Advisor；
 *
 * 代表具有切点的切面，它包含Advice和Pointcut两个类，
 * 这样就可以通过类、方法名以及方法方位等信息灵活地定义切面的连接点，提供更具适用性的切面。其有6种实现类：
 *
 * DefaultPointcutAdvisor：最常用的切面类型，它可以通过任意Pointcut和Advice定义一个切面，唯一不支持的是引介的切面类型，一般可以通过扩展该类实现自定义的切面；
 * NameMatchMethodPointcutAdvisor：通过该类可以定义，按方法名定义切点的切面；
 * RegexpMethodPointcutAdvisor：按正则表达式匹配方法名进行切点定义的切面；
 * StaticMethodMatcherPointcutAdvisor：静态方法匹配器切点定义的切面；
 * AspecJExpressionPointcutAdvisor：Aspecj切点表达式定义切点的切面；
 * AspecJPointcutAdvisor：使用AspecJ语法定义切点的切面。
 *
 * PointcutAdvisor的切点是方法级别的，有Pointcut，具有getClassFilter、getMethodMatcher方法
 * IntroductionAdvisor切点是类级别的，具有getClassFilter
 */
public interface PointcutAdvisor extends Advisor {

	/**
	 * Get the Pointcut that drives this advisor.
	 */
	// 获取Pointcut
	Pointcut getPointcut();

}
