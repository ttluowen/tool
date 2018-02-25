var Home = {
	template: "#Home-template",
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
};