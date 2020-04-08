import React from 'react';
import MainLayout from '../../layout/MainLayout';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../../data/constants';
import SettingsView from '../../components/views/settings/SettingsView';
import UserListView from '../../components/views/settings/userlist/UserListView';

/**
 * Page containing the user list.
 */
class UserListPage extends React.Component {
  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | User View</title>
        </Head>

        <SettingsView>
          <UserListView />
        </SettingsView>
      </MainLayout>
    );
  }
}

export default UserListPage;
