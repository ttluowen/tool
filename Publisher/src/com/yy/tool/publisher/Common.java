package com.yy.tool.publisher;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rt.util.file.FileUtil;
import com.rt.web.config.SystemConfig;

/**
 * 配置集中管理。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Common {

	/** 中心服务器域名。 */
//	public static final String DOMAIN = "http://192.168.31.248:86/";
	public static final String DOMAIN = "http://www.yiyuen.com/";
	

	/** 下一发布版本的版本号。 */
	private static int nextVersion;
	
	
	/** 版本输出目录。 */
	private static File outDir;
	/** 打包、比较时的目录。 */
	private static File tempDir;
	

	/** yiyuen 工程根目录。*/
	private static String yiyuenRoot;
	/** yiyuen-admin 工程根目录。 */
	private static String yiyuenAdminRoot;
	
	/** 全量包文件名格式。 */
	private static String fullFilename;
	/** 增量包文件名格式。 */
	private static String upgradeFilename;
	/** 全量包文件名匹配正则。 */
	private static String fullFilenameReg;
	/** 增量包文件名匹配正则。 */
	private static String upgradeFilenameReg;
	
	/** 排除的扩展名类型。 */
	private static List<String> excludeExts;
	/** 排除的文件或文件夹列表，包括所有工程。 */
	private static List<File> excludeFiles;

	/** 发布执行的动作列表。*/
	private static Actions actions;
	
	/** 打包的方式，big、small、patch。 */
	private static String pack;
	
	/** 打包后的升级包，在升级前是否要先停止 Tomcat 服务。 */
	private static boolean upgradeBeforeStop;
	/** 打包后的升级包，在升级后是否要重启 Tomcat 服务。 */
	private static boolean upgradeAfterRestart;
	
	
	/** FTP 主机地址。 */
	private static String ftpHost;
	/** FTP 端口号。 */
	private static int ftpPort;
	/** FTP 用户名。 */
	private static String ftpUsername;
	/** FTP 密码。 */
	private static String ftpPassword;
	/** FTP 远程目录。 */
	private static String ftpPath;
	
	
	public static void setNextVersion(int version) {
		nextVersion = version;
	}
	
	public static int getNextVersion() {
		return nextVersion;
	}
	
	public static String getNextVersionStr() {
		return SystemConfig.toStringVersion(nextVersion);
	}
	
	
	public static void setRoot(String root) {
		
		outDir = new File(root, "out");
		tempDir = new File(root, "temp");
		
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		} else {
			// 删除已有的内容，再重新创建。
			FileUtil.delete(tempDir);
			tempDir.mkdir();
		}
	}
	
	public static File getOutDir() {
		return outDir;
	}
	
	public static File getTempDir() {
		return tempDir;
	}

	public static String getYiyuenRoot() {
		return yiyuenRoot;
	}

	public static void setYiyuenRoot(String yiyuenRoot) {
		Common.yiyuenRoot = yiyuenRoot;
	}

	public static String getYiyuenAdminRoot() {
		return yiyuenAdminRoot;
	}

	public static void setYiyuenAdminRoot(String yiyuenAdminRoot) {
		Common.yiyuenAdminRoot = yiyuenAdminRoot;
	}

	public static String getFullFilename() {
		return fullFilename;
	}

	public static void setFullFilename(String fullFilename) {
		Common.fullFilename = fullFilename;
	}

	public static String getUpgradeFilename() {
		return upgradeFilename;
	}

	public static void setUpgradeFilename(String upgradeFilename) {
		Common.upgradeFilename = upgradeFilename;
	}

	public static String getFullFilenameReg() {
		return fullFilenameReg;
	}

	public static void setFullFilenameReg(String fullFilenameReg) {
		Common.fullFilenameReg = fullFilenameReg;
	}

	public static String getUpgradeFilenameReg() {
		return upgradeFilenameReg;
	}

	public static void setUpgradeFilenameReg(String upgradeFilenameReg) {
		Common.upgradeFilenameReg = upgradeFilenameReg;
	}

	public static List<String> getExcludeExts() {
		return excludeExts;
	}

	public static void setExcludeExts(List<String> excludeExts) {
		Common.excludeExts = excludeExts;
	}

	public static List<File> getExcludeFiles() {
		return excludeFiles;
	}

	public static void setExcludeFiles(List<File> excludeFiles) {
		Common.excludeFiles = excludeFiles;
	}

	public static Actions getActions() {
		return actions;
	}

	public static void setActions(Actions actions) {
		Common.actions = actions;
	}

	public static String getPack() {
		return pack;
	}

	public static void setPack(String pack) {
		Common.pack = pack;
	}

	public static boolean isUpgradeBeforeStop() {
		return upgradeBeforeStop;
	}

	public static void setUpgradeBeforeStop(boolean upgradeBeforeStop) {
		Common.upgradeBeforeStop = upgradeBeforeStop;
	}

	public static boolean isUpgradeAfterRestart() {
		return upgradeAfterRestart;
	}

	public static void setUpgradeAfterRestart(boolean upgradeAfterRestart) {
		Common.upgradeAfterRestart = upgradeAfterRestart;
	}

	public static String getFtpHost() {
		return ftpHost;
	}

	public static void setFtpHost(String ftpHost) {
		Common.ftpHost = ftpHost;
	}

	public static int getFtpPort() {
		return ftpPort;
	}

	public static void setFtpPort(int ftpPort) {
		Common.ftpPort = ftpPort;
	}

	public static String getFtpUsername() {
		return ftpUsername;
	}

	public static void setFtpUsername(String ftpUsername) {
		Common.ftpUsername = ftpUsername;
	}

	public static String getFtpPassword() {
		return ftpPassword;
	}

	public static void setFtpPassword(String ftpPassword) {
		Common.ftpPassword = ftpPassword;
	}

	public static String getFtpPath() {
		return ftpPath;
	}

	public static void setFtpPath(String ftpPath) {
		Common.ftpPath = ftpPath;
	}
	
	
	/**
	 * 获取指定版本全量包文件。
	 * 
	 * @param version
	 * @return
	 */
	public static File getTheVersionFile(String version) {

		Pattern pattern = Pattern.compile(Common.getFullFilenameReg(), Pattern.CASE_INSENSITIVE);
		for (File file : Common.getOutDir().listFiles()) {
			String filename = file.getName();
			Matcher matcher = pattern.matcher(filename);

			if (matcher.find()) {
				String vs = matcher.group(1);
				if (vs.equals(version)) {
					return file;
				}
			}
		}

		
		return null;
	}
	
	
	/**
	 * 获取指定版本增量包文件。
	 * 
	 * @param version
	 * @return
	 */
	public static File getTheVersionUpgradeFile(String version) {

		Pattern pattern = Pattern.compile(Common.getUpgradeFilenameReg(), Pattern.CASE_INSENSITIVE);
		for (File file : Common.getOutDir().listFiles()) {
			String filename = file.getName();
			Matcher matcher = pattern.matcher(filename);

			if (matcher.find()) {
				String vs = matcher.group(1);
				if (vs.equals(version)) {
					return file;
				}
			}
		}

		
		return null;
	}
}
