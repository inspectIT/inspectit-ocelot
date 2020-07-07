import React from 'react';
import MainLayout from '../layout/MainLayout';
import Head from 'next/head';
import AlertingView from '../components/views/alerting/AlertingView';

import { BASE_PAGE_TITLE } from '../data/constants';

/**
 * The alerting page. Users can manage their alerting rules on this page.
 */
class AlertingPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Alerting Configuration</title>
        </Head>

        <AlertingView />
      </MainLayout>
    );
  }
}

export default AlertingPage;
