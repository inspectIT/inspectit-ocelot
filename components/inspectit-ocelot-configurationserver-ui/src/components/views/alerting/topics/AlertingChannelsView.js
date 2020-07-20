import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import AlertingChannelsTreeContainer from './tree/AlertingChannelsTreeContainer';
import HandlerEditorContainer from './editor/HandlerEditorContainer';

/**
 * The component for managing alerting topics and handlers.
 */
const AlertingChannelsView = ({ topics, updateDate, onRefresh }) => {
  const readOnly = useSelector((state) => !state.authentication.permissions.write);

  const [currentSelection, setCurrentSelection] = useState({ topic: null, handler: null });

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-grow: 1;
        }
      `}</style>
      <AlertingChannelsTreeContainer
        selection={currentSelection}
        onSelectionChanged={(selection) => {
          setCurrentSelection(selection);
        }}
        onRefresh={onRefresh}
        updateDate={updateDate}
        topics={topics}
        readOnly={readOnly}
      />
      <HandlerEditorContainer selection={currentSelection} availableTopics={topics} readOnly={readOnly} />
    </div>
  );
};

AlertingChannelsView.propTypes = {
  /** An array of strings denoting the available notification topics */
  topics: PropTypes.array,
  /**  Last date the list of topics was loaded. */
  updateDate: PropTypes.array.isRequired,
  /** Callback on topic refresh */
  onRefresh: PropTypes.func,
};

AlertingChannelsView.defaultProps = {
  topics: [],
  onSelectionChanged: () => {},
};

export default AlertingChannelsView;
