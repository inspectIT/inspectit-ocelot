import App from 'next/app';
import Head from 'next/head';
import React from 'react';
import withReduxStore from '../lib/with-redux-store';
import { persistStore } from 'redux-persist';
import { PersistGate } from 'redux-persist/integration/react';
import { Provider } from 'react-redux';
import AuthenticationRouter from '../components/common/AuthenticationRouter';
import NotificationHandler from '../components/common/NotificationHandler';
import UnsavedChangesGate from '../components/common/UnsavedChangesGate';
import { BASE_PAGE_TITLE } from '../data/constants';
import { linkPrefix } from '../lib/configuration';

// importing required css files for primereact
import 'primereact/resources/themes/nova-alt/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

class OcelotConfigurationUI extends App {
  constructor(props) {
    super(props);
    this.persistor = persistStore(props.reduxStore);
  }

  render() {
    const { Component, pageProps, reduxStore } = this.props;
    return (
      <React.Fragment>
        <style global jsx>
          {`
            .p-component {
              font-size: 14px;
              font-family: 'Open Sans', 'Helvetica Neue', sans-serif;
            }
            .p-button {
              font-size: 14px;
            }
            body {
              margin: 0;
              font-family: 'Open Sans', 'Helvetica Neue', sans-serif;
            }

            .pi {
              font-size: 1.2em;
            }
          `}
        </style>
        <Head>
          <meta charSet="utf-8" />
          <title>{BASE_PAGE_TITLE}</title>
          <link rel="shortcut icon" type="image/x-icon" href={linkPrefix + '/favicon.ico'} />
        </Head>
        <Provider store={reduxStore}>
          <PersistGate loading={null} persistor={this.persistor}>
            <NotificationHandler>
              <UnsavedChangesGate>
                <AuthenticationRouter>
                  <Component {...pageProps} />
                </AuthenticationRouter>
              </UnsavedChangesGate>
            </NotificationHandler>
          </PersistGate>
        </Provider>
      </React.Fragment>
    );
  }
}

export default withReduxStore(OcelotConfigurationUI);
