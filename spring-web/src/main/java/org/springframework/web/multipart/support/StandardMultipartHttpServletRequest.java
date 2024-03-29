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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MultipartHttpServletRequest adapter, wrapping a Servlet 3.0 HttpServletRequest
 * and its Part objects. Parameters get exposed through the native request's getParameter
 * methods - without any custom processing on our side.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see StandardServletMultipartResolver
 */

/**
 * 1、继承 AbstractMultipartHttpServletRequest 抽象类，基于 Servlet 3.0 的 Multipart HttpServletRequest 实现类
 * 2、包含了一个 javax.servlet.http.HttpServletRequest 对象和它的 javax.servlet.http.Part 对象们
 * 3、其中 Part 对象会被封装成 StandardMultipartFile 对象
 * 4、MultipartRequest
 * 		MultipartHttpServletRequest
 * 			AbstractMultipartHttpServletRequest
 * 				StandardMultipartHttpServletRequest
 */
public class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {
	/**
	 * 普通参数名的集合，非上传文件的参数名
	 */
	@Nullable
	private Set<String> multipartParameterNames;


	/**
	 * Create a new StandardMultipartHttpServletRequest wrapper for the given request,
	 * immediately parsing the multipart content.
	 * @param request the servlet request to wrap
	 * @throws MultipartException if parsing failed
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
		this(request, false);
	}

	/**
	 * Create a new StandardMultipartHttpServletRequest wrapper for the given request.
	 * @param request the servlet request to wrap
	 * @param lazyParsing whether multipart parsing should be triggered lazily on
	 * first access of multipart files or parameters
	 * @throws MultipartException if an immediate parsing attempt failed
	 * @since 3.2.9
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request, boolean lazyParsing)
			throws MultipartException {

		super(request);
		// 如果不需要延迟解析
		if (!lazyParsing) {
			// 解析请求
			parseRequest(request);
		}
	}

	//解析请求，解析 HttpServletRequest 中的 Part 对象，如果是文件，则封装成 StandardMultipartFile 对象，否则就是普通参数，获取其名称
	private void parseRequest(HttpServletRequest request) {
		try {
			// <1> 从 HttpServletRequest 中获取 Part 们
			Collection<Part> parts = request.getParts();
			this.multipartParameterNames = new LinkedHashSet<>(parts.size());
			MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(parts.size());
			// <2> 遍历 parts 数组
			for (Part part : parts) {
				// <2.1> 获得请求头中的 Content-Disposition 信息，MIME 协议的扩展
				String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
				// <2.2> 对 Content-Disposition 信息进行解析，生成 ContentDisposition 对象
				// 包含请求参数信息，以面向“对象”的形式进行访问
				ContentDisposition disposition = ContentDisposition.parse(headerValue);
				// <2.3> 获得文件名
				String filename = disposition.getFilename();
				// <2.4> 情况一，文件名非空，说明是文件参数，则创建 StandardMultipartFile 对象
				if (filename != null) {
					if (filename.startsWith("=?") && filename.endsWith("?=")) {
						filename = MimeDelegate.decode(filename);
					}
					files.add(part.getName(), new StandardMultipartFile(part, filename));
				}
				// <2.5> 情况二，文件名为空，说明是普通参数，则保存参数名称
				else {
					this.multipartParameterNames.add(part.getName());
				}
			}
			// <3> 将上面生成的 StandardMultipartFile 文件对象们，设置到父类的 multipartFiles 属性中
			setMultipartFiles(files);
		}
		catch (Throwable ex) {
			handleParseFailure(ex);
		}
	}

	protected void handleParseFailure(Throwable ex) {
		String msg = ex.getMessage();
		if (msg != null && msg.contains("size") && msg.contains("exceed")) {
			throw new MaxUploadSizeExceededException(-1, ex);
		}
		throw new MultipartException("Failed to parse multipart servlet request", ex);
	}

	/** 初始化请求 */
	@Override
	protected void initializeMultipart() {
		parseRequest(getRequest());
	}

	/** 获取请求中的参数名称 */
	@Override
	public Enumeration<String> getParameterNames() {
		if (this.multipartParameterNames == null) {
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			return super.getParameterNames();
		}

		// Servlet 3.0 getParameterNames() not guaranteed to include multipart form items
		// (e.g. on WebLogic 12) -> need to merge them here to be on the safe side
		Set<String> paramNames = new LinkedHashSet<>();
		Enumeration<String> paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add(paramEnum.nextElement());
		}
		paramNames.addAll(this.multipartParameterNames);
		return Collections.enumeration(paramNames);
	}

	/** 获取请求中的参数，参数名和参数值的映射 */
	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.multipartParameterNames == null) {
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			return super.getParameterMap();
		}

		// Servlet 3.0 getParameterMap() not guaranteed to include multipart form items
		// (e.g. on WebLogic 12) -> need to merge them here to be on the safe side
		Map<String, String[]> paramMap = new LinkedHashMap<>(super.getParameterMap());
		for (String paramName : this.multipartParameterNames) {
			if (!paramMap.containsKey(paramName)) {
				paramMap.put(paramName, getParameterValues(paramName));
			}
		}
		return paramMap;
	}

	/** 获取请求的 Content-Type 内容类型 */
	@Override
	public String getMultipartContentType(String paramOrFileName) {
		try {
			Part part = getPart(paramOrFileName);
			return (part != null ? part.getContentType() : null);
		}
		catch (Throwable ex) {
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}

	/** 获取请求头信息 */
	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		try {
			Part part = getPart(paramOrFileName);
			if (part != null) {
				HttpHeaders headers = new HttpHeaders();
				for (String headerName : part.getHeaderNames()) {
					headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
				}
				return headers;
			}
			else {
				return null;
			}
		}
		catch (Throwable ex) {
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}


	/**
	 * Spring MultipartFile adapter, wrapping a Servlet 3.0 Part object.
	 */
	/**
	 * 这个类封装了 Servlet 3.0 的 Part 对象，也就是我们常用到的 MultipartFile 对象，支持对文件的操作，
	 * 内部其实都是调用 javax.servlet.http.Part 的方法
	 * 实现了 MultipartFile 接口和 Serializable 接口，内部封装了 javax.servlet.http.Part 对象和文件名称
	 */
	@SuppressWarnings("serial")
	private static class StandardMultipartFile implements MultipartFile, Serializable {

		private final Part part;

		private final String filename;

		public StandardMultipartFile(Part part, String filename) {
			this.part = part;
			this.filename = filename;
		}

		@Override
		public String getName() {
			return this.part.getName();
		}

		@Override
		public String getOriginalFilename() {
			return this.filename;
		}

		@Override
		public String getContentType() {
			return this.part.getContentType();
		}

		@Override
		public boolean isEmpty() {
			return (this.part.getSize() == 0);
		}

		@Override
		public long getSize() {
			return this.part.getSize();
		}

		@Override
		public byte[] getBytes() throws IOException {
			return FileCopyUtils.copyToByteArray(this.part.getInputStream());
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.part.getInputStream();
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			this.part.write(dest.getPath());
			if (dest.isAbsolute() && !dest.exists()) {
				// Servlet 3.0 Part.write is not guaranteed to support absolute file paths:
				// may translate the given path to a relative location within a temp dir
				// (e.g. on Jetty whereas Tomcat and Undertow detect absolute paths).
				// At least we offloaded the file from memory storage; it'll get deleted
				// from the temp dir eventually in any case. And for our user's purposes,
				// we can manually copy it to the requested location as a fallback.
				FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest.toPath()));
			}
		}

		@Override
		public void transferTo(Path dest) throws IOException, IllegalStateException {
			FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest));
		}
	}


	/**
	 * Inner class to avoid a hard dependency on the JavaMail API.
	 */
	private static class MimeDelegate {

		public static String decode(String value) {
			try {
				return MimeUtility.decodeText(value);
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
