const DIM = {
	/**
	 * 应用名称。
	 * 
	 * @type {String}
	 */
	NAME: "ServerMonitor",
	/**
	 * 最多可监控的数量。
	 * 
	 * @type {Number}
	 */
	MAX_SIZE: 9,
	
	/**
	 * hosts 数据本地存储键名。
	 * 
	 * @type {String}
	 */
	HOSTS_STORAGE_KEY: "hosts",
	
	/**
	 * 接口主机地址。
	 * 
	 * @type {String}
	 */
//	API_HOST: "http://www.yiyuen.com/api/",
	API_HOST: "http://local.yiyuen.com/api/",
	
	/**
	 * WebSocket 主机地址。
	 * 
	 * @type {String}
	 */
//	WS_HOST : "ws://www.yiyuen.com:8080/webSocket/",
	WS_HOST : "ws://local.yiyuen.com/webSocket/",
};
window.DIM = DIM;