  import { login, logout, getInfo, getToDoList } from 'api/login'
  import Cookies from 'js-cookie'

  const user = {
    originData: {},
    state: {
    user: '',
    status: '',
    email: '',
    code: '',
    uid: undefined,
    auth_type: '',
    token: Cookies.get('Admin-Token'),
    name: '',
    avator: '',
    introduction: '',
    userOrganization: 0,
    // roles: [], 
    rights: [],
    role: 0,
    setting: {
      articlePlatform: []
    },
    // 通知。
    messages: [],
    // 待办。
    todos: []
  },

  mutations: {
    SET_ORIGIN_DATA: (state, oData) => {
      state.originData = oData;
    },
    SET_AUTH_TYPE: (state, type) => {
      state.auth_type = type;
    },
    SET_CODE: (state, code) => {
      state.code = code;
    },
    SET_TOKEN: (state, token) => {
      state.token = token;
    },
    SET_UID: (state, uid) => {
      state.uid = uid;
    },
    SET_EMAIL: (state, email) => {
      state.email = email;
    },
    SET_INTRODUCTION: (state, introduction) => {
      state.introduction = introduction;
    },
    SET_SETTING: (state, setting) => {
      state.setting = setting;
    },
    SET_STATUS: (state, status) => {
      state.status = status;
    },
    SET_NAME: (state, name) => {
      state.name = name;
    },
    SET_AVATOR: (state, avator) => {
      state.avator = avator;
    },
    // SET_ROLES: (state, roles) => {
    //   state.roles = roles;
    // },
    SET_RIGHTS: (state, rights) => {
      state.rights = rights
    },
    SET_ROLE: (state, role) => {
      state.role = role;
    },
    LOGIN_SUCCESS: () => {
      console.log('login success')
    },
    LOGOUT_USER: state => {
      state.user = '';
    },
    SET_MESSAGES: (state, messages) => {
      state.messages = messages
    },
    SET_TODOS: (state, todos) => {
      state.todos = todos
    },
    SET_USERORGANIZATION: (state, organization) => {
      state.userOrganization = organization
    }
  },

  actions: {
    // 用户登录
    LogIn({ commit }, userInfo) {
      const username = userInfo.username.trim();
      return new Promise((resolve, reject) => {
        login(username, userInfo.password).then(response => {
          const oResult = response.data
          if (oResult.c === 1) {
            const data = oResult.r
            //  请求成功。
            // commit('SET_NAME', data.username);
            // commit('SET_AVATAR', data.avatar);
            commit('SET_UID', data.userId);
            // commit('SET_NAME', data.name);
            // commit('SET_AVATOR', data.avator);

            Cookies.set('Admin-Token', data.userId); // 'admin'
            commit('SET_TOKEN', data.userId);
            resolve()
          } else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo(state.token).then(response => {
          const oResult = response.data
          if (oResult.c === 1) {
            const data = oResult.r
            // data.role = ['admin']
            commit('SET_ORIGIN_DATA', data);

            commit('SET_RIGHTS', data.right)
            commit('SET_NAME', data.name)
            commit('SET_AVATOR', data.avator)
            commit('SET_USERORGANIZATION', data.organization)
            commit('SET_ROLE', data.role);
            commit('SET_INTRODUCTION', data.institutions);
            // commit('SET_UID', data.uid);
            // commit('SET_INTRODUCTION', data.introduction);
            resolve(data);
          } else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 获取todo列表
    GetToDoList({ commit, state }) {
      return new Promise((resolve, reject) => {
        getToDoList(state.token).then(response => {
          const oResult = response.data
          if (oResult.c === 1) {
            const data = oResult.r
            // 是否需要审核。
            commit('SET_MESSAGES', data.filter(o => !o.isApproval));
            commit('SET_TODOS', data.filter(o => o.isApproval));
            resolve(data);
          } else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 第三方验证登录
    LoginByThirdparty({ commit, state }, code) {
      return new Promise((resolve, reject) => {
        commit('SET_CODE', code);
        loginByThirdparty(state.status, state.email, state.code, state.auth_type).then(response => {
          commit('SET_TOKEN', response.data.token);
          Cookies.set('Admin-Token', response.data.token);
          resolve();
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 登出
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_RIGHTS', [])
          commit('SET_NAME', '')
          commit('SET_AVATOR', '')
          commit('SET_USERORGANIZATION', 0)
          commit('SET_ROLE', 0)
          Cookies.remove('Admin-Token')
          resolve();
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '');
        Cookies.remove('Admin-Token');
        alert('has logout');
        resolve();
      });
    },

    // 动态修改权限
    ChangeRole({ commit }, role) {
      return new Promise(resolve => {
        // commit('SET_ROLES', [role]);
        
        commit('SET_TOKEN', role);
        Cookies.set('Admin-Token', role);
        resolve();
      })
    }
  }
  };

  export default user;
