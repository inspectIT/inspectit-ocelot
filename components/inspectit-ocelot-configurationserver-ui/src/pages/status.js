import React from 'react';
import MainLayout from '../layout/MainLayout';
import StatusView from '../components/views/status/StatusView';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../data/constants';

class StatusPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Agent Status</title>
        </Head>

        <StatusView />
      </MainLayout>
    );
  }
}

export default StatusPage;
