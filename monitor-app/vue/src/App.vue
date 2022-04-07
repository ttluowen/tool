<template>
	<div>
		<mu-appbar class="appbar" title="服务器监控">
			<mu-icon-menu icon="more_vert" slot="right">
				<mu-menu-item title="监控点" @click="menuLink(0)" />
				<mu-menu-item title="预览" @click="menuLink(1)" />
			</mu-icon-menu>
		</mu-appbar>

		<div class="view-body">
			<router-view />
		</div>
	</div>
</template>

<script>
	export default {
		methods: {
			menuLink(nIndex) {
				switch(nIndex) {
					case 0:
						this.$router.push("config");
						break;
					case 1:
						this.$router.push("view");
						break;
				}
			}
		}
	}
</script>

<style lang="less">
.appbar {
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
}
.view-body {
	padding: 20px;
	margin-top: 56px;
}
</style>
