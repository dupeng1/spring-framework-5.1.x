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

package org.springframework.web.multipart.commons;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * Servlet-based {@link MultipartResolver} implementation for
 * <a href="https://commons.apache.org/proper/commons-fileupload">Apache Commons FileUpload</a>
 * 1.2 or above.
 *
 * <p>Provides "maxUploadSize", "maxInMemorySize" and "defaultEncoding" settings as
 * bean properties (inherited from {@link CommonsFileUploadSupport}). See corresponding
 * ServletFileUpload / DiskFileItemFactory properties ("sizeMax", "sizeThreshold",
 * "headerEncoding") for details in terms of defaults and accepted values.
 *
 * <p>Saves temporary files to the servlet container's temporary directory.
 * Needs to be initialized <i>either</i> by an application context <i>or</i>
 * via the constructor that takes a ServletContext (for standalone usage).
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see #CommonsMultipartResolver(ServletContext)
 * @see #setResolveLazily
 * @see org.apache.commons.fileupload.servlet.ServletFileUpload
 * @see org.apache.commons.fileupload.disk.DiskFileItemFactory
 */

/**
 * 1、实现 MultipartResolver 接口，基于 Apache Commons FileUpload 的 MultipartResolver 实现类
 * 2、会将 HttpServletRequest 封装成 DefaultMultipartHttpServletRequest 对象，由 Apache 的 Commons FileUpload 组件来实现，
 * 通过 org.apache.commons.fileupload.servlet.ServletFileUpload 对象获取请求中的 org.apache.commons.fileupload.FileItem 对象，然后进行解析，文件会封装成 CommonsMultipartFile 对象 *
 * 3、如果需要使用这个 MultipartResolver 实现类，需要引入 commons-fileupload、commons-io 和 commons-codec 组件，例如：
 * <dependencies>
 *  <dependency>
 *  	<groupId>commons-fileupload</groupId>
 *  	<artifactId>commons-fileupload</artifactId>
 *  	<version>1.4</version>
 *  </dependency>
 *  <dependency>
 *  	<groupId>commons-io</groupId>
 *  	<artifactId>commons-io</artifactId>
 *  	<version>2.8.0</version>
 *  </dependency>
 *  <dependency>
 *  	<groupId>commons-codec</groupId>
 *  	<artifactId>commons-codec</artifactId>
 *  	<version>1.15</version>
 *  </dependency>
 *  </dependencies>
 *  3、如果 Spring Boot 项目中需要使用 CommonsMultipartResolver，需要在 application.yml 中添加如下配置，排除其默认的配置，如下：
 *  spring:
 *   	autoconfigure:
 *     		exclude: org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration
 *
 */
public class CommonsMultipartResolver extends CommonsFileUploadSupport
		implements MultipartResolver, ServletContextAware {
	/**
	 * 是否延迟解析
	 */
	private boolean resolveLazily = false;


	/**
	 * Constructor for use as bean. Determines the servlet container's
	 * temporary directory via the ServletContext passed in as through the
	 * ServletContextAware interface (typically by a WebApplicationContext).
	 * @see #setServletContext
	 * @see org.springframework.web.context.ServletContextAware
	 * @see org.springframework.web.context.WebApplicationContext
	 */
	public CommonsMultipartResolver() {
		super();
	}

	/**
	 * Constructor for standalone usage. Determines the servlet container's
	 * temporary directory via the given ServletContext.
	 * @param servletContext the ServletContext to use
	 */
	public CommonsMultipartResolver(ServletContext servletContext) {
		this();
		setServletContext(servletContext);
	}


	/**
	 * Set whether to resolve the multipart request lazily at the time of
	 * file or parameter access.
	 * <p>Default is "false", resolving the multipart elements immediately, throwing
	 * corresponding exceptions at the time of the {@link #resolveMultipart} call.
	 * Switch this to "true" for lazy multipart parsing, throwing parse exceptions
	 * once the application attempts to obtain multipart files or parameters.
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * Initialize the underlying {@code org.apache.commons.fileupload.servlet.ServletFileUpload}
	 * instance. Can be overridden to use a custom subclass, e.g. for testing purposes.
	 * @param fileItemFactory the Commons FileItemFactory to use
	 * @return the new ServletFileUpload instance
	 */
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new ServletFileUpload(fileItemFactory);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (!isUploadTempDirSpecified()) {
			getFileItemFactory().setRepository(WebUtils.getTempDir(servletContext));
		}
	}


	//判断是否为 multipart 请求，必须是 POST 请求，且 Content-Type 为 multipart/ 开头
	@Override
	public boolean isMultipart(HttpServletRequest request) {
		// 必须是 POST 请求，且 Content-Type 为 multipart/ 开头
		return ServletFileUpload.isMultipartContent(request);
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
		Assert.notNull(request, "Request must not be null");
		if (this.resolveLazily) {
			//将 HttpServletRequest 转换成 DefaultMultipartHttpServletRequest 对象
			//如果开启了延迟解析，则重写该对象的 initializeMultipart() 方法，用于解析请求
			return new DefaultMultipartHttpServletRequest(request) {
				@Override
				protected void initializeMultipart() {
					// 解析请求，获取文件、参数信息
					MultipartParsingResult parsingResult = parseRequest(request);
					setMultipartFiles(parsingResult.getMultipartFiles());
					setMultipartParameters(parsingResult.getMultipartParameters());
					setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
				}
			};
		}
		else {
			//直接调用 parseRequest(HttpServletRequest request) 方法解析请求，
			// 返回 MultipartParsingResult 对象，包含 MultipartFile 对象和普通参数信息
			// 解析请求，获取文件、参数信息
			MultipartParsingResult parsingResult = parseRequest(request);
			return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
		}
	}

	/**
	 * Parse the given servlet request, resolving its multipart elements.
	 * @param request the request to parse
	 * @return the parsing result
	 * @throws MultipartException if multipart resolution failed.
	 */
	//用于解析请求，返回 MultipartParsingResult 对象，包含 MultipartFile 对象、普通参数信息以及参数的 Content-Type 信息
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		// <1> 获取请求中的编码
		String encoding = determineEncoding(request);
		// <2> 获取 ServletFileUpload （ commons-fileupload 中的类）对象
		FileUpload fileUpload = prepareFileUpload(encoding);
		try {
			// <3> 获取请求中的流数据
			List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
			// <4> 将这些流数据转换成 MultipartParsingResult，包含 CommonsMultipartFile、参数信息、Content-type
			return parseFileItems(fileItems, encoding);
		}
		catch (FileUploadBase.SizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		}
		catch (FileUploadBase.FileSizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
		}
		catch (FileUploadException ex) {
			throw new MultipartException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * Determine the encoding for the given request.
	 * Can be overridden in subclasses.
	 * <p>The default implementation checks the request encoding,
	 * falling back to the default encoding specified for this resolver.
	 * @param request current HTTP request
	 * @return the encoding for the request (never {@code null})
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String encoding = request.getCharacterEncoding();
		if (encoding == null) {
			encoding = getDefaultEncoding();
		}
		return encoding;
	}

	//清理文件产生的临时资源
	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			try {
				cleanupFileItems(request.getMultiFileMap());
			}
			catch (Throwable ex) {
				logger.warn("Failed to perform multipart cleanup for servlet request", ex);
			}
		}
	}

}
