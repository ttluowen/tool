package com.yy.tool.servermonitor;

import java.util.Properties;

import com.rt.log.Logger;
import com.rt.util.number.NumberUtil;
import com.rt.util.proterty.PropertyUtil;
import com.rt.util.string.StringUtil;
import com.rt.web.config.SystemConfig;

/**
 * 服务器 CPU、内存等性能监视服务。
 * 采集到服务器的性能指标，然后报给 MI 服务。
 * 
 * @since 2017-12-05
 * @version 1.0
 * @author Luowen
 */
public class Monitor {

	/** 当前版本。 */
	public final static int VERSION = SystemConfig.parseVersion("1.0");
	
	
	/**
	 * 初始化操作。
	 * 
	 * @return
	 */
	public static boolean init() {

		// 获取当前运行位置。
		String path = System.getProperty("user.dir") + "\\";


		// 设置系统目录和日志位置。
		SystemConfig.setSystemPath(path);
		Logger.setSystemPath(path);


		// 读取配置。
		Properties properties = PropertyUtil.read(path + "config.properties");
		if (properties != null) {
			String id = properties.getProperty("id");
			String sign = properties.getProperty("sign");

			if (!StringUtil.isEmpty(id) && !StringUtil.isEmpty(sign)) {
				Reportor.set(id, sign);
				
				String reportUrl = properties.getProperty("reportUrl");
				if (!StringUtil.isEmpty(reportUrl)) {
					Reportor.setReportUrl(reportUrl);
				}
				
				Watcher.setPeriod(NumberUtil.parseInt(properties.getProperty("period")));
				
				return true;
			} else {
				Logger.log("读取配置信息错误，请检查 config.properties 配置");
			}
		}
		
		
		return false;
	}

	
	/**
	 * 执行入口。
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		if (!init()) {
			Logger.log("初始化失败");
			return;
		}


		// 启动并自动上报。
		new Watcher(new Reportor()).start();
		
		
		Logger.log("ServerMonitor 启动成功，采集周期 " + Watcher.getPeriod() + "秒");
	}
}
