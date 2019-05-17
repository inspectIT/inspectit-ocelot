import React from 'react'
import MainLayout from '../../layout/MainLayout'
import Head from 'next/head'
import { BASE_PAGE_TITLE } from '../../data/constants'
import SettingsLayout from '../../components/views/settings/layout/SettingsLayout';
import UserSettingsView from '../../components/views/settings/user/UserSettingsView';


class UserSettingsPage extends React.Component {

  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | User Settings</title>
        </Head>
        <SettingsLayout>
          <UserSettingsView/>
        </SettingsLayout>
      </MainLayout>
    )
  }
}

export default UserSettingsPage