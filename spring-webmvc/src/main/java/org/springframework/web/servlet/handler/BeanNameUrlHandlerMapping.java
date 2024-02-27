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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerMapping}
 * interface that map from URLs to beans with names that start with a slash ("/"),
 * similar to how Struts maps URLs to action names.
 *
 * <p>This is the default implementation used by the
 * {@link org.springframework.web.servlet.DispatcherServlet}, along with
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}.
 * Alternatively, {@link SimpleUrlHandlerMapping} allows for customizing a
 * handler mapping declaratively.
 *
 * <p>The mapping is from URL to bean name. Thus an incoming URL "/foo" would map
 * to a handler named "/foo", or to "/foo /foo2" in case of multiple mappings to
 * a single handler. Note: In XML definitions, you'll need to use an alias
 * name="/foo" in the bean definition, as the XML id may not contain slashes.
 *
 * <p>Supports direct matches (given "/test" -> registered "/test") and "*"
 * matches (given "/test" -> registered "/t*"). Note that the default is
 * to map within the current servlet mapping if applicable; see the
 * {@link #setAlwaysUseFullPath "alwaysUseFullPath"} property for details.
 * For details on the pattern options, see the
 * {@link org.springframework.util.AntPathMatcher} javadoc.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleUrlHandlerMapping
 */

/**
 * 1、继承 AbstractDetectingUrlHandlerMapping 抽象类，基于 Bean 的名字来自动探测的 HandlerMapping 实现类
 *
 * 2、使用实例：
 * <bean id="/hello.form" class="com.tiger.study.controller.HelloController"/>
 *
 * 3、如果有一个 Controller 或者 HttpRequestHandler 接口的实现类
 * 配置 BeanNameUrlHandlerMapping 类型的 HandlerMapping 对象，
 * 设置 Controller 实现类 的 beanName 为以 / 开头的名称就好了，
 * 它会探测到，将这个 Bean 的 beanName 作为 url，将 Controller 实现类 作为处理器
 */
public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

	/**
	 * Checks name and aliases of the given bean for URLs, starting with "/".
	 */
	//逻辑很简单，如果 Bean 的名称或者别名是以 / 开头，则会作为一个 url 返回，父类则会将该 Bean 作为一个处理器
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		List<String> urls = new ArrayList<>();
		// 如果是以 / 开头，添加到 urls
		if (beanName.startsWith("/")) {
			urls.add(beanName);
		}
		// 获得 beanName 的别名们，如果以 / 开头，则添加到 urls
		String[] aliases = obtainApplicationContext().getAliases(beanName);
		for (String alias : aliases) {
			if (alias.startsWith("/")) {
				urls.add(alias);
			}
		}
		return StringUtils.toStringArray(urls);
	}

}
