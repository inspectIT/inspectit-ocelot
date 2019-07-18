import React from 'react'
import Head from 'next/head'
import { connect } from 'react-redux'
import { notificationActions } from '../redux/ducks/notification'

import LoginView from '../components/views/LoginView'

import { BASE_PAGE_TITLE } from '../data/constants'

/**
 * The login page.
 */
class LoginPage extends React.Component {

  /**
   * A warning notification will be shown in case an unauthorized request was made.
   */
  componentDidMount = () => {
    if (this.props.unauthorized) {
      this.props.showWarningMessage("Unauthorized", "Your access token is no longer valid. Please login again.");
    }
  }

  render() {
    return (
      <LoginView>
        <Head>
          <title>{BASE_PAGE_TITLE} | Login</title>
        </Head>
      </LoginView>
    )
  }
}

function mapStateToProps(state) {
  const { unauthorized } = state.authentication;
  return {
    unauthorized
  }
}

const mapDispatchToProps = {
  showWarningMessage: notificationActions.showWarningMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(LoginPage);
