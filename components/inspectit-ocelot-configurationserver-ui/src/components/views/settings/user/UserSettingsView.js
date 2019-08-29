import React from 'react'
import UserDataTable from './UserDataTable';
import UserToolbar from './UserSettingsToolbar'
import {ScrollPanel} from 'primereact/scrollpanel'

class UserSettingsView extends React.Component {

  constructor(props){
    super(props)
    this.state = {
      filter: ''
    }
  }

  handleChange = (e) => {
    this.setState({filter: e.target.value})
  }

  render() {
    return (
      <div className='this'>
        <style jsx>{`
          .fixed{
            position: fixed;
            top: 6.5rem;
            width: calc(100vw - 4rem);
            overflow: auto auto;
          }
          .content{
            margin-top: 3rem;
            height: calc(100vh - 9.5rem);
            overflow: auto auto;
          }
        `}</style>
        <div className='fixed'><UserToolbar filterValue={this.state.filter} changeFilter={this.handleChange}/></div>
        <div className='content'>
            <UserDataTable filterValue={this.state.filter}/>
        </div>
      </div>
    )
  }

}

export default UserSettingsView
