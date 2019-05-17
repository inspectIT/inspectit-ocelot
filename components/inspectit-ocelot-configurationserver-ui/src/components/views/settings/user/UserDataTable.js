import React from 'react'
import {DataView} from 'primereact/dataview'
import { Button } from 'primereact/button';
import {InputText} from 'primereact/inputtext'
import {Password} from 'primereact/password'
import {Dropdown} from 'primereact/dropdown'
import * as data from '../../../../data/settings-configuration.json'
import { connect } from 'react-redux'
import {settingsActions} from '../../../../redux/ducks/settings'
import { notificationActions } from '../../../../redux/ducks/notification'

class UserDataTable extends React.Component{
  constructor(props){
    super(props)
    this.state = {
      layout: 'list',
      usercopy: {},
      newUser: {}
    }
    this.roles = data.roles
  }

  componentDidMount(){
    this.props.getUsers()
  }

  addUser = () => {
    const {newUser} = this.state
    if(!newUser.username || newUser.username === '' || !newUser.password || newUser.password === ''){
      this.props.showErrorMessage('Missing Input', 'username or password is missing')
      return
    }

    this.setState({newUser: {username: '', password: '', role: ''} })
    this.props.addUser(newUser)
  }

  itemTemplate = (user, layout) => {
    const {usercopy, newUser} = this.state
    let editMode = null
    if(usercopy[user.id] && usercopy[user.id].edit === true) {
      editMode = true
    }
    if (layout === 'list') {
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
            <div className='p-col-3'>
              {editMode ? 
                <InputText
                  value={usercopy[user.id].username}
                  onChange={(e) => {
                    this.setState({ usercopy: {
                      ...usercopy,
                      [user.id]: {...usercopy[user.id], username: e.target.value}
                    }})
                  }}
                /> : user.head ? 'Username' : user.newUser ? 
                <InputText
                  value={newUser.username}
                  onChange={(e) => { this.setState({ newUser: { ...newUser, username: e.target.value } })
                  }}
                /> : `${user.username}`
              }
            </div>
            <div className='p-col-3'>
              {editMode ? 
                <Password 
                  value={usercopy[user.id].password} 
                  feedback={false} 
                  onChange={(e) => {
                    this.setState({ usercopy: {
                      ...usercopy,
                      [user.id]: {...usercopy[user.id], password: e.target.value}
                    }})
                  }}
                /> : user.head ? 'Password' : user.newUser ? 
                <Password 
                  value={newUser.password} 
                  feedback={false} 
                  onChange={(e) => { this.setState({ newUser: { ...newUser, password: e.target.value } })
                  }}
                /> : `*****`
                }
            </div>
            <div className='p-col-3'>
               {editMode ? 
                <Dropdown 
                  disabled={true}
                  optionLabel='name' 
                  options={this.roles}
                  placeholder='Select a role'
                  value={usercopy[user.id].role} 
                  onChange={(e) => {
                    this.setState({ usercopy: {
                      ...usercopy,
                      [user.id]: {...usercopy[user.id], role: e.target.value}
                    }})
                  }}
                /> : user.head ? 'Role' : user.newUser ? 
                <Dropdown 
                  disabled={true}
                  optionLabel='name' 
                  options={this.roles}
                  placeholder='Select a role'
                  value={newUser.role} 
                  onChange={(e) => { this.setState({ newUser: { ...newUser, role: e.target.value } })
                  }}
                /> : `${user.role}`
                }
              </div>
            <div className='p-col'>
              {editMode ? 
              <div>
                <Button icon='pi pi-trash' onClick={ (e) => {this.props.deleteUser(user.id); this.setState({ usercopy:{ ...usercopy, [user.id]: { ...user, edit:false } } }) } } style={{marginRight:'.25em'}}/>
                <Button icon='pi pi-times' onClick={ (e) => {this.setState({ usercopy:{ ...usercopy, [user.id]: { ...user, edit:false } } }) } } style={{marginRight:'.25em'}}/>
                <Button disabled={true} icon='pi pi-check' onClick={ (e) => { /**TODO: request senden */this.setState({ usercopy:{ ...usercopy, [user.id]: { ...user, edit:false } } }) } } style={{marginRight:'.25em'}}/>
              </div>
              : user.head ? `` : user.newUser ? <Button icon='pi pi-plus' onClick={this.addUser}/> : 
              <Button icon='pi pi-pencil' 
                onClick={(e) => {
                  this.setState({ usercopy: {
                    ...usercopy,
                    [user.id]: {...user, edit: true}
                  }})
                }}
              />
              }
            </div>
          </div>
      );
    }
    if (layout === 'grid') {
        return (
            <div className="p-col-12 p-md-3">
                <div>{user.id}</div>
            </div>
        );
    }
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
        <DataView value={filteredUsers} layout={this.state.layout} itemTemplate={this.itemTemplate} style={{marginBottom: '2.5rem'}}></DataView>
      </div>
    )
  }
}

// change incoming user array into required usage for UserDataTable Component
const prepareUsers = (users) => {
  let res = []
  res.push({ head: true })
  res.push({ newUser: true })
  users.forEach(element => {
    res.push(
      {
        id: element.id,
        username: element.username,
        role: 'admin'
      }
    )
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
  deleteUser: settingsActions.deleteUser,
  addUser: settingsActions.addUser,
  showErrorMessage: notificationActions.showErrorMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(UserDataTable)