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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * A {@link HandlerExceptionResolver} that delegates to a list of other
 * {@link HandlerExceptionResolver HandlerExceptionResolvers}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */

/**
 * 1、实现 HandlerExceptionResolver、Ordered 接口，复合的 HandlerExceptionResolver 实现类
 *
 * 2、 包含三个实现类
 * org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
 *
 * org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
 *
 * org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
public class HandlerExceptionResolverComposite implements HandlerExceptionResolver, Ordered {
	/**
	 * 异常解析器数组
	 */
	@Nullable
	private List<HandlerExceptionResolver> resolvers;
	/**
	 * 优先级，默认最低
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Set the list of exception resolvers to delegate to.
	 */
	public void setExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.resolvers = exceptionResolvers;
	}

	/**
	 * Return the list of exception resolvers to delegate to.
	 */
	public List<HandlerExceptionResolver> getExceptionResolvers() {
		return (this.resolvers != null ? Collections.unmodifiableList(this.resolvers) : Collections.emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * Resolve the exception by iterating over the list of configured exception resolvers.
	 * <p>The first one to return a {@link ModelAndView} wins. Otherwise {@code null} is returned.
	 */
	@Override
	@Nullable
	public ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		if (this.resolvers != null) {
			//遍历 HandlerExceptionResolver 数组，逐个处理异常 ex，如果成功，则返回 ModelAndView 对象
			for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {
				ModelAndView mav = handlerExceptionResolver.resolveException(request, response, handler, ex);
				if (mav != null) {
					return mav;
				}
			}
		}
		return null;
	}

}
