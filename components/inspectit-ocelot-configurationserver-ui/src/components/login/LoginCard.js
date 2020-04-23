import React from 'react';
import { connect } from 'react-redux';
import { authenticationActions } from '../../redux/ducks/authentication';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';
import LoginCardHeader from './LoginCardHeader';

/**
 * The login card which wrapes and handles the interaction in order to log in into the application.
 */
class LoginCard extends React.Component {
  state = {
    username: '',
    password: '',
  };

  doLogin = () => {
    this.props.fetchToken(this.state.username, this.state.password);
  };

  onKeyPress = (e) => {
    if (this.canLogin() && e.key === 'Enter') {
      this.doLogin();
      e.target.blur();
    }
  };

  canLogin = () => {
    return !(this.state.username === '' || this.state.password === '' || this.props.loading);
  };

  render() {
    const fullWidthStyle = { width: '100%' };

    return (
      <div className="this">
        <style jsx>{`
          .this {
            width: 25rem;
            position: relative;
            box-shadow: none;
          }
          .input {
            margin-top: 1rem;
          }
          .pi-spinner {
            position: absolute;
            top: 0.5rem;
            right: 0.5rem;
          }
        `}</style>
        <LoginCardHeader />
        <div className="p-inputgroup input">
          <span className="p-inputgroup-addon">
            <i className="pi pi-user"></i>
          </span>
          <InputText
            placeholder="Username"
            style={fullWidthStyle}
            onKeyPress={this.onKeyPress}
            value={this.state.username}
            onChange={(e) => this.setState({ username: e.target.value })}
          />
        </div>
        <div className="p-inputgroup input">
          <span className="p-inputgroup-addon">
            <i className="pi pi-lock"></i>
          </span>
          <Password
            style={fullWidthStyle}
            placeholder="Password"
            feedback={false}
            onKeyPress={this.onKeyPress}
            value={this.state.password}
            onChange={(e) => this.setState({ password: e.target.value })}
          />
        </div>

        {this.props.error ? (
          <div className="input">
            <Message style={fullWidthStyle} severity="error" text={'Login failed: ' + this.props.error}></Message>
          </div>
        ) : null}

        <div className="input">
          <Button style={fullWidthStyle} onClick={this.doLogin} disabled={!this.canLogin()} label="Login" />
        </div>

        {this.props.loading && <i className="pi pi-spin pi-spinner"></i>}
      </div>
    );
  }
}

function mapStateToProps(state) {
  const { loading, error } = state.authentication;
  return {
    loading,
    error,
  };
}

const mapDispatchToProps = {
  fetchToken: authenticationActions.fetchToken,
};

export default connect(mapStateToProps, mapDispatchToProps)(LoginCard);
