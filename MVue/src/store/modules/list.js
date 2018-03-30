import { approvalResult, state, partyState, joinState, gender, nation, education, work, organizationCategory, organizationAffiliation, 
  unitCharacter, primaryOrganization, company, getChannel, 
  getOrganization, deleteOrganization, modifyOrganization, createOrganization } from 'api/list'
import {objectMerge} from 'utils/index.js'

const list = {
  state: {
    upload: "http://192.168.0.143:881/upload/",
    partyState,
    joinState,
    gender,
    nation,
    education,
    work,
    organizationCategory,
    organizationAffiliation,
    unitCharacter,
    primaryOrganization,
    company,
    approvalResult,
    state,
    channel: [],
    organization: [],
    fetchedOrganization: false,
    fetchedChannel: false
  },

  mutations: {
    SET_CHANNEL: (state, channel) => {
      state.channel = channel.map(o => {
        o.label = o.name
        o.value = o.id
        return o
      })
    },
    SET_ORGANIZATION: (state, organization) => {
      state.organization = organization.map(o => {
        o.label = o.name
        o.value = o.id
        return o
      })
    }
  },

  actions: {
    // 获取频道数据。
    GetChannel({commit, state}) {
      if(state.fetchedChannel){
        return 
      }
      return new Promise((resolve, reject) => {
        state.fetchedChannel = true
        getChannel().then(response => {
          const oResult = response.data
          if(oResult.c === 1) {
            let data = oResult.r
            //  请求成功。 
            commit('SET_CHANNEL', data)
            resolve()
          }else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error)
        });
      });
    },
    // 获取组织结构（支部）数据。
    GetOrganization({commit, state}) {
      if(state.fetchedOrganization){
        return 
      }
      return new Promise((resolve, reject) => {
        state.fetchedOrganization = true
        getOrganization().then(response => {
          const oResult = response.data
          if(oResult.c === 1) {
            let data = oResult.r.map(o => {
              if(!o.name) {
                o.name = o.shortname
              }
              return o
            })
            //  请求成功。 
            commit('SET_ORGANIZATION', data)
            resolve()
          }else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error)
        });
      });
    },
    // 创建组织结构
    CreateOrganization({commit, state}, data) {
      return new Promise((resolve, reject) => {
        createOrganization(data).then(response => {
          const oResult = response.data
          if(oResult.c === 1) {
            //  请求成功。 
            // 返回ID
            data.id = oResult.r
            let aoData = objectMerge([], state.organization)
            aoData.push(data)
            commit('SET_ORGANIZATION', aoData)
            resolve()
          }else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error)
        });
      });
    },
    // 删除组织结构
    DeleteOrganization({commit, state}, id) {
      return new Promise((resolve, reject) => {
        deleteOrganization(id).then(response => {
          const oResult = response.data
          if(oResult.c === 1) {
            //  请求成功。 
            let aoData = objectMerge(true, [], state.organization).filter(o => id.indexOf(o.id) < 0)

            commit('SET_ORGANIZATION', aoData)
            resolve()
          }else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error)
        });
      });
    },
    //  修改组织结构
    ModifyOrganization({commit, state}, data) {
      return new Promise((resolve, reject) => {
        modifyOrganization(data).then(response => {
          const oResult = response.data

          let aoData = objectMerge(true, [], state.organization)
          aoData[aoData.findIndex(o => o.id === data.id)] = data

          if(oResult.c === 1) {
            //  请求成功。 
            commit('SET_ORGANIZATION', aoData)
            resolve()
          }else {
            reject(oResult.d)
          }
        }).catch(error => {
          reject(error)
        });
      });
    },
  }
};

export default list
