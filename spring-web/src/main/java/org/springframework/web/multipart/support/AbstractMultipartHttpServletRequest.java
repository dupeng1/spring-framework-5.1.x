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

package org.springframework.web.multipart.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * Abstract base implementation of the MultipartHttpServletRequest interface.
 * Provides management of pre-generated MultipartFile instances.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 06.10.2003
 */

/**
 * 1、继承了 HttpServletRequestWrapper 类，实现了 MultipartHttpServletRequest接口
 * 2、该类是 StandardMultipartHttpServletRequest 和 DefaultMultipartHttpServletRequest 的父类，实现了一些公共的方法
 * 3、MultipartRequest
 *   	MultipartHttpServletRequest
 *  		AbstractMultipartHttpServletRequest
 */
public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper
		implements MultipartHttpServletRequest {
	/**
	 * 请求中的文件信息，保存由子类解析出请求中的 Part 对象所封装成的 MultipartFile 对象
	 */
	@Nullable
	private MultiValueMap<String, MultipartFile> multipartFiles;


	/**
	 * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
	 * @param request the request to wrap
	 */
	protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public HttpServletRequest getRequest() {
		return (HttpServletRequest) super.getRequest();
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.resolve(getRequest().getMethod());
	}

	/** 获取请求头信息 */
	@Override
	public HttpHeaders getRequestHeaders() {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.put(headerName, Collections.list(getHeaders(headerName)));
		}
		return headers;
	}

	/** 获取文件名称列表 */
	@Override
	public Iterator<String> getFileNames() {
		return getMultipartFiles().keySet().iterator();
	}

	/** 获取指定文件名的单个文件 */
	@Override
	public MultipartFile getFile(String name) {
		return getMultipartFiles().getFirst(name);
	}

	/** 获取指定文件名的多个文件 */
	@Override
	public List<MultipartFile> getFiles(String name) {
		List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
		if (multipartFiles != null) {
			return multipartFiles;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, MultipartFile> getFileMap() {
		return getMultipartFiles().toSingleValueMap();
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return getMultipartFiles();
	}

	/**
	 * Determine whether the underlying multipart request has been resolved.
	 * @return {@code true} when eagerly initialized or lazily triggered,
	 * {@code false} in case of a lazy-resolution request that got aborted
	 * before any parameters or multipart files have been accessed
	 * @since 4.3.15
	 * @see #getMultipartFiles()
	 */
	public boolean isResolved() {
		return (this.multipartFiles != null);
	}


	/**
	 * Set a Map with parameter names as keys and list of MultipartFile objects as values.
	 * To be invoked by subclasses on initialization.
	 */
	protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
		this.multipartFiles =
				new LinkedMultiValueMap<>(Collections.unmodifiableMap(multipartFiles));
	}

	/**
	 * Obtain the MultipartFile Map for retrieval,
	 * lazily initializing it if necessary.
	 * @see #initializeMultipart()
	 */
	protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
		if (this.multipartFiles == null) {
			initializeMultipart();
		}
		return this.multipartFiles;
	}

	/**
	 * Lazily initialize the multipart request, if possible.
	 * Only called if not already eagerly initialized.
	 */
	protected void initializeMultipart() {
		throw new IllegalStateException("Multipart request not initialized");
	}

}
