import React from 'react';
import PropTypes from 'prop-types';
import { Toolbar } from 'primereact/toolbar';

const DefaultToolbar = ({ name, icon }) => {
  return (
    <>
      <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
          border-radius: 0;
        }
        .header {
          display: flex;
          height: 2rem;
          align-items: center;
        }
        .header :global(.pi) {
          font-size: 1.75rem;
          color: #aaa;
          margin-right: 1rem;
        }
        .h4 {
          font-weight: normal;
          margin-right: 1rem;
        }
      `}</style>

      <div className="this">
        <Toolbar>
          <div className="p-toolbar-group-left">
            <div className="header">
              <i className={'pi ' + icon}></i>
              <h4>{name}</h4>
            </div>
          </div>
        </Toolbar>
      </div>
    </>
  );
};

DefaultToolbar.propTypes = {
  /** The name of the current selection */
  name: PropTypes.string,

  icon: PropTypes.string,
};

export default DefaultToolbar;
