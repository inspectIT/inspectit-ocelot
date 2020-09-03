import React from 'react';
import HistoryView from './history/HistoryView';

const ConfigurationSidebar = ({ selectedVersion, selectedVersionChange, showHistory, showHistoryView }) => {
  return (
    <>
      <style jsx>
        {`
          .sidebar {
            border: 0;
            border-radius: 0;
            background-color: #eee;
            border-left: 1px solid #ddd;
            flex: 0;
            display: flex;
            background-color: #eeeeee;
          }
          .content-container {
          }
          .vert-button {
            display: flex;
            flex-direction: column;
            padding: 1rem 0.5rem;
            border-radius: 0;
          }
          .vert-button i {
            margin-bottom: 0.5rem;
          }
          .vert-button span {
            writing-mode: vertical-rl;
            font-size: 1rem;
          }
        `}
      </style>

      <div className="sidebar">
        <div className="content-container">
          {showHistory && <HistoryView selectedVersion={selectedVersion} selectedVersionChange={selectedVersionChange} />}
        </div>

        <div>
          <button className={'vert-button p-button p-togglebutton' + (showHistory ? 'p-highlight' : '')} onClick={showHistoryView}>
            <i className={'pi pi-chevron-' + (showHistory ? 'right' : 'left')} />
            <span>Versioning</span>
          </button>
        </div>
      </div>
    </>
  );
};

export default ConfigurationSidebar;
