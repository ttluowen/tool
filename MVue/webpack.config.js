const path = require('path');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');

module.exports = {
	entry: {
		app: './src/main.js'
	},
	output: {
		filename: '[name].js?t=[hash]',
		path: path.resolve(__dirname, 'dist')
	},

	resolve: {
		extensions: ['.js', '.vue', '.json'],
		alias: {
			'vue$': 'vue/dist/vue.esm.js',
			'@': path.resolve(__dirname, 'src'),
			'src': path.resolve(__dirname, 'src'),
			'assets': path.resolve(__dirname, 'src/assets'),
			'components': path.resolve(__dirname, 'src/components'),
			'views': path.resolve(__dirname, 'src/views'),
			'utils': path.resolve(__dirname, 'src/utils'),
			'store': path.resolve(__dirname, 'src/store'),
			'router': path.resolve(__dirname, 'src/router'),
			'mock': path.resolve(__dirname, 'src/mock'),
			'vendor': path.resolve(__dirname, 'src/vendor'),
			'static': path.resolve(__dirname, 'static'),
			'templates': path.resolve(__dirname, 'src/templates'),
		}
	},

	devtool: 'inline-source-map',

	plugins: [
		new webpack.DefinePlugin({
			'process.env': {
				NODE_ENV: '"production"'
			}
		}),

		new CleanWebpackPlugin(['dist']),

		new HtmlWebpackPlugin({
			title: 'MVue',
			template: 'src/templates/index.html'
		})
	],

	module: {
		rules: [{
			test: /\.vue$/,
			loader: 'vue-loader',
		}, {
			test: /\.js$/,
			loader: 'babel-loader?cacheDirectory',
		}, {
			test: /\.css$/,
			use: ['style-loader', 'css-loader']
		}, {
			test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
			use: ['file-loader']
		}, {
			test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
			use: ['file-loader']
		}, {
			test: /\.less$/,
			loader: "style-loader!css-loader!less-loader",
		}]
	},
};