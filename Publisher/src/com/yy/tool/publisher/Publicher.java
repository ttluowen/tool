package com.yy.tool.publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.rt.log.Logger;
import com.rt.util.proterty.PropertyUtil;
import com.rt.util.string.StringUtil;
import com.rt.web.config.SystemConfig;
import com.yy.tool.publisher.packer.FileUtil;
import com.yy.tool.publisher.packer.Filter;


/**
 * 将 yiyuen、yiyuen-admin、yiyuen-vue 等工程打成一个 rar 的工程包；
 * 同时再跟上一个版本比较生成一个增长升级包。；
 * 然后再将包上传到中心服务器，并更新最新版本号；
 * 上传成功后，中心服务器会通知各终端服务器自动下载升级；
 * 后面升级的事情将由 Upgrade 程序来负责。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Publicher {
	
	private Filter filter = new Filter() {
		public boolean doFilter(String filename, String ext, File file) {
			if (excludeExts.indexOf(ext) != -1) {
				return true;
			}
			
			String filePath = file.getAbsolutePath();
			for (File f : excludeFiles) {
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


	/**
	 * 开始打包。
	 * @throws IOException 
	 */
	public void start() throws IOException {

		// 初始化配置信息。
		initConfig();

		Logger.log("开始打包");
		Date startDate = new Date();


		// 清理目标文件夹。
		clearPackToDir();


		// 打包 MI 系统运行相关的文件。
		Logger.log("打包 MI 系统相关文件");
		packMIFile();
		
		
		// 打开系统配置管理目录。
		Logger.log("打包系统配置管理目录");
		packManage();


		// 打包 Tomcat。
		Logger.log("打包 Tomcat 服务");
		packTomcat();


		// 打包 MI Web 工程。
		Logger.log("打包 MI Web 工程");
		packMIProject();

		// 打包 Api 工程。
		Logger.log("打包 Api 工程");
		packApiProject();

		// 打包 MI 工程。
		Logger.log("打包 FineReport 工程");
		packFineReportProject();

		// 打包 Weixin 工程。
		Logger.log("打包 Weixin 工程");
		packWeixinProject();

//		// 打包 MesReport 工程。
//		Logger.log("打包 MesReport 工程");
//		packMesReportProject();


		Date endDate = new Date();
		long cost = (endDate.getTime() - startDate.getTime()) / 1000;
		long minutes = cost / 60;
		long seconds = cost % 60;
		Logger.log("打包完成，共用时" +  + minutes + "分" + seconds + "秒");
	}


	/**
	 * 初始化配置信息。
	 * 
	 * @return
	 */
	private void initConfig() {

		// 获取当前系统运行环境的磁盘路径，并统一将 / 目录转换成 \ 方式。
		String path = System.getProperty("user.dir").replace("/", "\\");
		

		// 初始化系统目录。
		SystemConfig.setSystemPath(path);

		// 设置日志目录。
		Logger.setSystemPath(path);
		Logger.log("系统目录：" + path);


		// 读取配置文件内容。
		Properties properties = PropertyUtil.read(path + "\\config.properties");
		
		
		// 工程目录。
		Configs.setYiyuenRoot(SystemConfig.formatFilePath(properties.getProperty("root-yiyuen")));
		Configs.setYiyuenRoot(SystemConfig.formatFilePath(properties.getProperty("root-yiyuen-admin")));

		// 打包文件名格式。
		Configs.setYiyuenRoot(properties.getProperty("filename-full"));
		Configs.setYiyuenRoot(properties.getProperty("filename-upgrade"));

		// 排除文件类型。
		List<String> packExcludeExts = new ArrayList<>();
		for (String ext : StringUtil.unNull(properties.getProperty("pack-excludeExts")).split(",")) {
			ext = ext.trim().toLowerCase();
			if (!ext.isEmpty()) {
				packExcludeExts.add(ext);
			}
		}
		Configs.setPackExcludeExts(packExcludeExts);
		
		
		// 设置执行动作。
		Actions actions = new Actions();
		for (String action : StringUtil.unNull(properties.getProperty("actions")).split(",")) {
			action = action.trim().toLowerCase();
			if (action.equals("pack")) {
				actions.setPack(true);
			} else if (action.equals("compare")) {
				actions.setCompare(true);
			} else if (action.equals("upload")) {
				actions.setUpload(true);
			} else if (action.equals("publish")) {
				actions.setPublish(true);
			}
		}
		Configs.setActions(actions);
		

		// 读取要排除不打包的文件或文件夹。
		List<String> excludeFiles = new ArrayList<>();
		String[] projects = {
				"yiyuen", "yiyuen-admin"
		};
		
		for (String project : projects) {
			
		}
		File excludeFilesFile = new File(path + "\\excludeFiles-yiyuen.txt");
		if (excludeFilesFile.exists()) {
			for (String line : FileUtil.read(excludeFilesFile, StringUtil.UTF8).split("\n")) {
				// 过滤前后空格。
				line = line.trim();

				// 过滤空行和注释行。
				if (!line.isEmpty() && line.indexOf("#") == -1) {
					excludeFiles.add(new File(SVNRoot + line));
				}
			}
		}


		Logger.log("SVN 目录为：" + SVNRoot);
		Logger.log("目标目录为：" + packTo);
		Logger.log("排除文件类型：" + excludeExts);
		Logger.log("排除文件名目录：" + excludeFiles);
	}
	
	
	/**
	 * 清空目标文件夹内容。
	 * @throws IOException 
	 */
	private void clearPackToDir() throws IOException {

		File packToDir = new File(packTo);

		// 检测目标文件夹是否存在，如果不存在则不需要后面的判断。
		if (!packToDir.exists()) {
			packToDir.mkdirs();

			return;
		}


		// 获取该目录层级下的所有文件列表。
		String[] files = packToDir.list();

		// 如果目标文件夹下没有文件，则也不需要操作。
		if (files.length == 0) {
			return;
		}
		

		// 读取当前文件夹下的文件列表，以输出显示。
		StringBuffer content = new StringBuffer();
		for (String file : files) {
			content.append(file).append("    ");
		}

		System.out.print("\"" + packTo + "\" 目录下已存在以下文件，是否要删除？\n" + content + "\n请输入y/n：");

		// 获取输入内容。
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line = reader.readLine().toLowerCase();
		reader.close();


		// 根据输入内容处理。
		if (line.equals("y")) {
			System.out.println("开始清理");

			FileUtil.delete(packTo);

			System.out.println("清理完成");
		} else if (!line.equals("n")) {
			System.out.println("输入错误");
		}
	}
	
	
	/**
	 * 打包 MI 系统运行相关的文件。
	 * 
	 * 启动.bat
	 * 帮助说明.txt
	 * bin 目录及文件
	 * apps 目录
	 */
	private void packMIFile() {

		// 单文件复制。
		String[] files = {
				"启动.bat",
				"安装.bat",
				"卸载.bat",
				"帮助说明.txt",
				"端口划分.txt"
		};
		for (String file : files) {
			FileUtil.copyFile(SVNRoot + "MI\\Deploy\\MI\\" + file, packTo + file);
		}


		// 单文件夹复制。
		String[] dirs = {
				"bin",
				"install",
				"soft"
		};
		for (String dir : dirs) {
			FileUtil.copyDirectiory(SVNRoot + "MI\\Deploy\\MI\\" + dir, packTo + dir, filter);
		}


		// 创建 upgrade 更新文件夹。
		new File(packTo + "upgrade").mkdirs();


		// 重新复制 bin 目录文件。
		String[] binFiles = {
				"sigar-amd64-winnt.dll",
				"sigar-x86-winnt.dll",
				"sigar-x86-winnt.lib"
		};
		for (String file: binFiles) {
			FileUtil.copyFile(SVNRoot + "MI\\Deploy\\MIBin\\lib\\" + file, packTo + "bin\\" + file);
		}
		// 复制 bin/MI.jar 文件。
		FileUtil.copyFile(SVNRoot + "MI\\Deploy\\MIBin\\versions\\1.1\\MI.jar", packTo + "bin\\MI.jar");
	}


	/**
	 * 打开系统配置管理目录。
	 */
	private void packManage() {

		FileUtil.copyDirectiory(SVNRoot + "MI\\Deploy\\MIBin\\manage", packTo + "manage", filter);
	}


	/**
	 * 打包 tomcat 服务目录。
	 */
	private void packTomcat() {

		FileUtil.copyDirectiory(SVNRoot + "MI\\Deploy\\MI\\tomcat", packTo + "tomcat", filter);


		/*
		 * 打包时部里将 tomcat 的配置设置成默认配置。
		 * 默认只保留 81(MIConfig) 工程，其它的都按需启用。
		 */
		File normalServerFile = new File(packTo + "tomcat\\conf\\server-normal.xml");
		if (normalServerFile.exists()) {
			File serverFile = new File(packTo + "tomcat\\conf\\server.xml");
			if (serverFile.exists()) {
				serverFile.delete();
			}

			normalServerFile.renameTo(serverFile);
		}
	}


	/**
	 * 打包 Portal 工程。
	 */
	private void packMIProject() {

		// 工程核心程序。
		FileUtil.copyDirectiory(SVNRoot + "MI\\Code\\Java\\src\\main\\webapp", packTo + "apps\\MI", filter);
		// 前端资源。
		FileUtil.copyDirectiory(SVNRoot + "MI\\Code\\Assets", packTo + "apps\\MI\\assets", filter);
		// 页面。
		FileUtil.copyDirectiory(SVNRoot + "MI\\Code\\Page", packTo + "apps\\MI\\page", filter);
	}


	/**
	 * 打包 Api 工程。
	 */
	private void packApiProject() {

		FileUtil.copyDirectiory(SVNRoot + "Api\\src\\main\\webapp", packTo + "apps\\Api", filter);
	}
	
	
	/**
	 * 打包 FineReport 工程。
	 */
	private void packFineReportProject() {

		FileUtil.copyDirectiory(SVNRoot + "RtReportPlatform\\FineReport", packTo + "apps\\FineReport", null);
	}
	
	
	/**
	 * 打包 Weixin 工程。
	 */
	private void packWeixinProject() {

		FileUtil.copyDirectiory(SVNRoot + "Weixin\\WebRoot", packTo + "apps\\Weixin", filter);
	}


	/**
	 * 打包 MesReport 工程。
	 */
	@SuppressWarnings("unused")
	private void packMesReportProject() {
	
		FileUtil.copyDirectiory(SVNRoot + "mes-report\\trunk\\src\\main\\webapp", packTo + "apps\\MesReport", null);
	}


	/**
	 * 执行入口。
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		new Publicher().start();
	}
}
