/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;

/**
 * Handles return values of types {@code void} and {@code String} interpreting them
 * as view name reference. As of 4.2, it also handles general {@code CharSequence}
 * types, e.g. {@code StringBuilder} or Groovy's {@code GString}, as view names.
 *
 * <p>A {@code null} return value, either due to a {@code void} return type or
 * as the actual return value is left as-is allowing the configured
 * {@link RequestToViewNameTranslator} to select a view name by convention.
 *
 * <p>A String return value can be interpreted in more than one ways depending on
 * the presence of annotations like {@code @ModelAttribute} or {@code @ResponseBody}.
 * Therefore this handler should be configured after the handlers that support these
 * annotations.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */

/**
 * 视图名称方法返回值解析器
 * 支持的返回值类型：void，String。
 * 如何处理返回值：如果返回值是null，不做任何处理；如果返回是字符串，把字符串当做视图名称；如果字符串中包含重定向字符，则设置ModelAndViewContainer的成员变量redirectModelScenario为true。
 *
 * 实现 HandlerMethodReturnValueHandler 接口，处理返回结果是视图名的 ReturnValueHandler 实现类
 * ViewNameMethodReturnValueHandler 适用于前后端未分离，Controller 返回视图名的场景，例如 JSP、Freemarker 等等。
 */
public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	/**
	 * 重定向的表达式的数组，用于判断某个视图是否为重定向的视图，一般情况下，不进行设置。
	 */
	@Nullable
	private String[] redirectPatterns;


	/**
	 * Configure one more simple patterns (as described in {@link PatternMatchUtils#simpleMatch})
	 * to use in order to recognize custom redirect prefixes in addition to "redirect:".
	 * <p>Note that simply configuring this property will not make a custom redirect prefix work.
	 * There must be a custom View that recognizes the prefix as well.
	 * @since 4.1
	 */
	public void setRedirectPatterns(@Nullable String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * The configured redirect patterns, if any.
	 */
	@Nullable
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}

	//判断是否支持处理该返回类型
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		//该方法的返回类型是否为void或者字符串
		return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
		// 如果是 String 类型
		if (returnValue instanceof CharSequence) {
			// 设置视图名到 mavContainer 中
			String viewName = returnValue.toString();
			mavContainer.setViewName(viewName);
			// 如果是重定向，则标记到 mavContainer 中
			if (isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		// 如果是非 String 类型，而且非 void ，则抛出 UnsupportedOperationException 异常
		else if (returnValue != null) {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

	/**
	 * Whether the given view name is a redirect view reference.
	 * The default implementation checks the configured redirect patterns and
	 * also if the view name starts with the "redirect:" prefix.
	 * @param viewName the view name to check, never {@code null}
	 * @return "true" if the given view name is recognized as a redirect view
	 * reference; "false" otherwise.
	 */
	protected boolean isRedirectViewName(String viewName) {
		// 符合 redirectPatterns 表达式，或者以 redirect: 开头
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
