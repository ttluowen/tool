package com.yy.tool.publisher.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

import com.yy.log.Logger;
import com.yy.tool.publisher.Common;
import com.yy.util.string.StringUtil;

/**
 * 上传版本文件操作。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Uploader implements ActionInterface {

	@Override
	public void todo() {

		Logger.log("开始上传");

		
		Date startDate = new Date();
		

		// 要上传的版本号及文件。
		String version = Common.getNextVersionStr();
		File fullVersionFile = Common.getTheVersionFile(version);
		File upgradeVersionFile = Common.getTheVersionUpgradeFile(version);

		// 校验文件是否存在。
		if (fullVersionFile == null || !fullVersionFile.exists()) {
			Logger.log("v" + version + " 全量包文件不存在");
		}
		if (upgradeVersionFile == null || !upgradeVersionFile.exists()) {
			Logger.log("v" + version + " 增量包文件不存在");
		}
		
		if ((fullVersionFile == null || !fullVersionFile.exists())
				&& (upgradeVersionFile == null || !upgradeVersionFile.exists())) {
			Logger.log("没有可上传的文件，中止操作");
			return;
		}


		// 实例化 FTP，开始上传。
		FTPClient ftpClient = new FTPClient();
		
		
		try {
			// 输出打印面板。
//			ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
			

			// 登录。
			ftpClient.connect(Common.getFtpHost(), Common.getFtpPort());
			if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
				if (ftpClient.login(Common.getFtpUsername(), Common.getFtpPassword())) {
					Logger.log("登录成功");
				} else {
					Logger.log("登录失败");
					return;
				}
			} else {
				Logger.log("远程 FTP 不可用");
				return;
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
				if (ftpClient.makeDirectory(path)) {
					if (!ftpClient.changeWorkingDirectory(path)) {
						Logger.log("目录[" + path + "]创建成功，但切换失败");
						return;
					}
				} else {
					Logger.log("创建[ + " + path + "]目录失败");
				}
			}


			// 开始上传。
			List<File> uploadFiles = new ArrayList<>();
			if (upgradeVersionFile != null) {
				uploadFiles.add(upgradeVersionFile);
			}
//			if (fullVersionFile != null) {
//				uploadFiles.add(fullVersionFile);
//			}

			int uploadCount = uploadFiles.size();
			int successCount = 0;
			for (File file : uploadFiles) {
				successCount += upload(ftpClient, path, file) ? 1 : 0;
			}

			Logger.log("本次上传 " + uploadCount + " 个文件，成功 " + successCount + " 个，失败 " + (uploadCount - successCount) + " 个");
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
		
		
		Date endDate = new Date();
		long cost = (endDate.getTime() - startDate.getTime()) / 1000;
		long minutes = cost / 60;
		long seconds = cost % 60;
		
		Logger.log("上传结束，共用时" +  + minutes + "分" + seconds + "秒");
	}
	
	
	/**
	 * 上传操作句柄。
	 * 
	 * @param ftpClient
	 * @param path
	 * @param localFile
	 * @param filename
	 * @return
	 */
	private boolean upload(FTPClient ftpClient, String path, File localFile) {

		final String filename = localFile.getName();


		Logger.log("上传 " + path + filename);
		
		
		try {
			// 校验远程是否已存在当前版本文件。
			FTPFile remoteFile = null;
			FTPFile[] remoteFiles = ftpClient.listFiles(path, new FTPFileFilter() {
				public boolean accept(FTPFile file) {
					return file.getName().equals(filename);
				}
			});
			if (remoteFiles.length > 0) {
				remoteFile = remoteFiles[0];
			}

			// 远程文件大小。
			long remoteFileSize = remoteFile != null ? remoteFile.getSize() : 0;
			

			// 设置起始点。
			InputStream in = new FileInputStream(localFile);
			in.skip(remoteFileSize);
			
			ftpClient.setRestartOffset(remoteFileSize);


			// 上传。
			return ftpClient.storeFile(filename, in);
		} catch (IOException e) {
			Logger.printStackTrace(e);
			return false;
		}
	}
}
