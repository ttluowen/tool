<style lang="less">
.model-1 {
	.model-title {
		text-align: center;
		font-size: 24px;;
		margin: 10px 0;
	}
	.model-content {
		padding: 10px;
		border: 1px solid #d1d1d1;
		margin: 20px;
	}
}
</style>

<template>
	<div>
		<mu-appbar title="Title"></mu-appbar>

		<div class="model-1">
			<div class="model-title">销售走势</div>
			<div class="model-content">
				<div id="J_Chart1" style="height: 200px;"></div>
			</div>
		</div>

		<div class="model-1">
			<div class="model-title">销售汇总</div>
			<div class="model-content">
				<div id="J_Chart2" style="height: 200px;"></div>
			</div>
		</div>

		<div class="model-1">
			<div class="model-title">毛利</div>
			<div class="model-content">
				<div id="J_Chart3" style="height: 200px;"></div>
			</div>
		</div>
	</div>
</template>

<script>
	import echarts from 'echarts';


	export default {
		name: 'home',
		data() {
			return {
			}
		},
		mounted() {
			plus.device.beep();
			plus.device.vibrate();

			this.chart1();
		},
		methods: {
			chart1() {
				echarts.init(document.querySelector("#J_Chart1")).setOption({
					color: ['#3398DB'],
					tooltip: {
						trigger: 'axis',
						axisPointer: { // 坐标轴指示器，坐标轴触发有效
							type: 'shadow' // 默认为直线，可选为：'line' | 'shadow'
						}
					},
					grid: {
						left: '3%',
						right: '4%',
						bottom: '3%',
						containLabel: true
					},
					xAxis: [{
						type: 'category',
						data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
						axisTick: {
							alignWithLabel: true
						}
					}],
					yAxis: [{
						type: 'value'
					}],
					series: [{
						name: '直接访问',
						type: 'bar',
						barWidth: '60%',
						data: [10, 52, 200, 334, 390, 330, 220]
					}]
				});
			},
			chart2() {
				echarts.init(document.querySelector("#J_Chart2")).setOption({
				    title : {
				        text: '某站点用户访问来源',
				        subtext: '纯属虚构',
				        x:'center'
				    },
				    tooltip : {
				        trigger: 'item',
				        formatter: "{a} <br/>{b} : {c} ({d}%)"
				    },
				    legend: {
				        orient: 'vertical',
				        left: 'left',
				        data: ['直接访问','邮件营销','联盟广告','视频广告','搜索引擎']
				    },
				    series : [
				        {
				            name: '访问来源',
				            type: 'pie',
				            radius : '55%',
				            center: ['50%', '60%'],
				            data:[
				                {value:335, name:'直接访问'},
				                {value:310, name:'邮件营销'},
				                {value:234, name:'联盟广告'},
				                {value:135, name:'视频广告'},
				                {value:1548, name:'搜索引擎'}
				            ],
				            itemStyle: {
				                emphasis: {
				                    shadowBlur: 10,
				                    shadowOffsetX: 0,
				                    shadowColor: 'rgba(0, 0, 0, 0.5)'
				                }
				            }
				        }
				    ]
				});
			},
			chart3() {
				echarts.init(document.querySelector("#J_Chart3")).setOption({
				    tooltip : {
				        trigger: 'axis',
				        axisPointer : {            // 坐标轴指示器，坐标轴触发有效
				            type : 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
				        }
				    },
				    legend: {
				        data: ['直接访问', '邮件营销','联盟广告','视频广告','搜索引擎']
				    },
				    grid: {
				        left: '3%',
				        right: '4%',
				        bottom: '3%',
				        containLabel: true
				    },
				    xAxis:  {
				        type: 'value'
				    },
				    yAxis: {
				        type: 'category',
				        data: ['周一','周二','周三','周四','周五','周六','周日']
				    },
				    series: [
				        {
				            name: '直接访问',
				            type: 'bar',
				            stack: '总量',
				            label: {
				                normal: {
				                    show: true,
				                    position: 'insideRight'
				                }
				            },
				            data: [320, 302, 301, 334, 390, 330, 320]
				        },
				        {
				            name: '邮件营销',
				            type: 'bar',
				            stack: '总量',
				            label: {
				                normal: {
				                    show: true,
				                    position: 'insideRight'
				                }
				            },
				            data: [120, 132, 101, 134, 90, 230, 210]
				        },
				        {
				            name: '联盟广告',
				            type: 'bar',
				            stack: '总量',
				            label: {
				                normal: {
				                    show: true,
				                    position: 'insideRight'
				                }
				            },
				            data: [220, 182, 191, 234, 290, 330, 310]
				        },
				        {
				            name: '视频广告',
				            type: 'bar',
				            stack: '总量',
				            label: {
				                normal: {
				                    show: true,
				                    position: 'insideRight'
				                }
				            },
				            data: [150, 212, 201, 154, 190, 330, 410]
				        },
				        {
				            name: '搜索引擎',
				            type: 'bar',
				            stack: '总量',
				            label: {
				                normal: {
				                    show: true,
				                    position: 'insideRight'
				                }
				            },
				            data: [820, 832, 901, 934, 1290, 1330, 1320]
				        }
				    ]
				});
			}
		}
	}
</script>

<style>

</style>