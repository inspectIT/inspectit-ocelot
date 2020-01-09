import React from 'react'
import Router from 'next/router'
import { linkPrefix } from '../lib/configuration';

/**
 * The landing page to user settings. This page will redirect the user to the account settings page.
 */
class UserSettings extends React.Component {

  componentDidMount() {
    Router.push(linkPrefix + "/usersettings/placeholder");
  }

  render() {
    return (
      <div />
    )
  }
}

export default UserSettings;
