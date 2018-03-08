package com.yy.tool.upgrader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.rt.log.Logger;
import com.rt.statuscode.Statuscode;
import com.rt.statuscode.StatuscodeMap;
import com.rt.util.date.DateUtil;
import com.rt.util.file.FileUtil;
import com.rt.util.http.HttpUtil;
import com.rt.util.string.StringUtil;
import com.rt.util.zip.ZipUtil;
import com.rt.web.config.SystemConfig;


/**
 * 具体升级句柄。
 * 
 * @since 2018-03-08
 * @version 1.0
 * @author Luowen
 */
public class Upgrade {
	
	/** config.properties 配置文件名。 */
	public static final String CONFIG_FILENAME = "config.properties";

	/**
	 * 升级前是否要重启 Tomcat 服务。
	 * 1|0
	 */
	public static final String BEFORE_STOP = "beforeStop";
	/**
	 * 升级完成后是否要重启 Tomcat 服务。
	 * 1|0
	 */
	public static final String AFTER_RESTART = "afterRestart";


	/** 差异化待覆盖或新增文件的目录名。 */
	public static final String FILE_DIR_NAME = "file";
	/** 升级时的备份目录名。 */
	public static final String BACKUP_DIR_NAME = "backup";
	/** 待删除的文件的列表文件。 */
	public static final String DELETE_LIST_FILENAME = "deleteList.txt";


	// 本地升级包。
	private File packFile;
	// 升级版本号。
	private String version;

	
	// 解压输出及升级时的位置。
	private File outDir;


	// 升级前是否停止 Tomcat。
	private boolean beforeTop;
	// 升级后是否重启 Tomcat，如果 升级前停止了，那升级后始终会启动。
	private boolean afterRestart;

	
	/*
	 * 根目录可更新的文件、文件夹列表。
	 * 在执行备份操作时会更新该值。
	 */
	private List<String> fileDirFileList;
	/*
	 * 待删除的文件列表。
	 * 在执行备份操作时会更新该值。
	 */
	private List<String> deleteFileList;


	/**
	 * 构造函数。
	 */
	public Upgrade(File packFile, String version) {
		
		this.packFile = packFile;
		this.version = version;
	}


	/**
	 * 执行入口。
	 */
	public boolean run() {

		boolean result = false;


		Logger.log("开始升级" + version);


		try {
			result =

			// 解压升级包。
			step1Unzip()

			// 备份。
			&& step2Backup()

			// 更新前停止。
			&& step3BeforeStopTomcat()

			// 删除不再需要的文件。
			&& step4Delete()

			// 复制/覆盖文件。
			&& step5Copy()
			;
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 更新完后重启。
			step6StartTomcat();
		}


		try {
			if (result) {
				// 更新成功，更新当前版本号配置。
				updateProperties();
				// 向中心服务器更新当前版本号。
				updateSiteVersion();
			} else {
				// 更新不成功，回滚版本。
				rollback();
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 最后再做清理工作。
			stepLastClean();

			Logger.log(version + "更新完成");
		}


		return result;
	}

	
	/**
	 * 第一步，解压升级包。
	 * 
	 * @return
	 */
	private boolean step1Unzip() {
		
		Logger.log("解压");


		String filename = packFile.getName();
		filename = filename.substring(0, filename.lastIndexOf("."));
		outDir = new File(Common.getDownloadDir(), filename);


		// 解压。
		try {
			ZipUtil.uncompress(packFile, outDir);
		} catch (Exception e) {
			Logger.printStackTrace(e);
			return false;
		}
		

		// 读取配置内容。
		File configFile = new File(outDir, CONFIG_FILENAME);
		if (configFile.exists()) {
			try {
				PropertiesConfiguration config = new PropertiesConfiguration(configFile);
				beforeTop = config.getInt(BEFORE_STOP) == 1;
				afterRestart = beforeTop || config.getInt(AFTER_RESTART) == 1;
			} catch (ConfigurationException e) {
				Logger.printStackTrace(e);
			}
		}


		return true;
	}
	
	
	/**
	 * 第二步，根据升级包，备份文件。
	 * 数据库无法备份。
	 * 
	 * 根据解压后的文件及待删除文件列表，先把这些文件都备份到 backup 文件夹下。
	 * 
	 * @return
	 */
	private boolean step2Backup() {

		Logger.log("备份");


		// file 文件夹。
		File fileDir = new File(outDir, FILE_DIR_NAME);

		// backup 文件夹。
		File backupDir = new File(outDir, BACKUP_DIR_NAME);


		/*
		 * 添加待更新的文件、文件夹列表。
		 */
		fileDirFileList = new ArrayList<>();
		if (fileDir.exists()) {
			for (String file : fileDir.list()) {
				fileDirFileList.add(file);
			}
		}


		/*
		 * 解析待删除文件列表。
		 */
		String[] deleteFileArray;
		File deleteListFile = new File(outDir, DELETE_LIST_FILENAME);

		if (deleteListFile.exists()) {
			deleteFileArray = FileUtil.read(deleteListFile).split("\n");
		} else {
			deleteFileArray = new String[0];
		}

		deleteFileList = new ArrayList<>();
		for (String file : deleteFileArray) {
			// 过滤前后空格。
			file = file.trim();

			// 过滤空行和注释行。
			if (!file.isEmpty() && file.indexOf("#") == -1) {
				deleteFileList.add(file);
			}
		}


		/*
		 * 备份待删除文件。
		 */
		for (String file : deleteFileList) {
			try {
				File sourceFile = new File(Common.getRoot(), file);

				if (sourceFile.exists()) {
					File targetFile = new File(backupDir, file);

					if (sourceFile.isFile()) {
						FileUtil.copy(sourceFile, targetFile);
					} else {
						FileUtil.copyDir(sourceFile, targetFile);
					}
				}
			} catch (Exception e) {
				Logger.printStackTrace(e);
			}
		}


		/*
		 * 备份待更新文件。
		 */
		for (String file : fileDirFileList) {
			File sourceFile = new File(Common.getRoot(), file);
			File targetFile = new File(backupDir, file);

			if (sourceFile.exists()) {
				try {
					if (sourceFile.isDirectory()) {
						FileUtil.copyDir(sourceFile, targetFile);
					} else {
						FileUtil.copy(sourceFile, targetFile);
					}
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}


		return true;
	}


	/**
	 * 第三步，升级前停止 Tomcat 服务，如果更新需要停止的话。
	 * 有些更新不需要停止。
	 * 
	 * @return
	 */
	private boolean step3BeforeStopTomcat() {

		if (beforeTop) {
			Logger.log("停止 Tomcat 服务");

			TomcatServer.shutdown();
		}


		return true;
	}
	
	
	/**
	 * 第四步，删除一些不再需要的文件。
	 * 
	 * @return
	 */
	private boolean step4Delete() {
		
		Logger.log("删除不需要的文件");


		for (String f : deleteFileList) {
			File file = new File(Common.getRoot(), f);

			if (file.exists()) {
				if (file.isFile()) {
					FileUtil.delete(file);
				} else {
					FileUtil.deleteDir(file);
				}
			}
		}


		return true;
	}


	/**
	 * 第五步，复制/覆盖文件。
	 * 
	 * @return
	 */
	private boolean step5Copy() {
		
		Logger.log("更新/覆盖文件");


		File fileDir = new File(outDir, FILE_DIR_NAME);


		for (String item : fileDirFileList) {
			File sourceFile = new File(fileDir, item);
			File targetFile = new File(Common.getRoot(), item);

			try {
				if (sourceFile.isDirectory()) {
					FileUtil.copyDir(sourceFile, targetFile);
				} else {
					FileUtil.copy(sourceFile, targetFile);
				}
			} catch (IOException e) {
				Logger.printStackTrace(e);
			}
		}


		return true;
	}


	/**
	 * 第六步，重启系统（可选）。
	 * 
	 * @return
	 */
	private boolean step6StartTomcat() {

		if (afterRestart) {
			Logger.log("重新启动 Tomcat 服务");

			TomcatServer.startup();
		}


		return true;
	}
	
	
	/**
	 * 升级完成后（不论成功还是失败）缓存等的清理。
	 */
	private void stepLastClean() {
		
		Logger.log("清理更新");


		FileUtil.deleteDir(outDir);
	}


	/**
	 * 升级回滚。
	 */
	private void rollback() {
		
		Logger.log("更新回滚");
	}


	/**
	 * 更新配置文件。
	 * 更新为当前版本号，时间为当前时间。
	 * 
	 * @return
	 * @throws ConfigurationException 
	 */
	private boolean updateProperties() throws ConfigurationException {

		String updatetime = DateUtil.get(1);


		// 更新本地配置文件信息。
		File localConfigFile = new File(new File(SystemConfig.getSystemPath()), CONFIG_FILENAME);
		if (localConfigFile.exists()) {
			PropertiesConfiguration localConfig = new PropertiesConfiguration(localConfigFile);
			localConfig.setProperty("version", version);
			localConfig.setProperty("updatetime", updatetime);
	
			localConfig.save();
		} else {
			Logger.log(localConfigFile + "文件不存在");
			
			return false;
		}


		// 更新工程配置文件信息。
		File projectConfigFile = new File(Common.getRoot(), "WEB-INF/configs.properties");
		if (projectConfigFile.exists()) {
			PropertiesConfiguration projectConfig = new PropertiesConfiguration(projectConfigFile);
			projectConfig.setProperty("version", version);
			projectConfig.setProperty("updatetime", updatetime);
	
			projectConfig.save();
		} else {
			Logger.log(projectConfigFile + "文件不存在");
			
			return false;
		}
		
		
		return true;
	}
	
	
	/**
	 * 向中心服务器更新自己当前的版本号。
	 * 
	 * @return
	 * @throws IOException 
	 */
	private boolean updateSiteVersion() throws IOException {
		
		Map<String, Object> params = new HashMap<>();
		params.put("siteId", Common.getSiteId());
		params.put("version", SystemConfig.parseVersion(version));
		
		String url = Common.DOMAIN + "api/base/version/updateSiteVersion?siteId={siteId}&version={version}";
		url = StringUtil.substitute(url, params);
		
		
		StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.get(url));
		if (sm.getCode() == Statuscode.SUCCESS) {
			Logger.log("向中心服务器更新当前版本成功");
			
			return true;
		} else {
			Logger.log("向中心服务器更新当前版本失败，" + sm.getDescription());
			
			return false;
		}
	}
}
