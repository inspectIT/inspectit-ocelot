import React from 'react';
import Menubar from '../components/layout/Menubar';
import SideNavigation from '../components/layout/SideNavigation';

/**
 * The main layout component.
 */
const MainLayout = (props) => {
  return (
    <div>
      <style jsx>
        {`
          .content {
            margin-left: 4rem;
            height: calc(100vh - 4rem);
            overflow: auto auto;
          }
        `}
      </style>
      <Menubar />
      <SideNavigation />
      <div className="content">{props.children}</div>
    </div>
  );
};

export default MainLayout;
