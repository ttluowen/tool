((function indexPage() {
	Vue.use(VueRouter);
	Vue.use(MuseUI);

	Vue.config.productionTip = false;

	const router = new VueRouter({
		routes: [{
				path: "/",
				redirect: localStorage.getItem(DIM.HOSTS_STORAGE_KEY) ? "/view" : "/config"
			},
			{
				path: "/config",
				component: Config
			},
			{
				path: "/view",
				component: View
			}
		]
	});


	new Vue({
		router,
		render: h => h(Home)
	}).$mount('#app')
}))();