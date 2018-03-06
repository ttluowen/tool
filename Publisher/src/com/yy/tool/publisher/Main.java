package com.yy.tool.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.rt.log.Logger;
import com.rt.statuscode.StatuscodeMap;
import com.rt.util.http.HttpUtil;
import com.rt.util.proterty.PropertyUtil;
import com.rt.util.string.StringUtil;
import com.rt.web.config.SystemConfig;
import com.yy.tool.publisher.action.Comparer;
import com.yy.tool.publisher.action.Packer;
import com.yy.tool.publisher.action.Publisher;
import com.yy.tool.publisher.action.Uploader;
import com.yy.tool.publisher.util.FileUtil;


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
public class Main {

	/**
	 * 初始化配置信息。
	 * 
	 * @return
	 * @throws IOException 
	 */
	private void initConfig() throws IOException {

		// 获取当前系统运行环境的磁盘路径，并统一将 / 目录转换成 \ 方式。
		String path = System.getProperty("user.dir").replace("/", "\\");
		

		// 初始化系统目录。
		SystemConfig.setSystemPath(path);

		// 设置日志目录。
		Logger.setSystemPath(path);
		Logger.log("System path：" + path);


		// 读取配置文件内容。
		Properties properties = PropertyUtil.read(path + "\\config.properties");
		
		
		// 工程目录。
		Configs.setYiyuenRoot(SystemConfig.formatFilePath(properties.getProperty("root-yiyuen")));
		Configs.setYiyuenRoot(SystemConfig.formatFilePath(properties.getProperty("root-yiyuen-admin")));

		// 打包文件名格式。
		Configs.setYiyuenRoot("YiYuen-v{version}-{date}.rar");
		Configs.setYiyuenRoot("YiYuen-upgrade-v{version}-{date}.rar");

		// 排除文件类型。
		List<String> packExcludeExts = new ArrayList<>();
		for (String ext : StringUtil.unNull(properties.getProperty("pack-excludeExts")).split(",")) {
			ext = ext.trim().toLowerCase();
			if (!ext.isEmpty()) {
				packExcludeExts.add(ext);
			}
		}
		Configs.setExcludeExts(packExcludeExts);
		
		
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


		// 打包方式。
		String pack = StringUtil.unNull(properties.getProperty("pack")).toLowerCase();
		Configs.setPack(pack);
		

		// 读取要排除不打包的文件或文件夹。
		List<File> excludeFiles = new ArrayList<>();
		String[] projects = {"yiyuen", "yiyuen-admin"};
		String[] roots = {Configs.getYiyuenRoot(), Configs.getYiyuenAdminRoot()};
		
		for (int i = 0, l = projects.length; i < l; i++) {
			String project = projects[i];
			String root = roots[i];
			File excludeFilesFile = new File(path + "\\excludeFiles-" + project + ".txt");

			if (excludeFilesFile.exists()) {
				for (String line : FileUtil.read(excludeFilesFile, StringUtil.UTF8).split("\n")) {
					// 过滤前后空格。
					line = line.trim();

					// 过滤空行和注释行。
					if (!line.isEmpty() && line.indexOf("#") == -1) {
						excludeFiles.add(new File(root + line));
					}
				}
			}
		}
		Configs.setExcludeFiles(excludeFiles);


		// 获取下一个对应的版本号。
		String nextVersionApi;
		if (pack.equals("big")) {
			nextVersionApi = "queryNextBigVersion";
		} else if (pack.equals("small")) {
			nextVersionApi = "queryNextSmallVersion";
		} else {
			nextVersionApi = "queryNextPatchVersion";
		}
		StatuscodeMap versionSm = StatuscodeMap.parse(HttpUtil.get(Configs.DOMAIN + "api/version/" + nextVersionApi));
		String nextVersion = versionSm.getResultAsString();
		Configs.setNextVersion(SystemConfig.parseVersion(nextVersion));


		Logger.log("root-yiyuen：" + Configs.getYiyuenRoot());
		Logger.log("root-yiyuen-admin：" + Configs.getYiyuenAdminRoot());
		Logger.log("filename-full：", Configs.getFullFilename());
		Logger.log("filename-upgrade：", Configs.getUpgradeFilename());
		Logger.log("excludeExts：" + Configs.getExcludeExts());
		Logger.log("excludeFiles：" + Configs.getExcludeExts());
		Logger.log("actions：" + Configs.getActions());
	}


	/**
	 * 开始打包。
	 * 
	 * @throws IOException 
	 */
	public void start() throws IOException {

		// 初始化配置信息。
		initConfig();


		Logger.log("发布任务开始执行");


		// 创建相关目录。
		File outDir = Configs.getOutDir();
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		File tempDir = Configs.getTempDir();
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}


		Actions actions = Configs.getActions();


		try {
			// 打包操作。
			if (actions.packable()) {
				new Packer().todo();
			}
			
			// 增量包比较操作。
			if (actions.compareable()) {
				new Comparer().todo();;
			}
			
			// 上传至中心服务器。
			if (actions.uploadable()) {
				new Uploader();
			}
	
			// 使中心服务器发布版本当前版本。
			if (actions.publishable()) {
				new Publisher();
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 清除临时目录。
			FileUtil.delete(tempDir.getAbsolutePath());
		}


		Logger.log("发布任务执行结束");
	}


	/**
	 * 执行入口。
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		new Main().start();
	}
}
