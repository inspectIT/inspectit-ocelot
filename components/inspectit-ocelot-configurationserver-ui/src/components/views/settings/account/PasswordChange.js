import React from 'react'
import SettingsElement from '../SettingsElement'
import {Password} from 'primereact/password'
import { Button } from 'primereact/button'
import { connect } from 'react-redux'
import {settingsActions} from '../../../../redux/ducks/settings'
import data from '../../../../data/settings-configuration.json'

const minPwLength = data.minPwLength

class PasswordChange extends React.Component{
  constructor(props){
    super(props)
    this.state = {
      currentPwErrorMsg: '',
      newPwErrorMsg: '',
      confirmPwErrorMsg: ''
    }
  }

  checkforAnyInput = () => {
    const {oldPassword, newPassword, newPasswordSecond} = this.state
    if(!oldPassword || !newPassword || !newPasswordSecond) {
      const currentPwErrorMsg = oldPassword ? '' : 'Please enter your old password'
      const newPwErrorMsg = newPassword ? '' : 'Please enter a new password'
      const confirmPwErrorMsg = newPasswordSecond ? '' : 'Please enter your new password again'
      this.setState({currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg})
      return true
    } else { return false }
  }

  checkforPwLength = () => {
    const { newPassword } = this.state
    if(newPassword.length < minPwLength) {
      const currentPwErrorMsg = ''
      const newPwErrorMsg = `Your new password must have at least ${minPwLength} characters`
      const confirmPwErrorMsg = ''
      this.setState({currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg})
      return true
    } else { return false }
  }

  checkIfNewPwDiffer = () => {
    const {oldPassword, newPassword} = this.state
    if(newPassword === oldPassword){
      const currentPwErrorMsg = ''
      const newPwErrorMsg = 'Your new password must differ from your old password'
      const confirmPwErrorMsg = ''
      this.setState({currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg})
      return true
    } else { return false }
  }

  checkforConfirmPw = () => {
    const {newPassword, newPasswordSecond} = this.state
    if(newPasswordSecond != newPassword) {
      const currentPwErrorMsg = ''
      const newPwErrorMsg = ''
      const confirmPwErrorMsg = 'Your confirmation password does not match your new password'
      this.setState({currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg})
      return true
    } else {
      return false
    }
  }

  changePassword = () => {
    const {oldPassword, newPassword, newPasswordSecond} = this.state
    const {loading, username} = this.props

    if(loading || this.checkforAnyInput() || this.checkforPwLength() || this.checkIfNewPwDiffer() || this.checkforConfirmPw()){
      return
    }

    const currentPwErrorMsg = ''
    const newPwErrorMsg = ''
    const confirmPwErrorMsg = ''
  
    this.props.changePassword(username, oldPassword, newPassword)
    this.setState({currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg, oldPassword: '', newPassword: '', newPasswordSecond: ''})
  }

  render() {
    const {currentPwErrorMsg, newPwErrorMsg, confirmPwErrorMsg, minPwLength} = this.state

    return(
      <SettingsElement title='Change Password' line>
        <div className='this'>
          <style jsx>{`
            .this{
              display: flex;
              flex-direction: column;
            }
            .row{
              display: flex;
              justify-content: space-between;
              align-items: center;
            }
            .row p {
              margin-right: 5rem;
            }
            .lastRow{
              display: flex;
              justify-content: end;
            }
            .errorMessage{
              font-size: 0.8rem;
              color: red;
              padding-left: 4rem;
            }
          `}</style>
          <div className='row'>
            <p>Current Password</p>
            <Password feedback={false} value={this.state.oldPassword} onChange={(e) => this.setState({oldPassword: e.target.value})} />
          </div>
          <div className='errorMessage'>{currentPwErrorMsg !== '' ? currentPwErrorMsg : ''}</div>
          <div className='row'>
            <p>New password</p>
            <Password feedback={false} tooltip={`Your password needs to have at least ${minPwLength} characters.`} value={this.state.newPassword} onChange={(e) => this.setState({newPassword: e.target.value})} />
          </div>
          <div className='errorMessage'>{newPwErrorMsg !== '' ? newPwErrorMsg : ''}</div>

          <div className='row'>
            
            <p>Confirm password</p>
            <Password feedback={false} value={this.state.newPasswordSecond} onChange={(e) => this.setState({newPasswordSecond: e.target.value})} />
          </div>
          <div className='errorMessage'>{confirmPwErrorMsg !== '' ? confirmPwErrorMsg : ''}</div>
          <div className='lastRow'>
            <Button label='Change' onClick={this.changePassword} />
          </div>
        </div>
      </SettingsElement>
    )
  }
}

function mapStateToProps(state) {
  const { loading } = state.settings
  const { username } = state.authentication
  return {
    loading,
    username
  }
}

const mapDispatchToProps = {
  changePassword: settingsActions.changePassword,
}

export default connect(mapStateToProps, mapDispatchToProps)(PasswordChange)