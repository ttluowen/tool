package com.yy.tool.publisher;

import java.util.List;

/**
 * 配置集中管理。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Configs {

	/** yiyuen 工程根目录。*/
	public static String yiyuenRoot;
	/** yiyuen-admin 工程根目录。 */
	public static String yiyuenAdminRoot;
	
	/** 全量包文件名格式。 */
	public static String fullFilename;
	/** 增量包文件名格式。 */
	public static String upgradeFilename;
	
	/** 排除的扩展名类型。 */
	public static List<String> packExcludeExts;
	/** 排除的文件或文件夹列表，包括所有工程。 */
	public static List<String> excludeFiles;

	/** 发布执行的动作列表。*/
	public static Actions actions;

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

	public static List<String> getPackExcludeExts() {
		return packExcludeExts;
	}

	public static void setPackExcludeExts(List<String> packExcludeExts) {
		Configs.packExcludeExts = packExcludeExts;
	}

	public static List<String> getExcludeFiles() {
		return excludeFiles;
	}

	public static void setExcludeFiles(List<String> excludeFiles) {
		Configs.excludeFiles = excludeFiles;
	}

	public static Actions getActions() {
		return actions;
	}

	public static void setActions(Actions actions) {
		Configs.actions = actions;
	}
}
