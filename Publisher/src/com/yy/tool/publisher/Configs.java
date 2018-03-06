package com.yy.tool.publisher;

import java.io.File;
import java.util.List;

import com.rt.web.config.SystemConfig;

/**
 * 配置集中管理。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Configs {
	
	/** 中心服务器域名。 */
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
	
	/** 排除的扩展名类型。 */
	private static List<String> excludeExts;
	/** 排除的文件或文件夹列表，包括所有工程。 */
	private static List<File> excludeFiles;

	/** 发布执行的动作列表。*/
	private static Actions actions;
	
	/** 打包的方式，big、small、patch。 */
	private static String pack;
	
	
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
		Configs.yiyuenRoot = yiyuenRoot;
	}

	public static String getYiyuenAdminRoot() {
		return yiyuenAdminRoot;
	}

	public static void setYiyuenAdminRoot(String yiyuenAdminRoot) {
		Configs.yiyuenAdminRoot = yiyuenAdminRoot;
	}

	public static String getFullFilename() {
		return fullFilename;
	}

	public static void setFullFilename(String fullFilename) {
		Configs.fullFilename = fullFilename;
	}

	public static String getUpgradeFilename() {
		return upgradeFilename;
	}

	public static void setUpgradeFilename(String upgradeFilename) {
		Configs.upgradeFilename = upgradeFilename;
	}

	public static List<String> getExcludeExts() {
		return excludeExts;
	}

	public static void setExcludeExts(List<String> excludeExts) {
		Configs.excludeExts = excludeExts;
	}

	public static List<File> getExcludeFiles() {
		return excludeFiles;
	}

	public static void setExcludeFiles(List<File> excludeFiles) {
		Configs.excludeFiles = excludeFiles;
	}

	public static Actions getActions() {
		return actions;
	}

	public static void setActions(Actions actions) {
		Configs.actions = actions;
	}

	public static String getPack() {
		return pack;
	}

	public static void setPack(String pack) {
		Configs.pack = pack;
	}
}
