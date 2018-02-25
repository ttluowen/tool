<template>
	<div class="view-page">
		<div class="host-item" v-for="item in hosts">
			<h2 class="host-name">{{item.name}}</h2>

			<ul class="host-number">
				<li class="number-item"><strong class="labelName">CPU</strong><span class="value">{{cpu(item.id) + "%"}}</span></li>
				<li class="number-item"><strong class="labelName">内存</strong><span class="value">{{(memory(item.id).usedPercent || 0) + "%"}}</span></li>
				<li class="number-item"><strong class="labelName">磁盘</strong>
					<ul class="subNumber-list">
						<li class="subNumber-item" v-for="diskItem in disk(item.id)">
							<strong>{{diskItem.model}}</strong>
							<em>读<i class="value">{{util.bytesToSize(diskItem.readSpeed)}}</i></em>
							<em>写<i class="value">{{util.bytesToSize(diskItem.writeSpeed)}}</i></em>
						</li>
					</ul>
				</li>
				<li class="number-item"><strong class="labelName">网络</strong>
					<ul class="subNumber-list">
						<li class="subNumber-item" v-for="networkItem in network(item.id)">
							<strong>{{networkItem.name}}</strong>
							<em>上行<i class="value">{{util.bytesToSize(networkItem.upSpeed)}}</i></em>
							<em>下行<i class="value">{{util.bytesToSize(networkItem.downSpeed)}}</i></em>
						</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</template>

<script>
	import ECharts from "vue-echarts/components/ECharts";
	import "echarts";

	var oWebSocket = null;

	export default {
		components: {
			chart: ECharts
		},
		data() {
			return {
				hosts: [],
				monitorDatas: {},

				// 将全局对象 util 注册到实例中，以使在 template 中使用。
				util
			}
		},
		computed: {
			hostIds() {
				let ids = [];
				this.hosts.forEach(host => {
					if(host.id) {
						ids.push(host.id);
					}
				});

				return ids;
			},
		},
		mounted() {
			var _this = this;

			_this.hosts = JSON.parse(localStorage.getItem(DIM.HOSTS_STORAGE_KEY) || "{}");
			console.log(_this.hosts);
			_this.getList(_this.initWebSocket);
		},
		methods: {
			/**
			 * 获取监控列表数据。
			 * 在获取成功后可以回调一个函数。
			 * 
			 * @param {Function} fnCallback
			 * @return {void}
			 */
			getList(fnCallback) {
				var _this = this;

				fetch(DIM.API_HOST + "serverMonitor/getList?id=" + this.hostIds.join(",")).then(oResponse => {
					oResponse.json().then(oAjaxData => {
						if(oAjaxData.c == 1) {
							_this.monitorDatas = oAjaxData.r || {};

							// 成功回调。
							fnCallback && fnCallback.apply(_this);
						} else {
							alert("获取列表数据失败");
						}
					});
				});
			},
			/**
			 * 初始化 WebSocket。
			 * 
			 * @returnl {void}
			 */
			initWebSocket() {
				var _this = this;

				oWebSocket = util.webSocket({
					url: DIM.WS_HOST + "common",
					onopen() {
						oWebSocket.sendData("regist", {
							key: DIM.NAME,
						});
					},
					onmessage: _this.onWSMessage
				});
			},
			onWSMessage(sAction, oData) {
				if(sAction == "registBack") {
					// 注册返回。

					if(oData.c == 1) {
						// 添加要监听的项。
						this.addWatch();
					} else {
						alert("注册失败");
					}
				} else if(sAction == "watchBack") {
					if(oData.c != 1) {
						alert("添加 watch 失败");
					}
				} else if(sAction == "update") {
					this.addNewestData(oData);
				}
			},
			/**
			 * 添加要监听的绑定服务器参数，可使服务器能针对性的推送。
			 * 
			 * @return {void}
			 */
			addWatch() {
				oWebSocket.sendData("watch", this.hosts);
			},
			addNewestData(oData) {
				let _this = this;

				for(var nId in oData) {
					let aoMonitorData = _this.monitorDatas[nId];

					if(!aoMonitorData) {
						aoMonitorData = [];
						_this.monitorDatas[nId] = aoMonitorData;
					}

					aoMonitorData.push(oData[nId]);

					if(aoMonitorData.length > 20) {
						aoMonitorData.shift();
					}
				}
			},

			/**
			 * 获取最新的一个数据项。
			 * 
			 * @param {Number} nid
			 * @return {Object}
			 */
			getNewestItemData(nId) {
				let aoMonitorData = this.monitorDatas[nId];

				if(aoMonitorData && aoMonitorData.length > 0) {
					return aoMonitorData[aoMonitorData.length - 1];
				} else {
					return {};
				}
			},
			/**
			 * 获取 CPU 的百分比。
			 * 
			 * @param {Number} nId
			 * @return {String}
			 */
			cpu(nId) {
				let anCpus = this.getNewestItemData(nId).cpus || [],
					nSize = anCpus.length || 1,
					nTotal = 0;

				anCpus.forEach(nValue => {
					nTotal += nValue;
				});

				return(nTotal / nSize * 100).toFixed(2);
			},
			/**
			 * 获取内存使用情况。
			 * 
			 * @param {Number} nId
			 * return {Object} used、total、available、usedPercent
			 */
			memory(nId) {
				let oMemory = this.getNewestItemData(nId).memory;
				if(!oMemory) {
					return {}
				}

				let nUsed = oMemory.total - oMemory.available;

				return {
					used: nUsed,
					total: oMemory.total,
					available: oMemory.available,
					usedPercent: (nUsed / oMemory.total * 100).toFixed(2)
				};
			},
			/**
			 * 获取最新的磁盘信息。
			 * 
			 * @param {Number} nId
			 * @return {Array} [{reads, writes}] 单位字节。
			 */
			disk(nId) {
				let aoMonitorData = this.monitorDatas[nId] || [];
				if(aoMonitorData.length < 2) {
					return {};
				}

				let oLastMonitorData = aoMonitorData[aoMonitorData.length - 1],
					oLastSecondMonitorData = aoMonitorData[aoMonitorData.length - 2],
					nTime = Math.round((oLastMonitorData.datetime - oLastSecondMonitorData.datetime) / 1000),
					aoLastData = oLastMonitorData.disk,
					aoLastSecondData = oLastSecondMonitorData.disk,
					aoSpeed = [];

				aoLastData.forEach((oItem, nIndex) => {
					aoSpeed.push({
						model: oItem.model,
						readSpeed: (oItem.readBytes - aoLastSecondData[nIndex].readBytes) / nTime,
						writeSpeed: (oItem.writeBytes - aoLastSecondData[nIndex].writeBytes) / nTime
					});
				});

				return aoSpeed;
			},
			/**
			 * 获取最新的网络数据。
			 * 
			 * @param {Number} nId
			 * @return {Array} [{byteSend, byteRecv}]
			 */
			network(nId) {
				let aoMonitorData = this.monitorDatas[nId] || [];
				if(aoMonitorData.length < 2) {
					return {};
				}

				let oLastMonitorData = aoMonitorData[aoMonitorData.length - 1],
					oLastSecondMonitorData = aoMonitorData[aoMonitorData.length - 2],
					nTime = Math.round((oLastMonitorData.datetime - oLastSecondMonitorData.datetime) / 1000),
					aoLastData = aoMonitorData[aoMonitorData.length - 1].network,
					aoLastSecondData = aoMonitorData[aoMonitorData.length - 2].network,
					aoSpeed = [];

				aoLastData.forEach((oItem, nIndex) => {
					let nUpSpeed = (oItem.bytesSend - aoLastSecondData[nIndex].bytesSend) / nTime,
						nDownSpeed = (oItem.bytesRecv - aoLastSecondData[nIndex].bytesRecv) / nTime;

					if(nUpSpeed && nDownSpeed) {
						// 仅添加有数据的网卡。
						aoSpeed.push({
							name: oItem.name,
							upSpeed: (oItem.bytesSend - aoLastSecondData[nIndex].bytesSend) / nTime,
							downSpeed: (oItem.bytesRecv - aoLastSecondData[nIndex].bytesRecv) / nTime
						});
					}
				});

				return aoSpeed;
			},
		}
	}
</script>

<style lang="less">
	.view-page {
		ul {
			padding: 0;
			margin: 0;
		}
		li {
			list-style: none;
		}
		.echarts {
			display: block;
			width: 100%;
			height: 300px;
			margin-bottom: 25px;
		}
		.host-item {
			margin-bottom: 70px;
		}
		.host-number {
			&:after {
				content: "\20";
				display: block;
				clear: both;
				height: 0;
				;
			}
			.number-item {
				float: left;
				width: 50%;
				list-style: none;
				margin: 0;
				.labelName {
					margin-right: 10px;
				}
			}
			.value {
				color: #f90;
			}
		}
		.subNumber-list {
			margin-left: 20px;
		}
		.subNumber-item {
			strong {
				display: block;
				font-weight: normal;
				white-space: nowrap;
				text-overflow: ellipsis;
				overflow: hidden;
				margin-right: 15px;
			}
			em {
				display: block;
				font-style: normal;
				margin-left: 20px;
			}
			i {
				font-style: normal;
				margin-left: 5px;
			}
		}
	}
</style>