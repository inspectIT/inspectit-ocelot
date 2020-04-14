import React from 'react';
import { connect } from 'react-redux';
import { settingsActions } from '../../../redux/ducks/settings';

import { Fieldset } from 'primereact/fieldset';
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

const initialState = {
  oldPassword: '',
  newPassword: '',
  repeatedPassword: '',
  error: null,
};

class AccountSettingsView extends React.Component {
  state = initialState;

  render() {
    return (
      <div>
        <style>{`
                    .centered {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding-bottom: 1rem;
                    }
                    .textDiv {
                        width: 150px;
                        text-align: end;
                        padding-right: 1rem;
                    }
                `}</style>
        <Fieldset legend="Change Your Password" style={{ marginLeft: '1rem', marginRight: '1rem' }}>
          <div className="centered">
            <div className="textDiv">Old Password:</div>
            <Password
              value={this.state.oldPassword}
              onChange={(e) => {
                this.setState({ oldPassword: e.target.value });
              }}
              feedback={false}
            />
          </div>
          <div className="centered">
            <div className="textDiv">New Password:</div>
            <Password
              value={this.state.newPassword}
              onChange={(e) => {
                this.setState({ newPassword: e.target.value });
              }}
              feedback={false}
            />
          </div>
          <div className="centered">
            <div className="textDiv">Repeat Password:</div>
            <Password
              value={this.state.repeatedPassword}
              onChange={(e) => {
                this.setState({ repeatedPassword: e.target.value });
              }}
              feedback={false}
            />
          </div>
          {this.state.error && (
            <div className="centered">
              <Message severity="error" text={this.state.error}></Message>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button label="Change" onClick={this.handleClick} />
          </div>
        </Fieldset>
      </div>
    );
  }

  handleClick = () => {
    const { oldPassword, newPassword, repeatedPassword } = this.state;

    let error = null;
    if (newPassword === repeatedPassword) {
      this.props.changePassword(this.props.username, oldPassword, newPassword);
      this.setState(initialState);
    } else {
      error = 'Your confirmation password does not match your new password';
    }

    this.setState({ error });
  };
}

function mapStateToProps(state) {
  const { username } = state.authentication;
  return {
    username,
  };
}

const mapDispatchToProps = {
  changePassword: settingsActions.changePassword,
};

export default connect(mapStateToProps, mapDispatchToProps)(AccountSettingsView);
