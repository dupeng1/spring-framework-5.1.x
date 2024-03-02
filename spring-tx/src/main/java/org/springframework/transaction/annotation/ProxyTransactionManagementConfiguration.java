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

package org.springframework.transaction.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans
 * necessary to enable proxy-based annotation-driven transaction management.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 */

/**
 * 事务管理配置类，引入这个配置类的目的就是为了向Spring容器中注入Bean，该类注册Spring基础架构beans，这些bean是启用基于代理的注解驱动事务管理所必须的，
 *  1、事务注解@Transactional的解析器（AnnotationTransactionAttributeSource），简称：@Transactional注解的解析类，类似于切点
 *  2、事务方法拦截器（TransactionInterceptor），简称：事务方法的拦截器，类似于切面里的通知
 *  3、事务增强器(BeanFactoryTransactionAttributeSourceAdvisor)。简称：事务增强器(Advisor) 类似于切面
 * 当Bean在初始化的时候，通过注解解析器判断类或者方法上带有@Transactional注解，就对这个Bean生成代理对象，并添加拦截方法，用于提交和回滚
 */
@Configuration
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

	//3、定义事务增强器：重点是用来绑定"事务注解解析器"和"事务方法拦截器"
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		//依赖 @Transactional注解的解析类
		advisor.setTransactionAttributeSource(transactionAttributeSource());
		//依赖 事务方法拦截器
		advisor.setAdvice(transactionInterceptor());
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}

	//1、定义注解解析器：内部通过SpringTransactionAnnotationParser类来处理@Transaction注解
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}

	//2、事务方法拦截器TransactionInterceptor：实现了MethodInterceptor接口，主要用于拦截事务方法的执行
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor() {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource());
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
