import React from 'react';
import { withRouter } from 'next/router';
import { TabMenu } from 'primereact/tabmenu';

import tabMenuItems from '../../../data/settings-navigation-items.json';

/**
 * The tab menu to switch between different settings.
 */
class SettingsMenu extends React.Component {
  constructor(props) {
    super(props);

    const navItems = tabMenuItems.map((item) => ({ label: item.name, icon: `pi ${item.icon}`, href: item.href }));

    const activeItem = navItems.find((item) => {
      return this.props.router.pathname.endsWith(item.href);
    });

    this.state = {
      navItems,
      activeItem,
    };
  }

  render() {
    return (
      <div className="this">
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
        <TabMenu model={this.state.navItems} activeItem={this.state.activeItem} onTabChange={(e) => this.onTabChange(e.value)} />
      </div>
    );
  }

  onTabChange = (item) => {
    this.setState({ activeItem: item });

    this.props.router.push(item.href);
  };
}

export default withRouter(SettingsMenu);
