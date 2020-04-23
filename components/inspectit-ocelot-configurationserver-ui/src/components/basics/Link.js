/**
 * Higher order component, which is based on 'next/link'
 * When app is deployed on github pages repository which is not the main one
 * e.g https://username.github.io/repository/
 * standard 'next/link' is not wokring properly, it doesn't add the 'reporsitory'
 * to url so instead of expected 'https://username.github.io/repository/about',
 * it would be 'https://username.github.io/about/
 * This HOC solves the problem, by adding the prefix (which is defined in next.config.js)
 * to 'as' property.
 *
 * This component is based on the work of dmitriyaa (https://github.com/zeit/next.js/issues/3335#issuecomment-489354854).
 */
import React from 'react';
import getConfig from 'next/config';
import Link from 'next/link';

const { publicRuntimeConfig } = getConfig();
const linkPrefix = publicRuntimeConfig.linkPrefix;

const PrefixedLink = ({ href, as = href, children }) => (
  <Link href={href} as={`${linkPrefix}${as}`}>
    {children}
  </Link>
);

export default PrefixedLink;
