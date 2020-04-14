import React from 'react';
import MainLayout from '../layout/MainLayout';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../data/constants';
import AgentMappingsView from '../components/views/mappings/AgentMappingsView';

class MappingsPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Agent Mappings</title>
        </Head>

        <AgentMappingsView />
      </MainLayout>
    );
  }
}

export default MappingsPage;
