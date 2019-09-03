import React from 'react'
import {DataView} from 'primereact/dataview'
import { Button } from 'primereact/button';
import {InputText} from 'primereact/inputtext'
import {Password} from 'primereact/password'
import DeleteDialog from './DeleteDialog'
import { connect } from 'react-redux'
import {settingsActions} from '../../../../redux/ducks/settings'
import { notificationActions } from '../../../../redux/ducks/notification'

class UserDataTable extends React.Component{
  constructor(props){
    super(props)
    this.state = {
      userToDelete: null, 
      newUser: {}
    }
  }

  componentDidMount(){
    this.props.getUsers()
  }

  startDeleteUserDialog = (user) => {this.setState({userToDelete: user})}
  stopDeleteUserDialog = () => {this.setState({userToDelete: null})}

  addUser = () => {
    const {newUser} = this.state
    if(!newUser.username || !newUser.password){
      this.props.showErrorMessage('Missing Input', 'username or password is missing')
      return
    }

    this.setState({newUser: {username: '', password: '', role: ''} })
    this.props.addUser(newUser)
  }

  templateForNewUserUsername = () => {
    const {newUser} = this.state
    return (
      <InputText
        value={newUser.username}
        keyfilter='alphanum'
        onChange={(e) => { this.setState({ newUser: { ...newUser, username: e.target.value } })
        }}
      />
    )
  }

  templateForNewUserPassword = () => {
    const {newUser} = this.state
    return (
      <Password 
        value={newUser.password} 
        feedback={false} 
        onChange={(e) => { this.setState({ newUser: { ...newUser, password: e.target.value } })
        }}
      />
    )
  }

  templateForNewUserButtonColumn = () => {
    return <Button icon='pi pi-plus' onClick={this.addUser}/>
  }

  itemTemplate = (user) => {
      return (
        <div className={`p-grid p-align-center ${user.isHeader ? 'header': ''}`}>
          <style jsx>{`
            .p-grid:after{
              content: "";
              display: block;
              border-radius: 25px;
              height: 0.1rem;
              background: #ddd;
              position: relative;
              width: 99%;
              top: 0.5rem;
              left: calc(1% / 2);
            }
            .header{
              font-weight: bold;
            }
            .p-grid{
              margin: 0.5rem;
            }
          `}</style>
          <div className='p-col-1'>{user.isHeader ? 'ID' : user.isAddUserMask? '' : user.id}</div>
          <div className='p-col'>
          {user.isHeader ? 'Username' : user.isAddUserMask ? this.templateForNewUserUsername() : `${user.username}`}
          </div>
          <div className='p-col'>
          {user.isHeader ? 'Password' : user.isAddUserMask ? this.templateForNewUserPassword() : `*****`}
          </div>
          <div className='p-col-1'>
            {user.isHeader ? '' : user.isAddUserMask ? this.templateForNewUserButtonColumn() : <Button icon='pi pi-trash' onClick={() => this.startDeleteUserDialog(user)} style={{marginRight:'.25em'}}/> }
          </div>
        </div>
      );
  }

  render() {
    const {filterValue} = this.props
    let filteredUsers = this.props.users.filter((user) => {
      if(user.isHeader || user.isAddUserMask || user.username || user.id){
        return user.isHeader || user.isAddUserMask || user.username.includes(filterValue) || user.id.toString().includes(filterValue)
      }
    })

    return(
      <div className='this'>
        <style jsx>{`
          .this{
            margin: 0.5rem;
            width: calc(100%-0.5rem);
          }
        `}</style>
        <DataView value={filteredUsers} itemTemplate={this.itemTemplate} style={{marginBottom: '2.5rem'}}></DataView>
        <DeleteDialog user={this.state.userToDelete ? this.state.userToDelete : {}} visible={this.state.userToDelete} onHide={this.stopDeleteUserDialog} />
      </div>
    )
  }
}
  
/** change incoming user array into required usage */ 
const prepareUsers = (users) => {
  let res = []
  res.push({ isHeader: true })
  res.push({ isAddUserMask: true })
  users.forEach(element => {
    res.push(element)
  });
  return res
}

function mapStateToProps(state) {
  const {users} = state.settings
  return {
    users: prepareUsers(users)
  }
}

const mapDispatchToProps = {
  getUsers: settingsActions.fetchUsers,
  addUser: settingsActions.addUser,
  showErrorMessage: notificationActions.showErrorMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(UserDataTable)