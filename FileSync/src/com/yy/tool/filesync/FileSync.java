package com.yy.tool.filesync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.rt.log.Logger;
import com.rt.util.file.FileUtil;
import com.rt.util.number.NumberUtil;
import com.rt.util.proterty.PropertyUtil;
import com.rt.util.string.StringUtil;
import com.rt.web.config.SystemConfig;

/**
 * 文件变化监视器。
 * 
 * @since 2018-02-21
 * @version 1.0
 * @author Luowen
 */
public class FileSync {
	
	/** 默认差异监听时期，单位秒。 */
	private static final int DEFAULT_WATCH_INTERVAL = 2;
	

	/** 监听目录。 */
	private String watchPath;
	/** 同步目录。 */
	private String syncPath;
	
	/** 差异监听周期，单位秒。 */
	private int watchInterval = DEFAULT_WATCH_INTERVAL;

	/** 排除的文件类型。 */
	private List<String> excludeExts = new ArrayList<>();

	/** 排除的文件或文件夹列表。 */
	private List<String> excludeFiles = new ArrayList<>();
	

	/** 文件变化监视器。 */
	private FileAlterationMonitor monitor = null;

	
	/**
	 * 启动监视器。
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception {
		
		monitor.stop();
	}

	
	/**
	 * 关闭监视器。
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		
		monitor.start();
	}

	
	/**
	 * 系统信息初始化。
	 * 
	 * @param second
	 * @throws IOException 
	 */
	private void init() throws IOException {
	
		// 获取当前运行位置。
		String path = System.getProperty("user.dir") + "\\";
	
		
		// 设置系统目录和日志位置。
		SystemConfig.setSystemPath(path);
		Logger.setSystemPath(path);
	
		
		Map<String, String> properties = PropertyUtil.readAsMap(path + "config.properties");
		// 监视目录。
		watchPath = SystemConfig.appendLastFileSeparator(properties.get("watchPath"));
		// 同步目录。
		syncPath = SystemConfig.appendLastFileSeparator(properties.get("syncPath"));
	
		
		// 检查相关目录，如果不存在就立即创建。
		File watchPathFile = new File(watchPath);
		if (!watchPathFile.exists()) {
			watchPathFile.mkdirs();
		}
		watchPath = watchPathFile.getAbsolutePath();
		
		File asyncPathFile = new File(syncPath);
		if (!asyncPathFile.exists()) {
			asyncPathFile.mkdirs();
		}
		syncPath = asyncPathFile.getAbsolutePath();
		
		
		// 差异监听周期。
		watchInterval = NumberUtil.parseInt(StringUtil.unEmpty(properties.get("watchInterval"), DEFAULT_WATCH_INTERVAL + ""));
		
	
		// 排除的文件类型。
		String excludeExtsStr = StringUtil.unNull(properties.get("excludeExts"));
		if (!excludeExtsStr.isEmpty()) {
			for (String item : excludeExtsStr.split(",")) {
				item = item.trim().toLowerCase();
				
				if (!item.isEmpty()) {
					excludeExts.add(item.trim());
				}
			}
		}
	
		
		// 排除的文件或文件夹。
		File excludeFilesFile = new File(path + "excludeFiles.txt");
		if (excludeFilesFile.exists()) {
			for (String line : FileUtil.read(excludeFilesFile).split("\n")) {
				// 过滤前后空格。
				line = line.trim();
	
				// 过滤空行和注释行。
				if (!line.isEmpty() && line.indexOf("#") == -1) {
					excludeFiles.add(new File(watchPath + "\\" + line).getAbsolutePath());
				}
			}
		}


		// 监视器初始化。
		monitor = new FileAlterationMonitor(watchInterval * 1000);
		FileAlterationObserver observer = new FileAlterationObserver(new File(watchPath));
		monitor.addObserver(observer);
		observer.addListener(new SyncListener(this));


		Logger.log("Watch path: " + watchPath);
		Logger.log("Sync path: " + syncPath);
		Logger.log("Exclude exts: " + excludeExts);
		Logger.log("Exclude files: " + excludeFiles);
		Logger.log("Inited");
	}


	/**
	 * 是否是排除的。
	 * @param file
	 * 
	 * @return
	 */
	private boolean excluded(File file) {
		
		if (file == null) {
			return true;
		}

		
		if (file.isFile()) {
			String filename = file.getName();
			int lastPointIndex = filename.lastIndexOf(".");
			String ext = "";
			if (lastPointIndex != -1) {
				ext = filename.substring(lastPointIndex).toLowerCase();
			}
	
			// 文件类型的排除判断。
			if (excludeExts.indexOf(ext) != -1) {
				return true;
			}
		}


		String filePath = file.getAbsolutePath();
		int index = excludeFiles.indexOf(filePath);
		if (index != -1) {
			return true;
		} else {
			for (String item : excludeFiles) {
				if (item.startsWith(filePath)) {
					return true;
				}
			}
		}


		return false;
	}
	
	
	/**
	 * 将监视的文件转换成同步路径文件。
	 * 
	 * @param watchFile
	 * @return
	 */
	private File toSyncFile(File watchFile) {
		
		return new File(watchFile.getAbsolutePath().replace(watchPath, syncPath));
	}



	/**
	 * 新建操作。
	 * 
	 * @param file
	 * @param path
	 */
	public void create(File file) {
		
		try {
			if (excluded(file)) {
				return;
			}
			

			File syncFile = toSyncFile(file);
			
			
			if (file.isDirectory()) {
				// 新建文件夹。
				if (!syncFile.exists()) {
					syncFile.mkdirs();
				}
			} else {
				// 复制文件。
				FileUtil.save(syncFile, FileUtil.readAsByte(file));
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}
	}



	/**
	 * 修改操作。
	 * 
	 * @param file
	 * @param path
	 */
	public void modify(File file) {
		
		// 同新增操作。
		create(file);
	}



	/**
	 * 删除操作。
	 * 
	 * @param file
	 * @param path
	 */
	public void delete(File file) {
		
		try {
			if (excluded(file)) {
				return;
			}
			

			File syncFile = toSyncFile(file);


			if (file.isDirectory()) {
				// 删除文件夹。
				if (syncFile.exists()) {
					syncFile.delete();
				}
			} else {
				// 删除文件。
				FileUtil.delete(syncFile);
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}
	}


	/**
	 * 程序入口。
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		FileSync fileMonitor = new FileSync();
		fileMonitor.init();
		fileMonitor.start();
	}
}