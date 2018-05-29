package com.yy.tool.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.yy.log.Logger;
import com.yy.statuscode.StatuscodeMap;
import com.yy.tool.publisher.action.Comparer;
import com.yy.tool.publisher.action.Packer;
import com.yy.tool.publisher.action.Publisher;
import com.yy.tool.publisher.action.Uploader;
import com.yy.util.file.FileUtil;
import com.yy.util.http.HttpUtil;
import com.yy.util.number.NumberUtil;
import com.yy.util.proterty.PropertyUtil;
import com.yy.util.string.StringUtil;
import com.yy.web.config.SystemConfig;


/**
 * 将 yiyuen、yiyuen-admin、yiyuen-vue 等工程打成一个 rar 的工程包；
 * 同时再跟上一个版本比较生成一个增长升级包。；
 * 然后再将包上传到中心服务器，并更新最新版本号；
 * 上传成功后，中心服务器会通知各终端服务器自动下载升级；
 * 后面升级的事情将由 Upgrader 程序来负责。
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
		
		
		// 设置当前相关根目录。
		Common.setRoot(path);


		// 读取配置文件内容。
		Properties properties = PropertyUtil.read(path + "\\config.properties");


		// 工程目录。
		Common.setYiyuenRoot(new File(properties.getProperty("root-yiyuen")));
		Common.setYiyuenAdminRoot(new File(properties.getProperty("root-yiyuen-admin")));


		// 打包文件名格式。
		Common.setFullFilename("YiYuen-v{version}-{date}.zip");
		Common.setUpgradeFilename("YiYuen-v{version}-upgrade-{date}.zip");
		// 匹配打包名正则。
		Common.setFullFilenameReg("YiYuen\\-v(\\d+\\.\\d+\\.\\d+)\\-.*?\\.zip");
		Common.setUpgradeFilenameReg("YiYuen\\-v(\\d+\\.\\d+\\.\\d+)\\-upgrade\\-.*?\\.zip");


		// 排除文件类型。
		List<String> packExcludeExts = new ArrayList<>();
		for (String ext : StringUtil.unNull(properties.getProperty("excludedExts")).split(",")) {
			ext = ext.trim().toLowerCase();
			if (!ext.isEmpty()) {
				packExcludeExts.add(ext);
			}
		}
		Common.setExcludedExts(packExcludeExts);
		
		
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
		Common.setActions(actions);


		// 打包方式。
		String pack = StringUtil.unNull(properties.getProperty("pack")).toLowerCase();
		Common.setPack(pack);
		

		// 读取要排除不打包的文件或文件夹。
		List<File> excludedPackFiles = new ArrayList<>();
		String[] projects = {"yiyuen", "yiyuen-admin"};
		File[] roots = {Common.getYiyuenRoot(), Common.getYiyuenAdminRoot()};
		
		for (int i = 0, l = projects.length; i < l; i++) {
			String project = projects[i];
			File root = roots[i];
			File excludedFilesFile = new File(path, "excludedPackFiles-" + project + ".txt");

			if (excludedFilesFile.exists()) {
				for (String line : FileUtil.read(excludedFilesFile, StringUtil.UTF8).split("\n")) {
					// 过滤前后空格。
					line = line.trim();

					// 过滤空行和注释行。
					if (!line.isEmpty() && line.indexOf("#") == -1) {
						excludedPackFiles.add(new File(root, line));
					}
				}
			}
		}
		Common.setExcludedPackFiles(excludedPackFiles);


		// 读取要排除不比较的文件名文件夹。
		List<String> excludedCompareFiles = new ArrayList<>();
		File excludedCompareFile = new File(path, "excludedCompareFiles.txt");
		if (excludedCompareFile.exists()) {
			for (String line : FileUtil.read(excludedCompareFile, StringUtil.UTF8).split("\n")) {
				// 过滤前后空格。
				line = line.trim();

				// 过滤空行和注释行。
				if (!line.isEmpty() && line.indexOf("#") == -1) {
					excludedCompareFiles.add(line);
				}
			}
		}
		Common.setExcludedCompareFiles(excludedCompareFiles);


		// 获取下一个对应的版本号。
		String nextVersionApi;
		if (pack.equals("big")) {
			nextVersionApi = "queryNextBigVersion";
		} else if (pack.equals("small")) {
			nextVersionApi = "queryNextSmallVersion";
		} else {
			nextVersionApi = "queryNextPatch";
		}
		StatuscodeMap versionSm = StatuscodeMap.parse(HttpUtil.get(Common.DOMAIN + "api/base/version/" + nextVersionApi));
		String nextVersion = versionSm.getResultAsString();
		Common.setNextVersion(SystemConfig.parseVersion(nextVersion));


		// 升级包升级时的动作。
		Common.setUpgradeBeforeStop(properties.get("upgrade-beforeStop").equals("1"));
		Common.setUpgradeAfterRestart(properties.get("upgrade-afterRestart").equals("1"));


		// FTP 上传。
		Common.setFtpHost(StringUtil.unNull(properties.get("ftp-host")));
		Common.setFtpPort(NumberUtil.parseInt(properties.get("ftp-port")));
		Common.setFtpUsername(StringUtil.unNull(properties.get("ftp-username")));
		Common.setFtpPassword(StringUtil.unNull(properties.get("ftp-password")));
		Common.setFtpPath(StringUtil.unNull(properties.get("ftp-path")));


		Logger.log("next version：" + Common.getNextVersionStr());
		Logger.log("root-yiyuen：" + Common.getYiyuenRoot());
		Logger.log("root-yiyuen-admin：" + Common.getYiyuenAdminRoot());
		Logger.log("filename-full：" + Common.getFullFilename());
		Logger.log("filename-upgrade：" + Common.getUpgradeFilename());
		Logger.log("excludedExts：" + Common.getExcludedExts());
		Logger.log("excludedPackFiles：" + Common.getExcludedPackFiles());
		Logger.log("excludedCompareFiles：" + Common.getExcludedCompareFiles());
		Logger.log("actions：" + Common.getActions());
		Logger.log("ftp host：" + Common.getFtpHost());
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


		Actions actions = Common.getActions();

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
				new Uploader().todo();;
			}
	
			// 使中心服务器发布版本当前版本。
			if (actions.publishable()) {
				new Publisher().todo();;
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 清除临时目录。
			FileUtil.delete(Common.getTempDir());
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
