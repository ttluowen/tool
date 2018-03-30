import Vue from 'vue';
import Vuex from 'vuex';
import app from './modules/app';
import user from './modules/user';
import page from './modules/page';
import list from './modules/list';
import permission from './modules/permission';
import getters from './getters';

Vue.use(Vuex);

const store = new Vuex.Store({
	modules: {
		app,
		user,
		permission,
		page,
		list
	},
	getters
});

export default store