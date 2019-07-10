const withCSS = require('@zeit/next-css')
const isProduction = process.env.NODE_ENV === 'production';

module.exports = withCSS({
    assetPrefix: isProduction ? '/ui' : '',

    // Will only be available on the server side
    serverRuntimeConfig: {
    },

    // Will be available on both server and client
    publicRuntimeConfig: {
        // used in '/components/basics/Link.js', for more details go to the component itself
        linkPrefix: isProduction ? '/ui' : ''
    },

    // Required for successfully importing CSS files (e.g. from PrimeReact)
    // See: https://github.com/zeit/next-plugins/issues/273#issuecomment-430597241
    webpack: function (config) {
        config.module.rules.push({
            test: /\.(eot|woff|woff2|ttf|svg|png|jpg|gif)$/,
            use: {
                loader: 'url-loader',
                options: {
                    limit: 100000,
                    name: '[name].[ext]'
                }
            }
        })
        return config
    }
})