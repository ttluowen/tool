package com.yy.tool.servermonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.alibaba.fastjson.JSONObject;
import com.rt.encrypt.BASE64Q;
import com.rt.log.Logger;
import com.rt.statuscode.Statuscode;
import com.rt.statuscode.StatuscodeMap;
import com.rt.util.http.HttpUtil;
import com.rt.util.string.StringUtil;

/**
 * 上报管理类。
 * 
 * @since 2018-02-06
 * @version 1.0
 * @author Luowen
 */
public class Reportor {

	/** 上报服务器接口地址。 */
	private static final String REPORT_URL = "http://www.yiyuen.com/api/serverMonitor/report";
	private static String reportUrl = REPORT_URL;
	
	
	private static String key;
	private static String code;
	
	
	/** 当前是否正在上报中。 */
	private static boolean isReporting = false;
	/**
	 * 待上报的队列数据，
	 * 有新数据进来时会直接覆盖原始的数据，不管有没有上报完成。
	 */
	private static Map<String, Object> reportData;
	
	
	/**
	 * 设置 id 身份和 code 密码。
	 * 
	 * @param id
	 * @param sign
	 */
	public static void set(String id, String sign) {

		key = StringUtil.gsid(8);
		code = new BASE64Q(key, true).encode(id + "," + sign); 
	}
	
	
	/**
	 * 设置上报接口地址。
	 * 
	 * @param url
	 */
	public static void setReportUrl(String url) {
		
		reportUrl = url;
	}
	
	
	/**
	 * 上报指定数据。
	 * 
	 * @param data
	 * @return
	 */
	public void report(Map<String, Object> data) {
		
		if (isReporting) {
			reportData = data;
			return;
		}
		
		
		// 上报中状态。
		isReporting = true;


		try {
			String jsonData = JSONObject.toJSONString(data);

			// 参数设置。
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("key", key));
			params.add(new BasicNameValuePair("code", code));
			params.add(new BasicNameValuePair("data", jsonData));


			// 提交 HTTP 请求。
			StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.post(reportUrl, params));
			if (sm != null) {
				if (sm.getCode() == Statuscode.SUCCESS) {
					// 上报成功，再检查是否有待上报的。
					if (reportData != null) {
						Map<String, Object> copy = reportData;
						reportData = null;

						// 继续上报待上报的。
						report(copy);
					}
				} else {
					Logger.log(params.toString() + "上报失败，" + sm.getDescription());
				}
			} else {
				Logger.log("请求失败");
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		} finally {
			// 上报结束。
			isReporting = false;
		}
	}
}
