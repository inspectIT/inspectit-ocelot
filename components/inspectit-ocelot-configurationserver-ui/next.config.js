const isProduction = process.env.NODE_ENV === 'production';

const nextConfig = {
  output: 'export',
  distDir: './out',

  // Each page will be exported as a directory
  trailingSlash: true,

  assetPrefix: isProduction ? '/ui' : '/',

  // Will only be available on the server side
  serverRuntimeConfig: {},

  // Will be available on both server and client
  publicRuntimeConfig: {
    // used in '/components/basics/Link.js', for more details go to the component itself
    linkPrefix: isProduction ? '/ui' : '',
  },

  env: {
    VERSION: '2.7.0',
    BUILD_DATE: new Date().toUTCString(),
  },
};

module.exports = nextConfig;
