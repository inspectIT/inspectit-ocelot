import React from 'react'
import MainLayout from '../../layout/MainLayout'
import Head from 'next/head'
import { BASE_PAGE_TITLE } from '../../data/constants'

import SettingsLayout from '../../components/views/settings/layout/SettingsLayout'
import AccountSettingsView from '../../components/views/settings/account/AccountSettingsView'

class AccountSettingsPage extends React.Component {

  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Account Settings</title>
        </Head>
        <SettingsLayout>
          <AccountSettingsView />
        </SettingsLayout>
      </MainLayout>
    )
  }
}

export default AccountSettingsPage