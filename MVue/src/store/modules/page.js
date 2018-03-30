const page = {
    state: {
        mode: "",
        article: {},
        // tableData: []
    },
    mutations: {
        CHANGE_MODE: (state, view) => {
            state.mode = view
        },
        SET_ARTICLE: (state, view) => {
            state.article = view
        },
        // SET_TABLE: (state, table) => {
        //     state.tableData = table
        // }
    },
    actions: {
        changeMode: ({ commit }, view) => {
            commit('CHANGE_MODE', view)
        },
        setArticle: ({ commit }, view) => {
            commit('SET_ARTICLE', view)
        },
        // setTableData: ({ commit }, table) => {
        //     commit('SET_TABLE', table)
        // },
  }
};

export default page;
