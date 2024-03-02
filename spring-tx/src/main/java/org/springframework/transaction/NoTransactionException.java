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

package org.springframework.transaction;

/**
 * Exception thrown when an operation is attempted that
 * relies on an existing transaction (such as setting
 * rollback status) and there is no existing transaction.
 * This represents an illegal usage of the transaction API.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */

/**
 * 当一个操作依赖于一个现有的事务（比如设置回滚的状态），但是不存在这个现有的事务时抛出的异常。该异常表示非法使用事务API的情况。
 */
@SuppressWarnings("serial")
public class NoTransactionException extends TransactionUsageException {

	/**
	 * Constructor for NoTransactionException.
	 * @param msg the detail message
	 */
	public NoTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NoTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public NoTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
