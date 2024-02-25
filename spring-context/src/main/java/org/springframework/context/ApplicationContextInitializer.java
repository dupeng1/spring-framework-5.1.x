/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * Callback interface for initializing a Spring {@link ConfigurableApplicationContext}
 * prior to being {@linkplain ConfigurableApplicationContext#refresh() refreshed}.
 *
 * <p>Typically used within web applications that require some programmatic initialization
 * of the application context. For example, registering property sources or activating
 * profiles against the {@linkplain ConfigurableApplicationContext#getEnvironment()
 * context's environment}. See {@code ContextLoader} and {@code FrameworkServlet} support
 * for declaring a "contextInitializerClasses" context-param and init-param, respectively.
 *
 * <p>{@code ApplicationContextInitializer} processors are encouraged to detect
 * whether Spring's {@link org.springframework.core.Ordered Ordered} interface has been
 * implemented or if the @{@link org.springframework.core.annotation.Order Order}
 * annotation is present and to sort instances accordingly if so prior to invocation.
 *
 * @author Chris Beams
 * @since 3.1
 * @param <C> the application context type
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */
//

/**
 * 接口用于在 Spring 容器刷新之前执行的一个回调函数，通常用于向 SpringBoot 容器中注入属性
 *
 * 例如：
 * public class FirstInitializer implements ApplicationContextInitializer {
 *
 *     @Override
 *     public void initialize(ConfigurableApplicationContext applicationContext) {
 *         ConfigurableEnvironment environment = applicationContext.getEnvironment();
 *
 *         Map<String, Object> map = new HashMap<>();
 *         map.put("key1", "First");
 *
 *         MapPropertySource mapPropertySource = new MapPropertySource("firstInitializer", map);
 *         environment.getPropertySources().addLast(mapPropertySource);
 *
 *         System.out.println("run firstInitializer");
 *     }
 *
 * }
 *
 * 1、在 resources/META-INF/spring.factories 中配置
 * org.springframework.context.ApplicationContextInitializer=com.learn.springboot.initializer.FirstInitializer
 *
 * 2、在 mian 函数中添加
 * @SpringBootApplication
 * public class SpringbootApplication {
 *
 *     public static void main(String[] args) {
 * //        SpringApplication.run(SpringbootApplication.class, args);
 *         SpringApplication springApplication = new SpringApplication(SpringbootApplication.class);
 *         springApplication.addInitializers(new SecondInitializer());
 *         springApplication.run();
 *     }
 *
 * }
 *
 * 3、在配置文件中配置
 * context.initializer.classes=com.learn.springboot.initializer.ThirdInitializer
 * @param <C>
 */
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * Initialize the given application context.
	 * @param applicationContext the application to configure
	 */
	void initialize(C applicationContext);

}
