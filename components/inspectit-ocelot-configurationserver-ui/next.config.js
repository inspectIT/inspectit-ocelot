const isProduction = process.env.NODE_ENV === 'production';

module.exports = {
  distDir: '../.next',

  // Each page will be exported as a directory
  trailingSlash: true,

  assetPrefix: isProduction ? '/ui' : '',

  // Will only be available on the server side
  serverRuntimeConfig: {},

  // Will be available on both server and client
  publicRuntimeConfig: {
    // used in '/components/basics/Link.js', for more details go to the component itself
    linkPrefix: isProduction ? '/ui' : '',
  },

  env: {
    VERSION: process.env.GITHUB_REF_NAME || 'SNAPSHOT',
    BUILD_DATE: new Date().toUTCString(),
  },
};
