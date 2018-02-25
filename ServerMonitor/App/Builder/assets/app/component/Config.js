var Config = {
	template: "#Config-template",
	data() {
		return {
			openPopup: false,
			popupText: "",

			openDialog: false,
			dialogText: "",

			hosts: [
				/*
					{
						id, sign, name
					}
				 */
			],
		}
	},
	computed: {
		addDisabled() {
			return this.hosts.length >= DIM.MAX_SIZE;
		},
	},
	mounted() {
		this.hosts = JSON.parse(localStorage.getItem(DIM.HOSTS_STORAGE_KEY) || "[]");
	},
	methods: {
		add() {
			if(this.hosts.length < DIM.MAX_SIZE) {
				this.hosts.push({});
				setTimeout(function() {
					document.documentElement.scrollTop = document.documentElement.scrollHeight;
				}, 50);
			}
		},
		submit() {
			var bHasError = !!this.hosts.filter(oItem => {
				return !oItem.id || !oItem.sign || !oItem.name;
			}).length;

			if(bHasError) {
				this.dialogText = "还有未填完整的项";
				this.openDialog = true;

				return;
			}

			// 验证正确，保存操作。
			this.save();
		},
		remove(nIndex) {
			console.log(nIndex);
			this.hosts.splice(nIndex, 1);
		},

		closeDialog() {
			this.openDialog = false;
		},

		save() {
			localStorage.setItem(DIM.HOSTS_STORAGE_KEY, JSON.stringify(this.hosts));

			this.openPopup = true;
			this.popupText = "保存成功";

			setTimeout(() => {
				this.openPopup = false;
			}, 2 * 1000);
		}
	}
};