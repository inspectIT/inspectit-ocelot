import getConfig from 'next-server/config';

const { publicRuntimeConfig } = getConfig();
const linkPrefix = publicRuntimeConfig.linkPrefix;

export {
    linkPrefix
};