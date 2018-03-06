package com.yy.tool.publisher.util;

import java.io.File;

/**
 * 复制文件夹时的过滤器。
 * 
 * @author Luowen
 */
public interface Filter {

	/**
	 * 过滤操作。
	 * 返回 true 表示要过滤。
	 * 
	 * @param filename
	 * @param ext
	 * @param file
	 * @return
	 */
	boolean doFilter(String filename, String ext, File file);
}