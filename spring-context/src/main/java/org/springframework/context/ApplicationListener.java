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

package org.springframework.context;

import java.util.EventListener;

/**
 * Interface to be implemented by application event listeners.
 * Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
 *
 * <p>As of Spring 3.0, an ApplicationListener can generically declare the event type
 * that it is interested in. When registered with a Spring ApplicationContext, events
 * will be filtered accordingly, with the listener getting invoked for matching event
 * objects only.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @param <E> the specific ApplicationEvent subclass to listen to
 * @see org.springframework.context.event.ApplicationEventMulticaster
 */

/**
 * spring提供的事件订阅者必须实现的接口，实现该接口的同时会指定一个泛型，当发布的事件类型与实现ApplicationListener接口的bean的泛型一致时，
 * 会回调这个bean的监听方法，并且把事件传递过去。
 * @Component
 * public class SysApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
 *     @Override
 *     public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
 *         // 这里可以使用传递过来的事件参数, 来获得ApplicationContext
 *         // 因此可以考虑在此处开启一些任务。比如缓存初始化, 读取定时任务, 开启线程任务等。
 *         // 因为这个时候, Spring容器已经初始化好了, 并且还能获取到ApplicationContext。
 *         ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();
 * 		System.out.println("==============Spring容器刷新完毕, 应用启动成功!!!==============");
 *     }
 * }
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);

}
