import React from 'react'
import MainLayout from '../../layout/MainLayout'
import Head from 'next/head'

import { BASE_PAGE_TITLE } from '../../data/constants'
import UserSettingsView from '../../components/views/userSettings/UserSettingsView'

/** placeholder example page for possible user settings */
class MappingsPage extends React.Component {

  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Placeholder</title>
        </Head>

        <UserSettingsView>
          <p style={{ margin: "0", padding: "0" }}>placeholder</p>
        </UserSettingsView>
      </MainLayout>
    )
  }
}

export default MappingsPage;
