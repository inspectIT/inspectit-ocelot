import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import _ from 'lodash';
import AlertingChannelsTreeContainer from './tree/AlertingChannelsTreeContainer';
import HandlerEditorContainer from './editor/HandlerEditorContainer';
import { fetchHandlers } from '../alerting-api';

import useDeepEffect from '../../../../hooks/use-deep-effect';

/**
 * The component for managing alerting topics and handlers.
 */
const AlertingChannelsView = ({ topics, updateDate, onRefresh }) => {
  const readOnly = useSelector((state) => !state.authentication.permissions.write);

  const [currentSelection, setCurrentSelection] = useState({ topic: null, handler: null });
  const [topicsStructure, setTopicsStructure] = useState([]);

  // update 1st level of the topics data when topics list changes
  useDeepEffect(() => {
    const updateHandlerStructure = async () => {
      const topicsToAdd = _.differenceBy(topics || [], topicsStructure, 'id');
      const topicsToDelete = _.differenceBy(topicsStructure, topics || [], 'id');

      if (topicsToAdd.length > 0 || topicsToDelete.length > 0) {
        let newTopicsStructure = [...topicsStructure];
        _.pullAllBy(newTopicsStructure, topicsToDelete, 'id');
        topicsToAdd.forEach((topic) => newTopicsStructure.push(topic));
        setTopicsStructure(_.sortBy(newTopicsStructure, ['id']));
      }
    };

    updateHandlerStructure();
  }, [topics, updateDate]);

  const updateTopics = async (topicIdsToUpdate) => {
    const newTopicsStructure = _.cloneDeep(topicsStructure);
    const topicsToUpdate = newTopicsStructure.filter((topic) => _.includes(topicIdsToUpdate, topic.id));

    // parallelize fetching
    const promises = topicsToUpdate.map((topic) => ({ topic: topic, promise: loadTopicChildren(topic.id) }));
    // await all fetches
    for (let idx = 0; idx < promises.length; idx++) {
      promises[idx].topic.handlers = await promises[idx].promise;
    }

    if (topicsToUpdate.length > 0) {
      setTopicsStructure(newTopicsStructure);
    }
  };

  const loadTopicChildren = async (topicId) => {
    const handlers = await fetchHandlers(topicId);

    return !handlers ? [] : _.sortBy(handlers, ['id']).map((handler) => ({ ...handler }));
  };

  const selectedTopic = topicsStructure.find((topic) => topic.id === currentSelection.topic);
  const selectedHandler =
    selectedTopic && selectedTopic.handlers ? selectedTopic.handlers.find((handler) => handler.id === currentSelection.handler) : null;

  return (
    <div className="this">
      <style jsx>{`
        .this {
          display: flex;
          flex-grow: 1;
          overflow: hidden;
        }
      `}</style>
      <AlertingChannelsTreeContainer
        selection={currentSelection}
        topicNodes={topicsStructure}
        onSelectionChanged={(selection, topicsToUpdate = null) => {
          if (topicsToUpdate) {
            updateTopics(_.uniq(topicsToUpdate));
          }
          if (selection.topic !== currentSelection.topic || selection.handler !== currentSelection.handler) {
            setCurrentSelection(selection);
          }
        }}
        onRefresh={async () => {
          // reset all topics and load them from scratch
          await setTopicsStructure([]);
          onRefresh();
        }}
        updateDate={updateDate}
        readOnly={readOnly}
      />
      <HandlerEditorContainer
        selection={currentSelection}
        savedHandlerContent={selectedHandler}
        availableTopics={topics || []}
        readOnly={readOnly}
        onSaved={async () => {
          await updateTopics([currentSelection.topic]);
        }}
      />
    </div>
  );
};

AlertingChannelsView.propTypes = {
  /** An array of topics denoting the available notification topics */
  topics: PropTypes.array,
  /**  Last date the list of topics was loaded. */
  updateDate: PropTypes.number.isRequired,
  /** Callback on topic refresh */
  onRefresh: PropTypes.func,
};

AlertingChannelsView.defaultProps = {
  topics: [],
  onSelectionChanged: () => {},
};

export default AlertingChannelsView;
