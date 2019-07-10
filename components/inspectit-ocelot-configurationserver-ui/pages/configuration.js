import React from 'react'
import MainLayout from '../layout/MainLayout'
import Head from 'next/head'

import { BASE_PAGE_TITLE } from '../data/constants'

/**
 * The configuration page. Users can manage their configurations files on this page.
 */
class ConfigurationPage extends React.Component {

  render() {
    return (
      <div>
        <Head>
          <title>{BASE_PAGE_TITLE} | Configuration</title>
        </Head>
        
        <MainLayout>
          <a>hey</a>
        </MainLayout>
      </div>
    )
  }
}

export default ConfigurationPage;
