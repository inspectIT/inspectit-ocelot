import React from 'react';
import { connect } from 'react-redux';
import { authenticationSelectors, authenticationActions } from '../../redux/ducks/authentication';
import Router, { withRouter } from 'next/router';
import { linkPrefix } from '../../lib/configuration';
import { RENEW_TOKEN_TIME_INTERVAL, MIN_TOKEN_EXPIRATION_TIME } from '../../data/constants';

/**
 * Handles the routing based on the current authentication (user is logged in/out) status.
 */
class AuthenticationRouter extends React.Component {
  componentDidMount = () => {
    this.checkRoute();
    this.intervalId = setInterval(this.triggerRenewTokenInterval, RENEW_TOKEN_TIME_INTERVAL);
  };

  componentDidUpdate = () => {
    this.checkRoute();
  };

  componentWillUnmount = () => {
    clearInterval(this.intervalId);
  };

  checkRoute = () => {
    const { pathname } = this.props.router;

    if (this.props.isAuthenticated) {
      if (pathname.endsWith('/login')) {
        Router.push(linkPrefix + '/');
      }
    } else {
      if (!pathname.endsWith('/login')) {
        Router.push(linkPrefix + '/login');
      }
    }
  };

  triggerRenewTokenInterval = () => {
    if (this.props.isAuthenticated) {
      const isSoonExpired = this.props.tokenExpirationDate * 1000 - Date.now() < MIN_TOKEN_EXPIRATION_TIME;
      if (isSoonExpired) {
        this.props.renewToken();
      }
    }
  };

  render() {
    return <>{this.props.children}</>;
  }
}

function mapStateToProps(state) {
  return {
    isAuthenticated: authenticationSelectors.isAuthenticated(state),
    tokenExpirationDate: authenticationSelectors.getTokenExpirationDate(state),
  };
}

const mapDispatchToProps = {
  renewToken: authenticationActions.renewToken,
};

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(AuthenticationRouter));
