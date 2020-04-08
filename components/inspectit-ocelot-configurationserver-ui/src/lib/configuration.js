import getConfig from 'next/config';

const { publicRuntimeConfig } = getConfig();
const linkPrefix = publicRuntimeConfig.linkPrefix;

export { linkPrefix };
