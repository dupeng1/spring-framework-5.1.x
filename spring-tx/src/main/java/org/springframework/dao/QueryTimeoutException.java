/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.dao;

/**
 * Exception to be thrown on a query timeout. This could have different causes depending on
 * the database API in use but most likely thrown after the database interrupts or stops
 * the processing of a query before it has completed.
 *
 * <p>This exception can be thrown by user code trapping the native database exception or
 * by exception translation.
 *
 * @author Thomas Risberg
 * @since 3.1
 */

/**
 * 查询超时抛出的异常。根据使用的数据库API可能会有不同的原因引发该异常，最有可能的就是一个执行中的查询操作完成前被数据库中断或者中止了。
 */
@SuppressWarnings("serial")
public class QueryTimeoutException extends TransientDataAccessException {

	/**
	 * Constructor for QueryTimeoutException.
	 * @param msg the detail message
	 */
	public QueryTimeoutException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for QueryTimeoutException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public QueryTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
