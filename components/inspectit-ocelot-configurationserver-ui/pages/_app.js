import App, { Container } from 'next/app'
import Head from 'next/head'
import React from 'react'
import withReduxStore from '../lib/with-redux-store'
import { Provider } from 'react-redux'

import {BASE_PAGE_TITLE} from '../data/constants'

// importing required css files for primereact
import 'primereact/resources/themes/nova-dark/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

class OcelotConfigurationUI extends App {
  render() {
    const { Component, pageProps, reduxStore } = this.props
    return (
      <Container>
        <Head>
            <meta charSet="utf-8" />
            <title>{BASE_PAGE_TITLE}</title>
        </Head>
        <Provider store={reduxStore}>
          <Component {...pageProps} />
        </Provider>
      </Container>
    )
  }
}

export default withReduxStore(OcelotConfigurationUI)
