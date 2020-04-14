import React from 'react';
import { connect } from 'react-redux';
import { authenticationActions } from '../../redux/ducks/authentication';
import Router from 'next/router';

import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { linkPrefix } from '../../lib/configuration';

/**
 * The application's menu bar.
 */
class Menubar extends React.Component {
  logout = () => {
    this.props.logout();
    Router.push(linkPrefix + '/login');
  };

  render() {
    return (
      <Toolbar id="toolbar">
        <style global jsx>{`
          #toolbar {
            height: 4rem;
            padding: 0.5rem;
            border-radius: 0;
            border: 0;
            border-bottom: 1px solid #ccc;
          }
        `}</style>
        <style jsx>{`
          .flex-v-center {
            display: flex;
            align-items: center;
            height: 3rem;
          }
          .ocelot-head {
            height: 3rem;
            margin-right: 1rem;
          }
          .ocelot-text {
            font-size: 1rem;
            font-weight: bold;
            color: #eee;
          }
          .user-description {
            color: #ccc;
            margin-right: 1rem;
          }
          .user-description b {
            color: #fff;
          }
        `}</style>
        <div className="p-toolbar-group-left flex-v-center">
          <img className="ocelot-head" src={linkPrefix + '/static/images/inspectit-ocelot-head.svg'} />
          <div className="ocelot-text">inspectIT Ocelot</div>
        </div>
        <div className="p-toolbar-group-right flex-v-center">
          <div className="user-description">
            Logged in as <b>{this.props.username}</b>
          </div>
          <Button label="Logout" onClick={this.logout} icon="pi pi-power-off" style={{ marginLeft: 4 }} />
        </div>
      </Toolbar>
    );
  }
}

function mapStateToProps(state) {
  const { username } = state.authentication;
  return {
    username,
  };
}

const mapDispatchToProps = {
  logout: authenticationActions.logout,
};

export default connect(mapStateToProps, mapDispatchToProps)(Menubar);
