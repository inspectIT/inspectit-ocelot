import React from 'react';
import Router from 'next/router';
import { linkPrefix } from '../lib/configuration';

import tabMenuItems from '../data/settings-navigation-items.json';

/**
 * The landing page to settings. This page will redirect the user to the first link in tabMenuItems.
 */
class SettingsPage extends React.Component {
  componentDidMount() {
    Router.push(linkPrefix + tabMenuItems[0].href);
  }

  render() {
    return <div />;
  }
}

export default SettingsPage;
