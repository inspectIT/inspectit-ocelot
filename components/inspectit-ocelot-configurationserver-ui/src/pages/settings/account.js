import React from 'react';
import MainLayout from '../../layout/MainLayout';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../../data/constants';
import SettingsView from '../../components/views/settings/SettingsView';
import AccountSettingsView from '../../components/views/settings/AccountSettingsView';

/**
 * The user account settings page.
 */
class AccountSettingsPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Account Settings</title>
        </Head>

        <SettingsView>
          <AccountSettingsView />
        </SettingsView>
      </MainLayout>
    );
  }
}

export default AccountSettingsPage;
