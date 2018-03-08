package com.yy.tool.publisher.action;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.rt.log.Logger;
import com.rt.util.file.FileFilter;
import com.rt.util.file.FileUtil;
import com.rt.util.string.StringUtil;
import com.rt.util.zip.ZipUtil;
import com.yy.tool.publisher.Common;

/**
 * 打包操作。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Packer implements ActionInterface {

	private File packDir;
	private FileFilter filter;


	@Override
	public void todo() {

		Logger.log("开始打包");


		Date startDate = new Date();


		// 校验打包输出目录。
		packDir = new File(Common.getTempDir(), "pack");
		if (!packDir.exists()) {
			packDir.mkdirs();
		}
		
		
		// 打包文件过滤器。
		filter = new FileFilter() {
			public boolean doFilter(String filename, String ext, File file) {
				if (Common.getExcludeExts().indexOf(ext) != -1) {
					return true;
				}

				String filePath = file.getAbsolutePath();
				for (File f : Common.getExcludeFiles()) {
					String fPath = f.getAbsolutePath();
					
					if (f.isDirectory()) {
						if (filePath.startsWith(fPath)) {
							return true;
						}
					} else {
						if (filePath.equals(fPath)) {
							return true;
						}
					}
				}
				
				return false;
			}
		};


		// yiyuen 工程。
		File yiyuenRoot = new File(Common.getYiyuenRoot());
		if (yiyuenRoot.exists()) {
			packYiyuen();
		}

		// yiyuen-admin 工程。
		File yiyuenAdminRoot = new File(Common.getYiyuenAdminRoot());
		if (yiyuenAdminRoot.exists()) {
			packYiyuenAdmin();
		}
		
		
		// 打包。
		zip();


		// 清除临时目录。
		FileUtil.delete(packDir.getAbsoluteFile().toString());
		
		
		Date endDate = new Date();
		long cost = (endDate.getTime() - startDate.getTime()) / 1000;
		long minutes = cost / 60;
		long seconds = cost % 60;
		
		Logger.log("打包完成，共用时" +  + minutes + "分" + seconds + "秒");
	}
	
	
	/**
	 * 打包 yiyuen 工程。
	 */
	private void packYiyuen() {
		
		try {
			FileUtil.copyDir(Common.getYiyuenRoot(), packDir.getAbsolutePath(), filter);
		} catch (IOException e) {
			Logger.printStackTrace(e);
		}
	}


	/**
	 * 打包 yiyuen-admin 工程。
	 */
	private void packYiyuenAdmin() {
		
		try {
			FileUtil.copyDir(Common.getYiyuenAdminRoot(), packDir.getAbsolutePath(), filter);
		} catch (IOException e) {
			Logger.printStackTrace(e);
		}
	}
	
	
	/**
	 * 打包。
	 * 
	 * @throws IOException 
	 */
	private void zip() {

		String nextVersion = Common.getNextVersionStr();
		String dateTag = new SimpleDateFormat("YYYYMMdd").format(new Date());
		
		Map<String, Object> params = new HashMap<>();
		params.put("version", nextVersion);
		params.put("date", dateTag);
		
		String fullFilename = StringUtil.substitute(Common.getFullFilename(), params);
		File outFilename = new File(Common.getOutDir(), fullFilename);
		
		
		try {
			ZipUtil.compress(packDir.getAbsolutePath(), outFilename.getAbsolutePath());
		} catch (IOException e) {
			Logger.printStackTrace(e);
		}
	}
}