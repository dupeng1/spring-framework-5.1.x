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

package org.springframework.http.converter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Strategy interface that specifies a converter that can convert from and to HTTP requests and responses.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> the converted object type
 */

/**
 *  1、消息转换器
 *  2、将返回结果设置到响应体中，当然也可能需要通过它从请求体获取入参
 *  3、@RequestBody 和 @ResponseBody 两个注解，分别完成请求报文到 Java 对象、Java 对象到响应报文的转换
 *  ，底层的实现就是通过 Spring 3.x 中引入的 HttpMessageConverter 消息转换机制来实现的
 *  4、获取到请求体和响应体，ServletInputStream 和 ServletOutputStream 分别继承 java 中的 InputStream 和 OutputStream 流对象，
 *  可以通过它们获取请求报文和设置响应报文
 *  	public ServletInputStream getInputStream() throws IOException;
 *  	public ServletOutputStream getOutputStream() throws IOException;
 * @param <T>
 */
public interface HttpMessageConverter<T> {

	/**
	 * Indicates whether the given class can be read by this converter.
	 * @param clazz the class to test for readability
	 * @param mediaType the media type to read (can be {@code null} if not specified);
	 * typically the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	/** 能否读取 */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * Indicates whether the given class can be written by this converter.
	 * @param clazz the class to test for writability
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	/** 能够写入 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * Return the list of {@link MediaType} objects supported by this converter.
	 * @return the list of supported media types
	 */
	/** 读取请求体 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Read an object of the given type from the given input message, and returns it.
	 * @param clazz the type of object to return. This type must have previously been passed to the
	 * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	/** 设置响应体 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * Write an given object to the given output message.
	 * @param t the object to write to the output message. The type of this object must have previously been
	 * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
	 * @param contentType the content type to use when writing. May be {@code null} to indicate that the
	 * default content type of the converter must be used. If not {@code null}, this media type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
	 * returned {@code true}.
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	//消息转换器将响应消息按照一定的规则写到响应报文中
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
