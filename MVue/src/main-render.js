import Vue from 'vue';
import axios from 'axios';
import App from './App';
import router from './router';

import 'material-design-icons/iconfont/material-icons.css';
import 'material-icons/css/material-icons.css';

import MuseUI from 'muse-ui';
import 'muse-ui/dist/muse-ui.css';
import 'muse-ui/dist/theme-light.css';


Vue.prototype.$http = axios;
Vue.use(MuseUI);


new Vue({
	el: '#app',
	router,
	template: '<App/>',
	render: h => h(App)
});
