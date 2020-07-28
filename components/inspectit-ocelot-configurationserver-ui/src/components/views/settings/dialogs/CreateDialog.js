import React from 'react';
import { connect } from 'react-redux';

import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';
import { Password } from 'primereact/password';

import { settingsActions } from '../../../../redux/ducks/settings';

const initialState = {
  error: null,
  username: '',
  password: '',
};

/**
 * Dialog for creating a new user.
 */
class CreateDialog extends React.Component {
  input = React.createRef();

  state = initialState;

  render() {
    return (
      <Dialog
        header={'Create User'}
        focusOnShow={false}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        style={{ width: '400px' }}
        footer={
          <div>
            <Button label="Create" disabled={!this.canCreateUser()} className="p-button-primary" onClick={this.handleCreate} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        <div style={{ width: '100%', paddingBottom: '0.5em', borderBottom: 'solid #bbb 1px' }}>
          Users created here will not be registered in LDAP
        </div>
        <div style={{ width: '100%', paddingBottom: '0.5em', paddingTop: '0.5em' }}>New Username:</div>
        <InputText
          ref={this.input}
          style={{ width: '100%' }}
          onKeyPress={this.onKeyPress}
          value={this.state.username}
          placeholder={'username'}
          onChange={(e) => this.handleUsernameChanged(e.target.value)}
        />
        {this.state.error && (
          <div style={{ width: '100%', paddingBottom: '0.5em', paddingTop: '0.5em' }}>
            <Message style={{ width: '100%' }} severity="error" text={this.state.error} />
          </div>
        )}
        <div style={{ width: '100%', paddingBottom: '0.5em', paddingTop: '0.5em' }}>New Password:</div>
        <Password
          style={{ width: '100%' }}
          feedback={false}
          onKeyPress={this.onKeyPress}
          value={this.state.password}
          placeholder={'password'}
          onChange={(e) => this.setState({ password: e.target.value })}
        />
      </Dialog>
    );
  }

  handleUsernameChanged = (name) => {
    let error = null;
    const existingUser = this.props.users.find((user) => user.username === name);

    if (existingUser) {
      error = 'A user with this name already exists';
    }

    this.setState({
      username: name,
      error: error,
    });
  };

  handleCreate = () => {
    this.props.addUser({ username: this.state.username, password: this.state.password });
    this.props.onHide();
  };

  onKeyPress = (e) => {
    if (e.key === 'Enter' && this.canCreateUser()) {
      this.handleCreate();
    }
  };

  canCreateUser = () => {
    return !this.state.error && this.state.username && this.state.password;
  };

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      /**Timeout is needed for .focus() to be triggered correctly. */
      setTimeout(() => {
        this.input.current.element.focus();
      }, 0);
      this.setState({ ...initialState });
    }
  }
}

function mapStateToProps(state) {
  const { users } = state.settings;
  return {
    users,
  };
}

const mapDispatchToProps = {
  addUser: settingsActions.addUser,
};

export default connect(mapStateToProps, mapDispatchToProps)(CreateDialog);
