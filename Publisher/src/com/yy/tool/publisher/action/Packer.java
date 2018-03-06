package com.yy.tool.publisher.action;

import java.io.File;
import java.util.Date;

import com.rt.log.Logger;
import com.yy.tool.publisher.Configs;
import com.yy.tool.publisher.util.FileUtil;
import com.yy.tool.publisher.util.Filter;

/**
 * 打包操作。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Packer implements ActionInterface {

	private File tempDir;
	private Filter filter;


	@Override
	public void todo() {

		Logger.log("开始打包");


		Date startDate = new Date();
		
		
		// 打包文件过滤器。
		filter = new Filter() {
			public boolean doFilter(String filename, String ext, File file) {
				if (Configs.getExcludeExts().indexOf(ext) != -1) {
					return true;
				}

				String filePath = file.getAbsolutePath();
				for (File f : Configs.getExcludeFiles()) {
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
		File yiyuenRoot = new File(Configs.getYiyuenRoot());
		if (!yiyuenRoot.exists()) {
			packYiyuen();
		}

		// yiyuen-admin 工程。
		File yiyuenAdminRoot = new File(Configs.getYiyuenAdminRoot());
		if (!yiyuenAdminRoot.exists()) {
			packYiyuenAdmin();
		}


		// 清除临时目录。
		tempDir.delete();
		
		
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
		
		FileUtil.copyDirectiory(Configs.getYiyuenRoot(), tempDir.getAbsolutePath(), filter);
	}


	/**
	 * 打包 yiyuen-admin 工程。
	 */
	private void packYiyuenAdmin() {
		
		FileUtil.copyDirectiory(Configs.getYiyuenAdminRoot(), tempDir.getAbsolutePath(), filter);
	}
}
