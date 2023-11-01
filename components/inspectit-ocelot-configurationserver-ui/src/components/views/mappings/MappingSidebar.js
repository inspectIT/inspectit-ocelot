import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import MappingsHistoryView from './history/MappingsHistoryView';
import SidebarTypes from './SidebarTypes';
import { mappingsActions } from '../../../redux/ducks/mappings';

/**
 * The sidebar of the configuration view.
 */
const MappingSidebar = () => {
  const dispatch = useDispatch();

  // global state variables
  const currentSidebar = useSelector((state) => state.mappings.currentSidebar);

  const toggleHistoryView = () => {
    dispatch(mappingsActions.toggleHistoryView());
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
        <div className="content-container">{currentSidebar == SidebarTypes.HISTORY && <MappingsHistoryView />}</div>
        <div>
          <button
            className={'vert-button p-button p-togglebutton' + (currentSidebar == SidebarTypes.HISTORY ? 'p-highlight' : '')}
            onClick={toggleHistoryView}
          >
            <i className={'pi pi-chevron-' + (currentSidebar == SidebarTypes.HISTORY ? 'right' : 'left')} />
            <span>Versioning</span>
          </button>
        </div>
      </div>
    </>
  );
};

MappingSidebar.propTypes = {};

export default MappingSidebar;
