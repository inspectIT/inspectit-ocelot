import React from 'react'
import MainLayout from '../layout/MainLayout'
import Head from 'next/head'

import { BASE_PAGE_TITLE } from '../data/constants'

class MappingsPage extends React.Component {

  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Agent Mappings</title>
        </Head>

        
      </MainLayout>
    )
  }
}

export default MappingsPage;
