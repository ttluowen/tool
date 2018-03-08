package com.yy.tool.upgrader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.rt.log.Logger;
import com.rt.statuscode.Statuscode;
import com.rt.statuscode.StatuscodeMap;
import com.rt.util.http.HttpUtil;
import com.rt.util.number.NumberUtil;
import com.rt.util.proterty.PropertyUtil;
import com.rt.util.string.StringUtil;
import com.rt.web.config.SystemConfig;


/**
 * 从中心服务器下载升级包并升级。
 * 
 * @since 2018-03-08
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

		
		// 当前站点。
		Common.setSiteId(StringUtil.unNull(properties.get("siteId")));


		// 当前版本号。
		Common.setCurrentVersion(SystemConfig.parseVersion(StringUtil.unNull(properties.get("version"))));


		// 设置相关目录。
		File downloadDir = new File(new File(path), "download");

		Common.setRoot(new File(StringUtil.unNull(properties.get("root"))));
		Common.setTomcatHome(new File(StringUtil.unNull(properties.get("tomcatHome"))));
		Common.setDownloadDir(downloadDir);


		// 检查周期。
		int checkPeriod = NumberUtil.parseInt("checkPeriod");
		Common.setCheckPeriod(checkPeriod > 0 ? checkPeriod : 60);
		

		// 匹配包名正则。
		Common.setFullFilenameReg("YiYuen\\-v(\\d+\\.\\d+\\.\\d+)\\-.*?\\.zip");
		Common.setUpgradeFilenameReg("YiYuen\\-v(\\d+\\.\\d+\\.\\d+)\\-upgrade\\-.*?\\.zip");


		// FTP 上传。
		Common.setFtpHost(StringUtil.unNull(properties.get("ftp-host")));
		Common.setFtpPort(NumberUtil.parseInt(properties.get("ftp-port")));
		Common.setFtpUsername(StringUtil.unNull(properties.get("ftp-username")));
		Common.setFtpPassword(StringUtil.unNull(properties.get("ftp-password")));
		Common.setFtpPath(StringUtil.unNull(properties.get("ftp-path")));


		Logger.log("site id：" + Common.getSiteId());
		Logger.log("current version：" + Common.getCurrentVersionStr());
		Logger.log("root：" + Common.getRoot());
		Logger.log("tomcat home：" + Common.getTomcatHome());
		Logger.log("checkPeriod：" + Common.getCheckPeriod());
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


		int period = Common.getCheckPeriod();
		long periodTime = period * 60 * 1000;


		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				try {
					taskDo();
				} catch (Exception e) {
					Logger.printStackTrace(e);
				}
			}
		}, periodTime, periodTime);


		Logger.log("设置定期检查并更新版本任务成功，周期 " + period + " 分钟");
		
		
		// 立即执行一次。
		taskDo();
	}
	
	
	/**
	 * 执行任务 。
	 * 
	 * @throws IOException
	 */
	private void taskDo() throws IOException {
		
		// 检查版本。
		if (!checkVersion()) {
			return;
		}
		Logger.log("当前版本 v" + Common.getCurrentVersionStr() + "，最新版本 v" + Common.getNewestVersionStr() + "，将执行升级操作");


		// 下载升级包。
		List<File> files = download();
		if (files == null || files.size() == 0) {
			return;
		}
		Logger.log("下载升级包成功，下面开始解压升级");


		// 升级操作。
		if (!upgrade(files)) {
			return;
		}
		Logger.log("升级成功，当前版本 v" + Common.getCurrentVersion());
	}
	
	
	/**
	 * 检查是否有可升级的版本，
	 * 如果有会修改 nextVersion 变量。
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean checkVersion() throws IOException {
		
		Common.setNewestVersion(0);
		
		
		StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.get(Common.DOMAIN + "api/base/version/query"));
		
		if (sm.getCode() == Statuscode.SUCCESS) {
			int lastVersion = NumberUtil.parseInt(sm.getResultAsMap().get("number"));
			if (lastVersion != 0) {
				int currentVersion = Common.getCurrentVersion();
				
				if (lastVersion > currentVersion) {
					Common.setNewestVersion(lastVersion);

					return true;
				}
			} else {
				Logger.log("解析最新版本号失败，" + sm.getResultAsDouble());
			}
		} else {
			Logger.log("查询最新版本失败，" + sm.getDescription());
		}
		
		
		return false;
	}
	
	
	/**
	 * 下载升级包。
	 * 
	 * @return
	 */
	private List<File> download() {
		
		FTPClient ftpClient = new FTPClient();
		int currentVersion = Common.getCurrentVersion();
		
		
		try {
			// 输出打印面板。
			ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
			

			// 登录。
			ftpClient.connect(Common.getFtpHost(), Common.getFtpPort());
			if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
				if (ftpClient.login(Common.getFtpUsername(), Common.getFtpPassword())) {
					Logger.log("登录成功");
				} else {
					Logger.log("登录失败");
					return null;
				}
			} else {
				Logger.log("远程 FTP 不可用");
				return null;
			}


			// 统一使用 UTF-8 编码。
			ftpClient.setControlEncoding(StringUtil.UTF8);
			// 被动模式。
			ftpClient.enterLocalPassiveMode();
			// 以二进制方式传输。
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);


			// 切换目录。
			String path = Common.getFtpPath();
			boolean changeDir = ftpClient.changeWorkingDirectory(path);
			if (!changeDir) {
				Logger.log("远程目录[ + " + path + "]不存在");
				return null;
			}


			// 获取远程可升级的增量包版本文件。
			List<FTPFile> upgradeFiles = new ArrayList<>();
			for (FTPFile file : ftpClient.listFiles()) {
				String filename = file.getName();
				int version = Common.parseFileVersion(filename);
				
				if (version > currentVersion && Common.isUpgradeFile(filename)) {
					upgradeFiles.add(file);
				}
			}
			
			// 排序
			FTPFile[] upgradeFileArr = upgradeFiles.toArray(new FTPFile[0]);
			Arrays.sort(upgradeFileArr, new Comparator<FTPFile>() {
				public int compare(FTPFile file1, FTPFile file2) {
					int version1 = Common.parseFileVersion(file1.getName());
					int version2 = Common.parseFileVersion(file2.getName());

					return version1 - version2;
				}
			});


			// 打印可下载文件列表日志。
			String upgradeFileLog = "";
			for (FTPFile file : upgradeFileArr) {
				upgradeFileLog += "," + file.getName();
			}
			Logger.log("可下载文件列表 [" + upgradeFileLog.substring(1) + "]");
			
			
			List<File> localFiles = new ArrayList<>();
			int downloadCount = upgradeFileArr.length;
			int successCount = 0;
			
			for (FTPFile remoteFile : upgradeFileArr) {
				FileOutputStream out = null;
				
				try {
					String filename = remoteFile.getName();
					
					File localFile = new File(Common.getDownloadDir(), filename);
					out = new FileOutputStream(localFile);
					long localFileSize = localFile.length();
					long remoteFileSize = remoteFile.getSize();
					
					
					Logger.log("开始下载 " + filename);
					
					
					if (localFileSize >= remoteFileSize) {
						Logger.log(filename + "已存在，跳过下载");
						continue;
					}
					

					// 设置断开位置。
					if (localFileSize > 0) {
						ftpClient.setRestartOffset(localFileSize);
						Logger.log("断点位置 " + localFileSize);
					}


					// 开始下载。
					if (ftpClient.retrieveFile(filename, out)) {
						successCount++;
						localFiles.add(localFile);

						Logger.log("下载 " + filename + "成功");
					} else {
						Logger.log("下载 " + filename + "失败");
					}
				} catch (Exception e) {
					Logger.printStackTrace(e);
				} finally {
					if (out != null) {
						out.close();
					}
				}
			}


			Logger.log("本次下载 " + downloadCount + " 个文件，成功 " + successCount + " 个，失败 " + (downloadCount - successCount) + " 个");
			
			
			return localFiles;
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}
		
		
		return null;
	}
	
	
	/**
	 * 升级指定版本。
	 * 
	 * @param version
	 * @return
	 * @throws IOException 
	 */
	private boolean upgrade(List<File> files) throws IOException {
		
		for (File file : files) {
			String version = SystemConfig.toStringVersion(Common.parseFileVersion(file.getName()));

			if (!new Upgrade(file, version).run()) {
				Logger.log(version + "升级不成功，中止升级");
				break;
			}
		}
		
		
		return false;
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
