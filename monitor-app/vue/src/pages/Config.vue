<template>
	<div class="config-page">
		<div class="host-add">
			<mu-raised-button label="新 增" :disabled="addDisabled" @click="add" icon="add" primary />
		</div>

		<div class="host-item" v-for="(item, index) in hosts">
			<div class="index">
				<mu-icon :value="'filter_' + (index + 1)" />
			</div>
			<div class="fields">
				<mu-text-field hintText="ID" icon="turned_in_not" v-model="item.id" type="number" :maxLength="10" />
				<mu-text-field hintText="SIGN" icon="web_asset" v-model="item.sign" :maxLength="16" />
				<mu-text-field hintText="NAME" icon="assignment_ind" v-model="item.name" :maxLength="20" />
			</div>
			<div class="edit">
				<mu-icon value="clear" @click="remove(index)" />
			</div>
		</div>

		<div class="host-submit">
			<mu-raised-button label="提 交" @click="submit" icon="check" primary fullWidth />
		</div>

		<mu-popup position="bottom" :overlay="false" popupClass="popup-top" :open="openPopup">{{popupText}}</mu-popup>

		<mu-dialog :open="openDialog">
			{{dialogText}}
			<mu-flat-button label="确定" slot="actions" primary @click="closeDialog" />
		</mu-dialog>
	</div>
</template>

<script>
	export default {
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
	}
</script>

<style lang="less">
	.mu-text-field {
		width: auto;
	}
	
	.config-page {
		.host-item {
			font-size: 0;
			white-space: nowrap;
			overflow: hidden;
			padding-bottom: 20px;
			border-bottom: 1px dashed #dfdfdf;
			margin-bottom: 25px;
			.index,
			.fields,
			.edit {
				display: inline-block;
				vertical-align: middle;
				font-size: 1.5rem;
				white-space: normal;
			}
			.index,
			.edit {
				width: 15%;
				overflow: hidden;
			}
			.index {
				text-align: center;
				color: #009688;
			}
			.fields {
				width: 70%;
				overflow: hidden;
			}
			.edit {
				text-align: right;
			}
		}
		.host-add {
			text-align: right;
		}
		.popup-top {
			color: #fff;
			width: 50%;
			height: 48px;
			line-height: 48px;
			display: flex;
			align-items: center;
			justify-content: center;
			max-width: 375px;
			background-color: rgba(0, 0, 0, .8);
		}
	}
</style>