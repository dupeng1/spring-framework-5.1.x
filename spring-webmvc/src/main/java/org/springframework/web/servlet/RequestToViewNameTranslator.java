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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for translating an incoming
 * {@link javax.servlet.http.HttpServletRequest} into a
 * logical view name when no view name is explicitly supplied.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */

/**
 * 1、RequestToViewNameTranslator 组件，视图名称转换器，用于解析出请求的默认视图名
 * 2、就是说当 ModelAndView 对象不为 null，但是它的 View 对象为 null，
 * 则需要通过 RequestToViewNameTranslator 组件根据请求解析出一个默认的视图名称
 */
public interface RequestToViewNameTranslator {

	/**
	 * Translate the given {@link HttpServletRequest} into a view name.
	 * @param request the incoming {@link HttpServletRequest} providing
	 * the context from which a view name is to be resolved
	 * @return the view name, or {@code null} if no default found
	 * @throws Exception if view name translation fails
	 */
	/**
	 * 根据请求，获得其视图名
	 */
	@Nullable
	String getViewName(HttpServletRequest request) throws Exception;

}
