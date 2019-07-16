import React from 'react'
import Router from 'next/router'
import { connect } from 'react-redux'
import { linkPrefix } from '../lib/configuration';
import { authenticationSelectors } from '../redux/ducks/authentication'



/**
 * The index page. This page will redirect the user to the login page or to the applications "home" page.
 */
class IndexPage extends React.Component {

  componentDidMount() {
    if (this.props.isAuthenticated) {
      Router.push(linkPrefix + "/configuration");
    } else {
      Router.push(linkPrefix + "/login");
    }
  }

  render() {
    return (
      <div />
    )
  }
}

function mapStateToProps(state) {
  return {
    isAuthenticated: authenticationSelectors.isAuthenticated(state)
  }
}

export default connect(mapStateToProps, null)(IndexPage);
