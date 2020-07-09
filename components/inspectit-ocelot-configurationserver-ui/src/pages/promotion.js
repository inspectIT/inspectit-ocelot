import React from 'react';
import MainLayout from '../layout/MainLayout';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../data/constants';
import PromotionView from '../components/views/promotion/PromotionView';

class PromotionPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Configuration Promotion</title>
        </Head>

        <PromotionView />
      </MainLayout>
    );
  }
}

export default PromotionPage;
