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

package org.springframework.web.method.annotation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Map} method arguments annotated with {@code @RequestHeader}.
 * For individual header values annotated with {@code @RequestHeader} see
 * {@link RequestHeaderMethodArgumentResolver} instead.
 *
 * <p>The created {@link Map} contains all request header name/value pairs.
 * The method parameter type may be a {@link MultiValueMap} to receive all
 * values for a header, not only the first one.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
/**
 * 请求头字段map解析器
 * 支持的参数类型：被@RequestParam注解的MultiValueMap或Map。
 * 参数值来源：来自request.getParameterMap()，如果参数类型是MultiValueMap，对应值是LinkedMultiValueMap；
 * 如果参数类型是Map，对应值是LinkedHashMap，此时某个参数有多个值，只取第一个。
 * 1、参数解析器 HandlerMethodArgumentResolver 的实现类，处理header的请求参数
 * 2、继承 AbstractNamedValueMethodArgumentResolver 抽象类
 * 3、解析用@RequestHeader注释的Map类型方法参数.
 * 4、创建的Map包含所有请求头名称/值对.
 * 5、方法参数类型可以是MultiValueMap，用于接收头的所有值，而不仅仅是第一个值.
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
	/**
	 * 方法参数检查.
	 * 方法参数由@RequestHeader注释.
	 * 方法参数类型必须是Map类型.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				Map.class.isAssignableFrom(parameter.getParameterType()));
	}
	/**
	 * 解析方法参数值.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		// 获取方法参数类型.
		Class<?> paramType = parameter.getParameterType();
		// 方法参数是MultiValueMap类型.
		if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<String, String> result;
			// 方法参数是HttpHeaders类型.
			if (HttpHeaders.class.isAssignableFrom(paramType)) {
				result = new HttpHeaders();
			}
			else {
				result = new LinkedMultiValueMap<>();
			}
			// 处理NativeWebRequest的所有头.
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
				String headerName = iterator.next();
				String[] headerValues = webRequest.getHeaderValues(headerName);
				if (headerValues != null) {
					for (String headerValue : headerValues) {
						result.add(headerName, headerValue);
					}
				}
			}
			return result;
		}
		// 方法参数是其他Map类型
		else {
			Map<String, String> result = new LinkedHashMap<>();
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
				String headerName = iterator.next();
				String headerValue = webRequest.getHeader(headerName);
				if (headerValue != null) {
					result.put(headerName, headerValue);
				}
			}
			return result;
		}
	}

}
