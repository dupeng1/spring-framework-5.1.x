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
 * Exception that gets thrown when an invalid isolation level is specified,
 * i.e. an isolation level that the transaction manager implementation
 * doesn't support.
 *
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
//当指定了一个非法的隔离级别时抛出的异常，比如，一个事务管理器不支持的隔离级别。

/**
 * （1） 未授权读取 Read Uncommitted：也称未提交读。防止更新丢失（对应一级锁），
 * 如果一个事务已经开始写数据则另外一个数据则不允许同时进行写操作但允许其他事务读此行数据。
 * 该隔离级别可以通过“排他写锁”实现。事务隔离的最低级别，仅可保证不读取物理损坏的数据。
 * 与READ COMMITTED 隔离级相反，它允许读取已经被其它用户修改但尚未提交确定的。
 *
 *  （2）授权读取 Read Committed：也称提交读。“未授权读取”之上防止脏读取（对应二级锁）。
 *  这可以通过“瞬间共享读锁”和“排他写锁”实现，读取数据的事务允许其他事务继续访问该行数据，但是未提交写事务将会禁止其他事务访问该行。
 *  SQL Server 默认的级别。在此隔离级下，SELECT 命令不会返回尚未提交（Committed） 的数据，也不能返回脏数据。
 *
 *   （3）可重复读取 Repeatable Read：“授权读取”之上防止不可重复读取（对应三级锁）。
 *   但是有时可能出现幻影数据，这可以通过“共享读锁”和“排他写锁”实现，读取数据事务将会禁止写事务（但允许读事务），
 *   写事务则禁止任何其他事务。在此隔离级下，用SELECT 命令读取的数据在整个命令执行过程中不会被更改。
 *   此选项会影响系统的效能，非必要情况最好不用此隔离级。三级封锁协议并不能阻止幻读，修改的不能再被读取，但是新增（删除）的记录数可以统计。
 *
 *   （4）串行 Serializable：也称可串行读（对应两段锁）。提供严格的事务隔离，它要求事务序列化执行，
 *   事务只能一个接着一个地执行，但不能并发执行。如果仅仅通过 “行级锁”是无法实现事务序列化的，
 *   必须通过其他机制保证新插入的数据不会被刚执行查询操作事务访问到。事务隔离的最高级别，事务之间完全隔离。
 *   如果事务在可串行读隔离级别上运行，则可以保证任何并发重叠事务均是串行的。
 */
@SuppressWarnings("serial")
public class InvalidIsolationLevelException extends TransactionUsageException {

	/**
	 * Constructor for InvalidIsolationLevelException.
	 * @param msg the detail message
	 */
	public InvalidIsolationLevelException(String msg) {
		super(msg);
	}

}
