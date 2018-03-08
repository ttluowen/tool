package com.yy.tool.upgrader;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	

	/** 当前站点编号。 */
	private static String siteId;
	
	
	/** 当前版本的版本号。 */
	private static int currentVersion;
	/** 服务器最新版本的版本号。 */
	private static int newestVersion;
	

	/** 项目工程的真实目录。 */
	private static File root;
	/** Tomcat 根目录。 */
	private static File tomcatHome;
	
	/** 下载目录，下载的文件会保留。 */
	private static File downloadDir;
	
	
	/** 检查周期，单位分钟。 */
	private static int checkPeriod;
	
	
	/** 全量包文件名匹配正则。 */
	private static String fullFilenameReg;
	/** 增量包文件名匹配正则。 */
	private static String upgradeFilenameReg;


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
	
	
	public static String getSiteId() {
		return siteId;
	}

	public static void setSiteId(String siteId) {
		Common.siteId = siteId;
	}

	public static void setCurrentVersion(int version) {
		currentVersion = version;
	}
	
	public static int getCurrentVersion() {
		return currentVersion;
	}
	
	public static String getCurrentVersionStr() {
		return SystemConfig.toStringVersion(currentVersion);
	}
	
	public static int getNewestVersion() {
		return newestVersion;
	}

	public static void setNewestVersion(int newestVersion) {
		Common.newestVersion = newestVersion;
	}
	
	public static String getNewestVersionStr() {
		return SystemConfig.toStringVersion(newestVersion);
	}

	public static File getRoot() {
		return root;
	}

	public static void setRoot(File root) {
		Common.root = root;
	}

	public static File getTomcatHome() {
		return tomcatHome;
	}

	public static void setTomcatHome(File tomcatHome) {
		Common.tomcatHome = tomcatHome;
	}
	
	public static File getTomcatStartupBat() {
		return new File(getTomcatHome(), "bin/startup.bat");
	}
	
	public static File getTomcatShutdownBat() {
		return new File(getTomcatHome(), "bin/shutdown.bat");
	}

	public static File getDownloadDir() {
		return downloadDir;
	}

	public static void setDownloadDir(File downloadDir) {
		
		Common.downloadDir = downloadDir;
		
		if (!downloadDir.exists()) {
			downloadDir.mkdir();
		}
	}

	public static int getCheckPeriod() {
		return checkPeriod;
	}

	public static void setCheckPeriod(int checkPeriod) {
		Common.checkPeriod = checkPeriod;
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
	 * 解析指定文件的版本号。
	 * 
	 * @param filename
	 * @return
	 */
	public static int parseFileVersion(String filename) {
		
		String[] regs = {
				Common.getUpgradeFilenameReg(),
				Common.getFullFilenameReg()
		};
		
		for (String reg : regs) {
			Matcher matcher = Pattern.compile(reg, Pattern.CASE_INSENSITIVE).matcher(filename);
			if (matcher.find()) {
				String versionStr = matcher.group(1);
				int version = SystemConfig.parseVersion(versionStr);
				if (version > 0) {
					return version;
				}
			}
		}
		

		return 0;
	}
	
	
	/**
	 * 检测当前版本是否是增量升级包。
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean isUpgradeFile(String filename) {
		
		return filename.toLowerCase().indexOf("upgrade") != -1;
	}
}
