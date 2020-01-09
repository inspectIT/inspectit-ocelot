import React from 'react';
import { withRouter } from 'next/router';
import { TabMenu } from 'primereact/tabmenu';

/** Data */
import itemData from '../../../data/user-settings-navigation-items.json';

/**
 * the tab menu to switch between different user settings
 */
class UserSettingsMenu extends React.Component {
  state = {
    navItems: itemData.map(item => ({ label: item.name, icon: `pi ${item.icon}`, href: item.href })),
  }

  componentDidMount() {
    this.setState({ activeItem: this.state.navItems.find((item) => { return this.props.router.pathname.endsWith(item.href) }) })
  }

  render() {
    return (
      <div className='this'>
        <style jsx>{`
          .this {
            padding-left: 0.5em;
            position: fixed;
            top: 4rem;
            left: 4rem;
            width: 100%;
            background-color: #eee;
          }
          .this :global(.p-tabmenu-nav) {
            border-bottom: solid #ddd 2px;
          }
          .this :global(.p-tabmenuitem) {
            padding-top: 0.5em;
            height: 2.8rem;
          }
          .this :global(.p-tabmenu-nav .p-tabmenuitem .p-menuitem-link) {
            background-color: #b6b6b6;
            border: solid #ddd 1px;
          }
        `}</style>
        <TabMenu model={this.state.navItems} activeItem={this.state.activeItem} onTabChange={(e) => { this.setState({ activeItem: e.value }); this.props.router.push(e.value.href) }} />
      </div>
    )
  }
}

export default withRouter(UserSettingsMenu)