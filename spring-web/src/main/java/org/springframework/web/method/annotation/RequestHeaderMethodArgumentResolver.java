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

package org.springframework.web.method.annotation;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Resolves method arguments annotated with {@code @RequestHeader} except for
 * {@link Map} arguments. See {@link RequestHeaderMapMethodArgumentResolver} for
 * details on {@link Map} arguments annotated with {@code @RequestHeader}.
 *
 * <p>An {@code @RequestHeader} is a named value resolved from a request header.
 * It has a required flag and a default value to fall back on when the request
 * header does not exist.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved
 * request header values that don't yet match the method parameter type.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
/**
 * 处理@RequestHeader注解的解析器
 * 支持的参数类型：参数被@RequestHeader注解了，并且被注解的参数类型不能是Map，通常被注解的参数类型应该是String或String[]。
 * 参数值来源：从request获取请求头字段值。跟@CookieValue一样，这个注解也可以配置是否必须和默认值。
 * 1、参数解析器 HandlerMethodArgumentResolver 的实现类，处理header的请求参数
 * 2、继承 AbstractNamedValueMethodArgumentResolver 抽象类
 * 3、解析用@RequestHeader注释的非Map类型方法参数.
 * 4、 @RequestHeader 用于从请求头解析的命名值.
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * Create a new {@link RequestHeaderMethodArgumentResolver} instance.
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to have expressions
	 */
	/**
	 * @param beanFactory 用于解析默认值中${…}占位符和#{…}SpEL表达式的Bean工厂，
	 * 						或如果默认值不应包含表达式，则返回null.
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * 检查方法参数.
	 * 方法参数由@RequestHeader注释.
	 * 方法参数类型不为Map类型.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				!Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
	}
	/**
	 * 创建NamedValueInfo.
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		return new RequestHeaderNamedValueInfo(ann);
	}
	/**
	 * 解析方法参数值.
	 */
	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		String[] headerValues = request.getHeaderValues(name);
		if (headerValues != null) {
			return (headerValues.length == 1 ? headerValues[0] : headerValues);
		}
		else {
			return null;
		}
	}
	/**
	 * 处理参数缺失异常.
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingRequestHeaderException(name, parameter);
	}

	/**
	 * RequestHeaderNamedValueInfo.
	 */
	private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
