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

package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * An {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}
 * that resolves cookie values from an {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */

/**
 * 基于servlet的cookievalue解析器
 * 支持的参数类型：参数被@CookieValue注解了。
 * 参数值来源：从request对象获取cookie值，如果被@CookieValue注解的参数类型是CookieValue，则直接把cookie对象赋值给参数；
 * 如果被@CookieValue注解的参数类型是字符串，则把cookie值赋值给参数。@CookieValue注解可以配置是否必须，默认值等。
 * 如果配置成了必须，但是从request获取不到cookie值，则返回默认值，如果未设置默认值，则spring抛出异常。
 */
public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	public ServletCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}


	@Override
	@Nullable
	protected Object resolveName(String cookieName, MethodParameter parameter,
			NativeWebRequest webRequest) throws Exception {

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");

		Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
		if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
			return cookieValue;
		}
		else if (cookieValue != null) {
			return this.urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
		}
		else {
			return null;
		}
	}

}
