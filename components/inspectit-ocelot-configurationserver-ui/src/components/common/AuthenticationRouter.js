import React from 'react'
import { connect } from 'react-redux'
import { authenticationSelectors } from '../../redux/ducks/authentication'
import Router, { withRouter } from 'next/router';
import { linkPrefix } from '../../lib/configuration';

/**
 * Handles the routing based on the current authentication (user is logged in/out) status.
 */
class AuthenticationRouter extends React.Component {

    componentDidMount = () => {
        this.checkRoute();
    }

    componentDidUpdate = () => {
        this.checkRoute();
    }

    checkRoute = () => {
        const { pathname } = this.props.router;

        if (this.props.isAuthenticated) {
            if (pathname.endsWith("/login")) {
                Router.push(linkPrefix + "/");
            }
        } else {
            if (!pathname.endsWith("/login")) {
                Router.push(linkPrefix + "/login");
            }
        }
    }

    render() {
        return (
            <>
                {this.props.children}
            </>
        );
    }
}

function mapStateToProps(state) {
    return {
        isAuthenticated: authenticationSelectors.isAuthenticated(state)
    }
}

export default withRouter(connect(mapStateToProps, null)(AuthenticationRouter));