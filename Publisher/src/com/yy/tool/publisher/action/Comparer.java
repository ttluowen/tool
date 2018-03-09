package com.yy.tool.publisher.action;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rt.encrypt.MD5;
import com.rt.log.Logger;
import com.rt.util.file.FileUtil;
import com.rt.util.string.StringUtil;
import com.rt.util.zip.ZipUtil;
import com.rt.web.config.SystemConfig;
import com.yy.tool.publisher.Common;

/**
 * 与上一版本的增量文件比较。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Comparer implements ActionInterface {

	/** 差异文件夹名称。 */
	public static final String FILE_DIR_NAME = "file";

	/** 配置文件。 */
	public static final String CONFIG_PROPERTIES_FILENAME = "config.properties";
	/** 打包输出的配置文件。 */
	public static final String UPGRADE_CONFIG_PROPERTIES_FILENAME = "config.properties";
	/** 待删除的文件的列表文件。 */
	public static final String DELETE_LIST_FILENAME = "deleteList.txt";
	

	/** 比较结果相同。 */
	private static int EQUALS = 0;
	/** 比较结果不同。 */
	private static int DIFFERENT = 1;
	
	
	// 版本号。
	private String prevVersion;
	private String nextVersion;
	
	// 版本目录临时文件夹。
	private File prevDir;
	private File nextDir;
	private File compareDir;

	// 差异文件列表。
	private List<String> differentList = new ArrayList<>();
	// 待删除文件列表。
	private List<String> deleteList = new ArrayList<>();


	@Override
	public void todo() {

		Logger.log("开始版本比较");
		
		
		Date startDate = new Date();
		
		
		boolean success =
		
		// 初始化目录。
		initDir()
				
		// 解压。
		&& unzip()

		// 比较。
		&& compare(prevDir.list(), nextDir.list(), "")

		// 输出结果。
		&& out()
		
		// 打包。
		&& zip()
		
		// 清理。
		&& clean();
		
		
		if (success) {
			System.out.println("success");
		}


		Date endDate = new Date();
		long cost = (endDate.getTime() - startDate.getTime()) / 1000;
		long minutes = cost / 60;
		long seconds = cost % 60;

		Logger.log("版本比较完成，共用时" +  + minutes + "分" + seconds + "秒");
	}
	
	
	/**
	 * 初始化目录。
	 * 
	 * @return
	 */
	private boolean initDir() {

		List<String> versions = new ArrayList<>();
		nextVersion = Common.getNextVersionStr();


		// 获取版本文件列表。
		Pattern pattern = Pattern.compile(Common.getFullFilenameReg(), Pattern.CASE_INSENSITIVE);
		for (File file : Common.getOutDir().listFiles()) {
			if (file.isDirectory()) {
				continue;
			}

			String filename = file.getName();
			Matcher matcher = pattern.matcher(filename);
			
			if (matcher.find()) {
				String version = matcher.group(1);
				if (!version.equals(nextVersion)) {
					versions.add(matcher.group(1));					
				}
			}
		}

		
		// 如果没有可用的上一版本，中止。
		if (versions.size() == 0) {
			return false;
		}

		
		// 取最后一个版本。
		prevVersion = versions.get(versions.size() - 1);
		
		
		Logger.log("比较的上一版本是 v" + prevVersion);


		File tempDir = Common.getTempDir();
		prevDir = new File(tempDir, prevVersion);
		nextDir = new File(tempDir, nextVersion);
		compareDir = new File(tempDir, "compare");
		
		if (!prevDir.exists()) {
			prevDir.mkdirs();
		}
		if (!nextDir.exists()) {
			nextDir.mkdirs();
		}
		if (!compareDir.exists()) {
			compareDir.mkdirs();
		}
		
		
		return true;
	}
	
	
	/**
	 * 解压两个版本的压缩包。
	 * 
	 * @throws IOException 
	 */
	private boolean unzip() {
		
		try {
			File prevVersionFile = Common.getTheVersionFile(prevVersion);
			if (prevVersionFile != null) {
				ZipUtil.uncompress(
						prevVersionFile.getAbsolutePath(),
						prevDir.getAbsolutePath()
				);
			}

			File nextVersionFile = Common.getTheVersionFile(nextVersion);
			if (nextVersionFile != null) {
				ZipUtil.uncompress(
						nextVersionFile.getAbsolutePath(),
						nextDir.getAbsolutePath()
				);
			} else {
				return false;
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
			return false;
		}


		return true;
	}


	/**
	 * 比较文件列表。
	 * 
	 * @param prevFileList
	 * @param nextFileList
	 * @param basePath
	 */
	private boolean compare(String[] prevFileList, String[] nextFileList, String basePath) {

		// 将下一版本的文件列表转换成 List 对象，便于作排除操作。
		List<String> nextVersionList = new ArrayList<>();
		for (String item : nextFileList) {
			nextVersionList.add(item);
		}


		for (String prevFileName : prevFileList) {
			String relativeFile = basePath + prevFileName;
			File prevFile = new File(prevDir, relativeFile);
			File nextFile = new File(nextDir, relativeFile);

			/*
			 * 检测该文件或文件夹在新版本中是否还存在。
			 * 如果已经不存在，则将该文件或文件夹添加到待删除列表，
			 * 并不再继续检测该文件夹的子目录。
			 */
			if (!nextFile.exists()) {
				deleteList.add(relativeFile);
				Logger.log("删除[" + relativeFile + "]");
	
				continue;
			}
	
	
			// 将该文件从下一个版本的列表中删除，以表示对该文件已操作过。
			nextVersionList.remove(prevFileName);


			if (prevFile.isFile()) {
				if (compare(prevFile, nextFile) == DIFFERENT) {
					differentList.add(relativeFile);
					Logger.log("更新[" + relativeFile + "]");
				}
			} else {
				compare(prevFile.list(), nextFile.list(), relativeFile + "\\");
			}
		}
	
	
		// 将上一个版本中不存在的文件添加到差异列表中。
		for (String item : nextVersionList) {
			String newFile = basePath + item;
			differentList.add(newFile);

			Logger.log("新增[" + newFile + "]");
		}
		
		
		return true;
	}


	/**
	 * 文件是否相同比较。
	 * 
	 * @param file1
	 * @param file2
	 * @return
	 */
	private int compare(File file1, File file2) {
	
		byte[] byte1 = FileUtil.readAsByte(file1);
		byte[] byte2 = FileUtil.readAsByte(file2);
	
		String md1 = MD5.encodeBytes(byte1);
		String md2 = MD5.encodeBytes(byte2);
	
	
		if (md1.equals(md2)) {
			return EQUALS;
		} else {
			return DIFFERENT;
		}
	}


	/**
	 * 结果输出。
	 */
	private boolean out() {
	
		outFile();
		outConfig();
		
		
		return true;
	}


	/**
	 * 输出差异文件。
	 */
	private boolean outFile() {
		
		Logger.log("输出差异文件");


		String comparePath = SystemConfig.formatDirRelativePath(compareDir.getAbsolutePath());
		String outFilePath = SystemConfig.formatDirRelativePath(new File(compareDir, FILE_DIR_NAME).getAbsolutePath());
		String nextVersionPath = SystemConfig.formatDirRelativePath(nextDir.getAbsolutePath());
	
	
		// 输出差异文件。
		for (String file : differentList) {
			File sourceFile = new File(nextVersionPath + file);
			File outFile = new File(outFilePath + file);
	
			try {
				if (sourceFile.isFile()) {
					FileUtil.copy(sourceFile, outFile);
				} else {
					FileUtil.copyDir(sourceFile, outFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		// 生成待删除文件列表。
		StringBuffer content = new StringBuffer("# 一行一个文件或文件夹，目录使用 /、\\ 都没有关系。\n\n");
	
		for (String file : deleteList) {
			content.append(file).append("\n");
		}

		FileUtil.save(comparePath + DELETE_LIST_FILENAME, content.toString());
		
		
		return true;
	}


	/**
	 * 输出配置文件。
	 */
	private boolean outConfig() {
		
		Logger.log("输出配置文件");


		String content = ""
				+ "beforeStop=" + (Common.isUpgradeBeforeStop() ? 1 : 0) + "\n"
				+ "afterRestart=" + (Common.isUpgradeAfterRestart() ? 1 : 0) + "\n"
		;


		FileUtil.save(
				new File(compareDir, UPGRADE_CONFIG_PROPERTIES_FILENAME),
				content.toString()
		);
		
		
		return true;
	}


	/**
	 * 打包。
	 */
	private boolean zip() {
		
		Logger.log("打包");


		try {
			Map<String, Object> params = new HashMap<>();
			params.put("version", nextVersion);
			params.put("date", new SimpleDateFormat("yyyyMMdd").format(new Date()));

			String filename = StringUtil.substitute(Common.getUpgradeFilename(), params);


			// 打包。
			ZipUtil.compress(compareDir.getAbsolutePath(), Common.getOutDir().getAbsolutePath() + "\\" + filename);
		} catch (IOException e) {
			Logger.printStackTrace(e);
			return false;
		}
		
		
		return true;
	}


	/**
	 * 清理打包内容。
	 */
	private boolean clean() {
		
		Logger.log("清理");


		FileUtil.deleteDir(prevDir.getAbsolutePath());
		FileUtil.deleteDir(nextDir.getAbsolutePath());
		FileUtil.deleteDir(compareDir.getAbsolutePath());
		
		
		return true;
	}
}
