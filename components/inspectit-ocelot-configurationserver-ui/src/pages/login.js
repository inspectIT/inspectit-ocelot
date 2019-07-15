import React from 'react'
import Head from 'next/head'
import LoginView from '../components/views/LoginView'

import { BASE_PAGE_TITLE } from '../data/constants'

/**
 * The configuration page. Users can manage their configurations files on this page.
 */
class LoginPage extends React.Component {

  render() {
    return (
      <LoginView>
        <Head>
          <title>{BASE_PAGE_TITLE} | Login</title>
        </Head>
      </LoginView>
    )
  }
}

export default LoginPage;
