import React from 'react';
import MainLayout from '../../layout/MainLayout';
import Head from 'next/head';

import { BASE_PAGE_TITLE } from '../../data/constants';
import SettingsView from '../../components/views/settings/SettingsView';

/** 
 * Placeholder example page for possible settings.
 */
class PlaceholderPage2 extends React.Component {

    render() {
        return (
            <MainLayout>
                <Head>
                    <title>{BASE_PAGE_TITLE} | Placeholder</title>
                </Head>

                <SettingsView>
                    <p style={{ margin: "0", padding: "0" }}>placeholder</p>
                </SettingsView>
            </MainLayout>
        )
    }
}

export default PlaceholderPage2;
