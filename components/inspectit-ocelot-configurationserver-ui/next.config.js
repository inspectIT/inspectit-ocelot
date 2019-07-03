const isProduction = process.env.NODE_ENV === 'production';

module.exports = {
    assetPrefix: isProduction ? '/ui' : '',

    // Will only be available on the server side
    serverRuntimeConfig: {
    },

    // Will be available on both server and client
    publicRuntimeConfig: {
        // used in '/components/basics/Link.js', for more details go to the component itself
        linkPrefix: isProduction ? '/next-hello-world' : ''
    }
}