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

package org.springframework.web.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Map} method arguments and handles {@link Map} return values.
 *
 * <p>A Map return value can be interpreted in more than one ways depending
 * on the presence of annotations like {@code @ModelAttribute} or
 * {@code @ResponseBody}. Therefore this handler should be configured after
 * the handlers that support these annotations.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */

/**
 * map对象解析器
 * 支持的参数类型：Map。
 * 参数值来源：spring会把ModelAndViewContainer持有的BindingAwareModelMap赋值给参数。
 *
 * Map返回值处理器
 * 支持的返回类型：Map接口的实现类。
 * 如何处理返回值：把Map中的键值对置入ModelAndViewContainer的Model。
 */
public class MapMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Map.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAndViewContainer is required for model exposure");
		return mavContainer.getModel();
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return Map.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue instanceof Map){
			mavContainer.addAllAttributes((Map) returnValue);
		}
		else if (returnValue != null) {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
