package com.yy.tool.upgrader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.rt.log.Logger;
import com.rt.util.number.NumberUtil;
import com.rt.util.string.StringUtil;


/**
 * MI系统管理的主入口。
 * 
 * @since 2015-11-13
 * @version 1.0
 * @author Luowen
 */
public class TomcatServer {

	/** 实例对象。 */
	private static TomcatServer newInstance;
	/** Tomcat 是否正在运行的。 */
	private static boolean isRunning = false;


	/** Tomcat 启动间隔时间，单位秒。在该时间段内的多次启动操作会被忽略。 */
	private static final int STARTUP_INTERVAL = 30;
	/** 上次启动时间，毫秒值。 */
	private static long lastStartDate;
	
	/** 停止超时时间，如果超过该时间仍未关闭 Tomcat 服务，将会强制把进程结束。 */
	private static final int SHUTDOWN_TIMEOUT = 10;
	
	
	// 日志记录列表。
	/*
	 * 在执行 startup 前会进行清空。
	 */
	private static List<String> logs = new ArrayList<>();


	/**
	 * 执行 CMD 命令。
	 * 
	 * @param command
	 */
	private static void runCmd(String command) {

		Process process = null;

		try {
			// 执行 CMD 命令，并返回它的进程。
			String cmd = "CMD.exe /C SET \"CATALINA_HOME=" + Common.getTomcatHome() + "\" && \"" + command + "\"";
			process = Runtime.getRuntime().exec(cmd);

			// 获取进程的的输入流。
			InputStream in = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StringUtil.UTF8));


			// 读取相关的内容。
			String line;
			while ((line = reader.readLine()) != null) {
				Logger.log(line);
				logs.add(line);
			}


			// 读取结束，关闭。
			if (reader != null) {
				reader.close();
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 销毁进程。
			if (process != null) {
				process.destroy();
			}
		}
	}


	/**
	 * 启动 Tomcat。
	 * 
	 * @return
	 */
	private void startupHandler() {

		// 清空。
		logs = new ArrayList<>();


		// 先调用关闭。
		try {
			shutdownHandler();
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}


		String log = "执行启动 Tomcat 命令";
		Logger.log(log);
		logs.add(log);


		// 执行启动 Tomcat 命令。
		runCmd(Common.getTomcatStartupBat().getAbsoluteFile().toString());


		// 标记当前运行的。
		isRunning = true;
	}


	/**
	 * 关闭 Tomcat。
	 * 
	 * @return
	 */
	private void shutdownHandler() {

		String log = "执行关闭 Tomcat 命令";
		Logger.log(log);
		logs.add(log);


		runCmd(Common.getTomcatShutdownBat().getAbsoluteFile().toString());


		/*
		 * 先获取 Tomcat 的进程号，之后再轮循该进程是否还存在，
		 * 如果存在则表示还未完全关闭。
		 * 
		 * 获取进程的运行目录，跟 Tomcat 的目录相比较来判断是否是 Tomcat 的进程。
		 * D:\SVN\MI\Deploy\MIBin
		 */
		Sigar sigar = new Sigar();
		String tomcatCwd = System.getProperty("user.dir");
		long tomcatPid = 0;

		try {
			// 获取系统进程列表。
			long[] pids = sigar.getProcList();
			
			// JRE 环境。
			String jreHome = System.getProperty("java.home");


			for (long pid : pids) {
				try {
					// 获取当前进程的运行信息。
					ProcExe exe = sigar.getProcExe(pid);
					// 启动的位置。D:\SVN\MI\Deploy\MIBin
					String cwd = exe.getCwd();
					// 程序名称。C:\Program Files\Java\jre1.8.0_45\bin\java.exe
					String name = exe.getName();

					// 当前进程运行时的 Java 环境变量位置。C:\Program Files\Java\jre1.8.0_45
					String envHome = sigar.getProcEnv(pid, "JAVA_HOME");
					if (StringUtil.isEmpty(envHome)) {
						envHome = jreHome;
					}


					/*
					 * 获取该进程号的运行目录。
					 * 有些系统级的进程可能会没有权限访问，所以会抛出异常。
					 */
					if (cwd.equals(tomcatCwd) && name.indexOf(envHome) > -1) {
						log = "检测到 Tomcat 相似进程，Pid[" + pid + "]，Name[" + name + "]，EvNHome[" + envHome + "]";
						Logger.log(log);
						logs.add(log);


						// 匹配到 Tomcat 的进程号，退出循环。
						tomcatPid = pid;
						break;
					}
				} catch (SigarException e) {
				}
			}
		} catch (Exception e) {
			tomcatPid = 0;

			Logger.printStackTrace(e);
		}


		// 轮循判断 Tomcat 是否真正关闭了。
		if (tomcatPid != 0) {
			long time = new Date().getTime();

			while (true) {
				try {
					sigar.getProcState(tomcatPid);
				} catch (SigarException e) {
					// 如果获取进行的状态失败，可能是该进行已不存在，所以可以退出。
					break;
				}

				// 检测是否关闭超时，超过限定时间仍不能关闭的，将强制关闭进程。
				if ((new Date().getTime() - time) / 1000 > SHUTDOWN_TIMEOUT) {
					log = "关闭进程[" + tomcatPid + "]超时，将强制关闭进程";
					Logger.log(log);
					logs.add(log);


					try {
						// 执行强制关闭进程命令。
						runCmd("taskkill /f /pid " + tomcatPid);
					} catch (Exception e) {
						Logger.printStackTrace(e);
					}
					
					break;
				}


				// 间隔一段时间后再重试。
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
		}


		// 标记当前非运行的。
		isRunning = false;
	}
	
	
	/**
	 * 清除上次启动时间。
	 * 一般可用于连续重启，因为短时间内调用多次 startup 默认是不被允许的。
	 */
	public static void cleanLastStartDate() {
		
		lastStartDate = 0;
	}
	
	
	/**
	 * 启动 Tomcat。
	 */
	public static void startup() {
		
		// 检测最小启动时间间隔。
		long date = new Date().getTime();
		int diff = NumberUtil.parseInt((date - lastStartDate) / 1000);

		if (diff < STARTUP_INTERVAL) {
			Logger.log("两次启动时间过短，请 " + (STARTUP_INTERVAL - diff) + " 秒后再试");

			return;
		}


		// 更新启动时间。
		lastStartDate = date;


		// 检测服务是否初始化过。
		if (newInstance == null) {
			newInstance = new TomcatServer();
		}


		// 新开一个线程来启动服务。
		new Thread(new Runnable() {
			public void run() {
				newInstance.startupHandler();
			}
		}).start();
	}


	/**
	 * 关闭 Tomcat。
	 */
	public static void shutdown() {

		if (newInstance != null) {
			newInstance.shutdownHandler();
		}
	}


	/**
	 * 重启 Tomcat。
	 */
	public static void restart() {
		
		startup();
	}
	
	
	/**
	 * 获取当前 Tomcat 的状态，返回当前是否是正在运行的。
	 * 
	 * @return
	 */
	public static boolean isRunning() {
		
		return isRunning;
	}


	/**
	 * 获取执行日志。
	 * 
	 * @return
	 */
	public static List<String> getLogs() {
		
		return logs;
	}
}
