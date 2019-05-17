import React from 'react'
import {TabMenu} from 'primereact/tabmenu'
import itemData from '../../../../data/settings-navigation-items.json'
import { withRouter } from 'next/router';

class SettingsMenu extends React.Component{
  constructor(props){
    super(props)
    this.state={
      navItems: itemData.map(item => ( {label: item.name, icon: `pi ${item.icon}`, href: item.href} )),
    }
  }

  componentDidMount(){
    this.setState({ activeItem: this.state.navItems.find((item) => {return this.props.router.pathname.endsWith(item.href)}) })
  }

  render() {
    return(
      <div className='this'>
        <style jsx>{`
          .this {
            padding-left: 0.2rem;
            position: fixed;
            top: 4rem;
            left: 4rem;
            width: 100%;
            background-color: #eee;
          }
          .this :global(.p-tabmenu-nav) {
            border-bottom: solid #ddd 2px;
          }
          .this :global(.p-tabmenu-nav .p-tabmenuitem .p-menuitem-link) {
            background-color: #b5b5b5;
            border: solid #ddd 1px;
          }
          .this :global(.p-tabmenu-nav .p-tabmenuitem.p-highlight .p-menuitem-link){
            background-color: #515151;
            border: solid #515151 1px;
          }
        `}</style>
        <TabMenu model={this.state.navItems} activeItem={this.state.activeItem} onTabChange={(e) => {this.setState({activeItem: e.value}); this.props.router.push(e.value.href) }} />
      </div>
    )  
  }
}

export default withRouter(SettingsMenu)