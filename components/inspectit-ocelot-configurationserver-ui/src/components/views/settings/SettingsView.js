import React from 'react';
import SettingsMenu from './SettingsMenu';

/**
 * The Settings View Wrapper, includes the Tab Menu.
 */
const SettingsView = (props) => {
  return (
    <div>
      <style jsx>
        {`
          .content {
            margin-top: 3rem;
            overflow: auto auto;
          }
        `}
      </style>

      <SettingsMenu />

      <div className="content">{props.children}</div>
    </div>
  );
};

export default SettingsView;
