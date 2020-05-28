import React from 'react';
import NavigationItem from './SideNavigationItem';
import {connect} from 'react-redux'

/** Data */
import itemData from '../../data/side-navigation-items.json';

/**
 * The application's side-navigation.
 * The items are defined in the JSON file which has been imported.
 */
const SideNavigation = ({isAdmin}) => {
  return (
    <div className="this">
      <style jsx>
        {`
          .this {
            position: fixed;
            top: 4rem;
            left: 0;
            bottom: 0;
            width: 4rem;
            background-color: #eee;
            display: flex;
            flex-direction: column;
            align-items: center;
            border-right: 1px solid #ddd;
          }
        `}
      </style>
      {itemData.top.map((item) => (
        <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} />
      ))}
      <div style={{ flexGrow: 1 }} />
      {isAdmin && itemData.bottom.map((item) => (
        <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} />
      ))}
    </div>
  );
};

function mapStateToProps(state) {
  return {
    isAdmin: state.authentication.permissions.admin
  };
}
export default connect(mapStateToProps)(SideNavigation);
