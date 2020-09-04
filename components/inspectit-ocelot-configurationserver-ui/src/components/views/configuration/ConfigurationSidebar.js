import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import HistoryView from './history/HistoryView';
import { configurationActions } from '../../../redux/ducks/configuration';

/**
 * The sidebar of the configuration view.
 */
const ConfigurationSidebar = () => {
  const dispatch = useDispatch();

  // global state variables
  const showHistoryView = useSelector((state) => state.configuration.showHistoryView);

  const toggleHistoryView = () => {
    dispatch(configurationActions.toggleHistoryView());
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
        <div className="content-container">{showHistoryView && <HistoryView />}</div>

        <div>
          <button className={'vert-button p-button p-togglebutton' + (showHistoryView ? 'p-highlight' : '')} onClick={toggleHistoryView}>
            <i className={'pi pi-chevron-' + (showHistoryView ? 'right' : 'left')} />
            <span>Versioning</span>
          </button>
        </div>
      </div>
    </>
  );
};

ConfigurationSidebar.propTypes = {};

export default ConfigurationSidebar;
