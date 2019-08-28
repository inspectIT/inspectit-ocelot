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
      isDeleteUserDialogShown: false,
      userToDelete: -1, 
      newUser: {}
    }
  }

  componentDidMount(){
    this.props.getUsers()
  }

  startDeleteUserDialog = (user) => {this.setState({isDeleteUserDialogShown: true, userToDelete: user})}
  stopDeleteUserDialog = () => {this.setState({isDeleteUserDialogShown: false, userToDelete: -1})}

  addUser = () => {
    const {newUser} = this.state
    if(!newUser.username || newUser.username === '' || !newUser.password || newUser.password === ''){
      this.props.showErrorMessage('Missing Input', 'username or password is missing')
      return
    }

    this.setState({newUser: {username: '', password: '', role: ''} })
    this.props.addUser(newUser)
  }

  templateAdditionNewUser = (string) => {
    const {newUser} = this.state
    switch(string){
      case 'usernameColumn':
        return (
          <InputText
            value={newUser.username}
            keyfilter='alphanum'
            onChange={(e) => { this.setState({ newUser: { ...newUser, username: e.target.value } })
            }}
          />
        )
      case 'passwordColumn':
        return (
          <Password 
            value={newUser.password} 
            feedback={false} 
            onChange={(e) => { this.setState({ newUser: { ...newUser, password: e.target.value } })
            }}
          />
        )
      case 'buttonColumn':
        return <Button icon='pi pi-plus' onClick={this.addUser}/>
    }
  }

  itemTemplate = (user) => {
      return (
        <div className={`p-grid p-align-center ${user.head ? 'head': ''}`}>
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
            .head{
              font-weight: bold;
            }
            .p-grid{
              margin: 0.5rem;
            }
          `}</style>
          <div className='p-col-1'>{user.head ? 'ID' : user.newUser? '' : user.id}</div>
          <div className='p-col'>
          {user.head ? 'Username' : user.newUser ? this.templateAdditionNewUser('usernameColumn') : `${user.username}`}
          </div>
          <div className='p-col'>
          {user.head ? 'Password' : user.newUser ? this.templateAdditionNewUser('passwordColumn') : `*****`}
          </div>
          <div className='p-col-1'>
            {user.head ? '' : user.newUser ? this.templateAdditionNewUser('buttonColumn') : <Button icon='pi pi-trash' onClick={() => this.startDeleteUserDialog(user)} style={{marginRight:'.25em'}}/> }
          </div>
        </div>
      );
  }

  render() {
    const {filterValue} = this.props
    let filteredUsers = this.props.users.filter((user) => {
      if(user.head || user.newUser || user.username || user.id){
        return user.head || user.newUser || user.username.includes(filterValue) || user.id.toString().includes(filterValue)
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
        <DeleteDialog user={this.state.userToDelete} visible={this.state.isDeleteUserDialogShown} onHide={this.stopDeleteUserDialog} />
      </div>
    )
  }
}
  

/** change incoming user array into required usage */ 
const prepareUsers = (users) => {
  let res = []
  res.push({ head: true })
  res.push({ newUser: true })
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