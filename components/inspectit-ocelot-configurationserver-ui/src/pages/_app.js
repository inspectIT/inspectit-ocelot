import App, { Container } from 'next/app'
import Head from 'next/head'
import React from 'react'
import withReduxStore from '../lib/with-redux-store'
import { persistStore } from 'redux-persist'
import { PersistGate } from 'redux-persist/integration/react'
import { Provider } from 'react-redux'
import AuthenticationRouter from '../components/common/AuthenticationRouter';
import NotificationHandler from '../components/common/NotificationHandler';

import { BASE_PAGE_TITLE } from '../data/constants'
import { linkPrefix } from '../lib/configuration';

// importing required css files for primereact
import 'primereact/resources/themes/nova-dark/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

class OcelotConfigurationUI extends App {

  constructor(props) {
    super(props)
    this.persistor = persistStore(props.reduxStore)
  }

  render() {
    const { Component, pageProps, reduxStore } = this.props
    return (
      <Container>
        <style global jsx>{`
        body {
          margin: 0;
          font-family: "Open Sans", "Helvetica Neue", sans-serif;
        }
        `}</style>
        <Head>
          <meta charSet="utf-8" />
          <title>{BASE_PAGE_TITLE}</title>
          <link rel="shortcut icon" type="image/x-icon" href={linkPrefix + "/static/favicon.ico"} />
        </Head>
        <Provider store={reduxStore}>
          <PersistGate loading={null} persistor={this.persistor}>
            <NotificationHandler>
              <AuthenticationRouter>
                <Component {...pageProps} />
              </AuthenticationRouter>
            </NotificationHandler>
          </PersistGate>
        </Provider>
      </Container >
    )
  }
}

export default withReduxStore(OcelotConfigurationUI)
