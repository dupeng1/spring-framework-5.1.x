/*
 * Copyright 2002-2007 the original author or authors.
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
 * Marker interface implemented by all AOP proxies. Used to detect
 * whether or not objects are Spring-generated proxies.
 *
 * @author Rob Harrop
 * @since 2.0.1
 * @see org.springframework.aop.support.AopUtils#isAopProxy(Object)
 */

/**
 * 标记接口，Spring使用JDK动态代理或者CGLIB的方式来生成代理对象，
 * 其中每个代理对象都会实现SpringProxy接口。用来判定是否是Spring产生的代理对象。
 */
public interface SpringProxy {

}
