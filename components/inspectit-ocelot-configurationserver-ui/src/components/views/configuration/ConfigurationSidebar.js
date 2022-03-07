import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import HistoryView from './history/HistoryView';
import { configurationActions } from '../../../redux/ducks/configuration';
import DocumentationView from './documentation/DocumentationView';
import SidebarTypes from './SidebarTypes';

/**
 * The sidebar of the configuration view.
 */
const ConfigurationSidebar = () => {
  const dispatch = useDispatch();

  // global state variables
  const currentSidebar = useSelector((state) => state.configuration.currentSidebar);

  const toggleHistoryView = () => {
    dispatch(configurationActions.toggleHistoryView());
  };

  const toggleDocumentationView = () => {
    dispatch(configurationActions.toggleDocumentationView());
  };

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
          {currentSidebar == SidebarTypes.HISTORY && <HistoryView />}
          {currentSidebar == SidebarTypes.CONFIGURATION_DOCS && <DocumentationView />}
        </div>

        <div>
          <button
            className={'vert-button p-button p-togglebutton' + (currentSidebar == SidebarTypes.HISTORY ? 'p-highlight' : '')}
            onClick={toggleHistoryView}
          >
            <i className={'pi pi-chevron-' + (currentSidebar == SidebarTypes.HISTORY ? 'right' : 'left')} />
            <span>Versioning</span>
          </button>
          <button
            className={'vert-button p-button p-togglebutton' + (currentSidebar == SidebarTypes.CONFIGURATION_DOCS ? 'p-highlight' : '')}
            onClick={toggleDocumentationView}
          >
            <i className={'pi pi-chevron-' + (currentSidebar == SidebarTypes.CONFIGURATION_DOCS ? 'right' : 'left')} />
            <span>Configuration Docs</span>
          </button>
        </div>
      </div>
    </>
  );
};

ConfigurationSidebar.propTypes = {};

export default ConfigurationSidebar;
