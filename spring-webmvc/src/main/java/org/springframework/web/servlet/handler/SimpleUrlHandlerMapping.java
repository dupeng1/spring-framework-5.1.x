/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerMapping}
 * interface to map from URLs to request handler beans. Supports both mapping to bean
 * instances and mapping to bean names; the latter is required for non-singleton handlers.
 *
 * <p>The "urlMap" property is suitable for populating the handler map with
 * bean references, e.g. via the map element in XML bean definitions.
 *
 * <p>Mappings to bean names can be set via the "mappings" property, in a form
 * accepted by the {@code java.util.Properties} class, like as follows:<br>
 * {@code
 * /welcome.html=ticketController
 * /show.html=ticketController
 * }<br>
 * The syntax is {@code PATH=HANDLER_BEAN_NAME}.
 * If the path doesn't begin with a slash, one is prepended.
 *
 * <p>Supports direct matches (given "/test" -> registered "/test") and "*"
 * pattern matches (given "/test" -> registered "/t*"). Note that the default
 * is to map within the current servlet mapping if applicable; see the
 * {@link #setAlwaysUseFullPath "alwaysUseFullPath"} property. For details on the
 * pattern options, see the {@link org.springframework.util.AntPathMatcher} javadoc.

 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setMappings
 * @see #setUrlMap
 * @see BeanNameUrlHandlerMapping
 */

/**
 * 1、继承 AbstractUrlHandlerMapping 抽象类，简单的就 URL 匹配的 HandlerMapping 实现类
 *
 * 2、使用实例：
 * <bean id="helloController" class="com.tiger.study.controller.HelloController"/>
 *
 * 定义请求处理映射 HandlerMapping
 * <bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
 *     <property name="mappings" ref="urlMappings" />
 * </bean>
 *
 * 定义请求映射表 map
 * <util:properties id="urlMappings">
 *     <prop key="/hello.form">helloController</prop>
 * </util:properties>
 *
 * 3、如果有一个 Controller 或者 HttpRequestHandler 接口的实现类
 * 	配置 SimpleUrlHandlerMapping 类型的 HandlerMapping 对象，
 * 	往它的 Map<String, Object> urlMap 中添加 url 与 Controller 实现类的映射就好了
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {
	/**
	 * 配置的 URL 与处理器的映射
	 *
	 * 最终，会调用 {@link #registerHandlers(Map)} 进行注册到 {@link AbstractUrlHandlerMapping#handlerMap} 中
	 */
	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * Map URL paths to handler bean names.
	 * This is the typical way of configuring this HandlerMapping.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
	 * details, see the {@link org.springframework.util.AntPathMatcher} javadoc.
	 * @param mappings properties with URLs as keys and bean names as values
	 * @see #setUrlMap
	 */
	//将 /hello.form 与 HelloController 设置到 urlMap，所以说处理器也可能是一个 Controller 接口
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * Set a Map with URL paths as keys and handler beans (or handler bean names)
	 * as values. Convenient for population with bean references.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
	 * details, see the {@link org.springframework.util.AntPathMatcher} javadoc.
	 * @param urlMap map with URLs as keys and beans as values
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * Allow Map access to the URL path mappings, with the option to add or
	 * override specific entries.
	 * <p>Useful for specifying entries directly, for example via "urlMap[myKey]".
	 * This is particularly useful for adding or overriding entries in child
	 * bean definitions.
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * Calls the {@link #registerHandlers} method in addition to the
	 * superclass's initialization.
	 */
	//用于初始化，将 urlMap 中的URL与处理器添加到父类的 handlerMap 中
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * Register all handlers specified in the URL map for the corresponding paths.
	 * @param urlMap a Map with URL paths as keys and handler beans or bean names as values
	 * @throws BeansException if a handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		}
		else {
			urlMap.forEach((url, handler) -> {
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			});
			if (logger.isDebugEnabled()) {
				List<String> patterns = new ArrayList<>();
				if (getRootHandler() != null) {
					patterns.add("/");
				}
				if (getDefaultHandler() != null) {
					patterns.add("/**");
				}
				patterns.addAll(getHandlerMap().keySet());
				logger.debug("Patterns " + patterns + " in " + formatMappingName());
			}
		}
	}

}
