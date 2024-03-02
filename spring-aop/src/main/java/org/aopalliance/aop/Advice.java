/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.aop;

/**
 * Tag interface for Advice. Implementations can be any type
 * of advice, such as Interceptors.
 *
 * @author Rod Johnson
 * @version $Id: Advice.java,v 1.1 2004/03/19 17:02:16 johnsonr Exp $
 */

/**
 * 1、通知/增强
 *
 * Advice只是起到一个超类标记功能。Advice（通知）定义了AOP框架在某个Joinpoint（连接点）的通用处理逻辑。
 *
 * 通知是织入到目标类连接点上的一段程序代码
 *
 * Spring提供的通知接口都是带方位名的，如：BeforeAdvice、AfterReturningAdvice、ThrowsAdvice等
 *
 * 我们通过AOP将横切关注功能加到原有的业务逻辑上，这是对原有业务逻辑的一种增强，可以是前置、后置、返回后、抛出异常时等
 *
 * 其实Advice翻译成“增强”更合理，更能准确表达其本质
 *
 * 2、Advice有以下几种常见的类型
 * AspectJMethodBeforeAdvice：前置通知。(@Before标注的方法会被解析成该通知）在切面方法执行之前执行。
 * AspectJAfterReturningAdvice：后置通知。（@AfterReturning 标注的方法会被解析成该通知）在切面方法执行之后执行，如果有异常，则不执行。
 * AspectJAroundAdvice：环绕通知。（@Around标注的方法会被解析成该通知）在切面方法执行前后执行。
 * AspectJAfterAdvice：返回通知。（@After 标注的方法会被解析成该通知）不论是否异常都会执行。
 * AspectJAfterThrowingAdvice：异常通知，（@AfterThrowing标注的方法会被解析成该通知）在连接点抛出异常后执行。
 */
public interface Advice {

}
