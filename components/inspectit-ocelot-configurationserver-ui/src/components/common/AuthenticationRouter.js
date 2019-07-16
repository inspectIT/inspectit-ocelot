import React from 'react'
import { connect } from 'react-redux'
import { authenticationSelectors } from '../../redux/ducks/authentication'
import Router, { withRouter } from 'next/router';
import { linkPrefix } from '../../lib/configuration';


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
                console.log("Forward to index.");
                Router.push(linkPrefix + "/");
            }
        } else {
            if (!pathname.endsWith("/login")) {
                console.log("Forward to login page.");
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