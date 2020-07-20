import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import AlertingChannelsTree from './AlertingChannelsTree';
import HandlerEditor from './editor/HandlerEditor';


/**
 * The component for managing alerting topics and handlers.
 */
const AlertingChannelsView = ({ topics, updateDate, onRefresh }) => {
  const readOnly = useSelector((state) => !state.authentication.permissions.write).readOnly;

  const [selectedTopicName, setSelectedTopicName] = useState(undefined);
  const [selectedHandlerName, setSelectedHandlerName] = useState(undefined);

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-grow: 1;
        }
        .this :global(.green) {
          color: green;
        }
        .this :global(.orange) {
          color: orange;
        }
        .this :global(.red) {
          color: red;
        }
        .this :global(.blue) {
          color: blue;
        }
        .this :global(.grey) {
          color: grey;
        }
      `}</style>
      <AlertingChannelsTree
        selectedTopicName={selectedTopicName}
        selectedHandlerName={selectedHandlerName}
        onSelectionChanged={(topic, handler) => {
          setSelectedTopicName(topic);
          setSelectedHandlerName(handler);
        }}
        onRefresh={onRefresh}
        updateDate={updateDate}
        availableTopics={topics}
      />
      <HandlerEditor
        selectedTopicName={selectedTopicName}
        selectedHandlerName={selectedHandlerName}
        availableTopics={topics}
        readOnly={readOnly}
      />
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
