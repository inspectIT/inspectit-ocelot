import React from 'react';
import Head from 'next/head';

import LoginView from '../components/views/LoginView';

import { BASE_PAGE_TITLE } from '../data/constants';

/**
 * The login page.
 */
const LoginPage = () => {
  return (
    <LoginView>
      <Head>
        <title>{BASE_PAGE_TITLE} | Login</title>
      </Head>
    </LoginView>
  );
};

export default LoginPage;
