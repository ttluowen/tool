import Vue from 'vue'
import VueRouter from 'vue-router'

import 'material-design-icons/iconfont/material-icons.css';
import 'material-icons/css/material-icons.css';

import './assets/dim.js';
import './assets/util.min.js';
import './assets/webSocket.min.js';

import MuseUI from 'muse-ui'
import 'muse-ui/dist/muse-ui.css'
import 'muse-ui/dist/theme-teal.css'

import './assets/base.css';

import App from './App.vue'
import Config from './pages/Config.vue';
import View from './pages/View.vue';

Vue.use(VueRouter);
Vue.use(MuseUI);

Vue.config.productionTip = false;

const router = new VueRouter({
	routes: [
		{path: "/", redirect: localStorage.getItem(DIM.HOSTS_STORAGE_KEY) ? "/view" : "/config"},
		{path: "/config", component: Config},
		{path: "/view", component: View}
	]
});


new Vue({
	router,
	render: h => h(App)
}).$mount('#app')