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
      <div>
        <style jsx>{`
          .p-dir-col{
            margin: auto;
          }
          .p-col{
            padding-top: 0;
            padding-botom: 0;
          }
          .errorMessage{
            font-size: 0.8rem;
            color: red;
            padding-left: 6rem;
          }
        `}</style>
        <SettingsElement title='Change Password' line>
          <div className="p-grid p-dir-col">
            <div className="p-col">
              <div className='p-grid p-align-center'>
                <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>Current password</div>
                <div className='p-col-6'>
                  <Password feedback={false} value={this.state.oldPassword} onChange={(e) => this.setState({oldPassword: e.target.value})} />
                </div>
                <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                  {currentPwErrorMsg !== '' ? currentPwErrorMsg : ''}
                </div>
              </div>
            </div>
            <div className="p-col">
              <div className='p-grid p-align-center'>
                <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>New password</div>
                <div className='p-col-6'>
                  <Password feedback={false} tooltip={`Your password needs to have at least ${minPwLength} characters.`} value={this.state.newPassword} onChange={(e) => this.setState({newPassword: e.target.value})} />
                </div>
                <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                  {newPwErrorMsg !== '' ? newPwErrorMsg : ''}
                </div>
              </div>
            </div>
            <div className="p-col">
              <div className='p-grid p-align-center'>
                <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>Confirm password</div>
                <div className='p-col-6'>
                  <Password feedback={false} value={this.state.newPasswordSecond} onChange={(e) => this.setState({newPasswordSecond: e.target.value})} />
                </div>
                <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                  {confirmPwErrorMsg !== '' ? confirmPwErrorMsg : ''}
                </div>
                <div className='p-col p-offset-9 p-lg-offset-8 p-xl-offset-7'><Button label='Change' onClick={this.changePassword}/></div>
              </div>
            </div>
          </div>
        </SettingsElement>
      </div>
        
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