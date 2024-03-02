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

package org.springframework.aop.aspectj;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractGenericPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;

/**
 * Spring AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 2.0
 */

/**
 * AspectJ表达式切点（通过解析XML配置文件中的<aop:pointcut>元素生成的就是此类型的bean）。
 * 它是一种切点，但与一般的切点不同，一般的切点需要持有单独的ClassFilter和MethodMatcher。
 * 但是AspectJ表达式切点本身就兼具了这两个组件的功能。因为切点表达式，就是用来描述要代理的目标类和目标方法的。
 *
 * 用来处理对应 AspectJ 的 advice 和切点的，有advice的设置和获取、切点表达式的一些处理、设置切点的Bean工厂，获取该切点等方法。
 * 该类创建了一个 AspectJExpressionPointcut，它们之间的关系是一对一的组合关系。
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor implements BeanFactoryAware {

	private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();


	public void setExpression(@Nullable String expression) {
		this.pointcut.setExpression(expression);
	}

	@Nullable
	public String getExpression() {
		return this.pointcut.getExpression();
	}

	public void setLocation(@Nullable String location) {
		this.pointcut.setLocation(location);
	}

	@Nullable
	public String getLocation() {
		return this.pointcut.getLocation();
	}

	public void setParameterNames(String... names) {
		this.pointcut.setParameterNames(names);
	}

	public void setParameterTypes(Class<?>... types) {
		this.pointcut.setParameterTypes(types);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.pointcut.setBeanFactory(beanFactory);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
