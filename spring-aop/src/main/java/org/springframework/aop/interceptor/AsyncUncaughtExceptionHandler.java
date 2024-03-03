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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

/**
 * A strategy for handling uncaught exceptions thrown from asynchronous methods.
 *
 * <p>An asynchronous method usually returns a {@link java.util.concurrent.Future}
 * instance that gives access to the underlying exception. When the method does
 * not provide that return type, this handler can be used to managed such
 * uncaught exceptions.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */

/**
 * 异步任务执行抛出的异常catch不到时通过此接口进行处理。一个异步任务通常会返回一个java.util.concurrent.Future实例，以访问潜在的异常，当这个异步任务不提供这个返回值时，这个句柄就负责处理此类捕获不到的异常。
 */
@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {

	/**
	 * Handle the given uncaught exception thrown from an asynchronous method.
	 * @param ex the exception thrown from the asynchronous method
	 * @param method the asynchronous method
	 * @param params the parameters used to invoked the method
	 */
	void handleUncaughtException(Throwable ex, Method method, Object... params);

}
