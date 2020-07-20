import React, { useState } from 'react';
import PropTypes from 'prop-types';

import CreateRenameCopyHandlerDialog from '../CreateRenameCopyHandlerDialog';
import AlertingChannelsTree from './AlertingChannelsTree';
import AlertingChannelsTreeToolbar from './AlertingChannelsTreeToolbar';
import DeleteDialog from '../../../../common/dialogs/DeleteDialog';

import * as topicsAPI from '../TopicsAPI';
import * as alertingConstants from '../../constants';

const AlertingChannelsTreeContainer = ({ topics, selection, readOnly, updateDate, onSelectionChanged, onRefresh }) => {
  const [isDeleteDialogShown, setDeleteDialogShown] = useState(false);
  const [isCreateDialogShown, setCreateDialogShown] = useState(false);
  const [isRenameDialogShown, setRenameDialogShown] = useState(false);
  const [isCopyDialogShown, setCopyDialogShown] = useState(false);

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-direction: column;
          border-right: 1px solid #ddd;
        }

        .this :global(.details) {
          color: #ccc;
          font-size: 0.75rem;
          text-align: center;
          padding: 0.25rem 0;
        }
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
        }
        .this :global(.p-toolbar-group-left) :global(.p-button) {
          margin-right: 0.25rem;
        }
        .this :global(.p-toolbar-group-right) :global(.p-button) {
          margin-left: 0.25rem;
        }
      `}</style>
      <AlertingChannelsTreeToolbar
        readOnly={readOnly}
        onShowDeleteDialog={() => setDeleteDialogShown(true)}
        onShowCreateDialog={() => setCreateDialogShown(true)}
        onShowRenameDialog={() => setRenameDialogShown(true)}
        onShowCopyDialog={() => setCopyDialogShown(true)}
        onRefresh={onRefresh}
        handlerSelected={!!selection.handler}
      />
      <AlertingChannelsTree topics={topics} selection={selection} onSelectionChanged={onSelectionChanged} />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateRenameCopyHandlerDialog
        visible={isCreateDialogShown}
        intention="create"
        onHide={() => setCreateDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topics.map((t) => t.id)}
        handlerTypes={alertingConstants.supportedHandlerTypes}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(handler, topic, handlerType) => {
          topicsAPI.createHandler({ id: handler, topic: topic, kind: handlerType }, () => {
            onSelectionChanged({ topic, handler });
            onRefresh();
          });
          setCreateDialogShown(false);
        }}
      />
      <CreateRenameCopyHandlerDialog
        visible={isCopyDialogShown}
        intention="copy"
        onHide={() => setCopyDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topics.map((t) => t.id)}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(handler, topic, kind) => {
          topicsAPI.createHandler({ id: handler, topic, kind }, () => {
            onSelectionChanged({ topic, handler });
            onRefresh();
          });
          setCopyDialogShown(false);
        }}
      />
      <CreateRenameCopyHandlerDialog
        visible={isRenameDialogShown}
        intention="rename"
        onHide={() => setRenameDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topics.map((t) => t.id)}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(handler, topic) => {
          topicsAPI.renameHandler(selection.handler, handler, selection.topic, topic, () => {
            onSelectionChanged({ topic, handler });
            onRefresh();
          });

          setRenameDialogShown(false);
        }}
      />
      <DeleteDialog
        visible={isDeleteDialogShown}
        onHide={() => setDeleteDialogShown(false)}
        name={selection.handler}
        text="Delete Alert Handler"
        onSuccess={(handlerName) =>
          topicsAPI.deleteHandler(handlerName, selection.topic, () => {
            onSelectionChanged({ topic: selection.topic, handler: null });
            onRefresh();
          })
        }
      />
    </div>
  );
};

AlertingChannelsTreeContainer.propTypes = {
  /** An array of strings denoting the available notification topics */
  topics: PropTypes.array,
  /** Current selection */
  selection: PropTypes.object.isRequired,
  /** read only mode */
  readOnly: PropTypes.bool,
  /** last update date */
  updateDate: PropTypes.object,
  /** Callback on selection change */
  onSelectionChanged: PropTypes.func,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingChannelsTreeContainer.defaultProps = {
  topics: [],
  readOnly: false,
  updateDate: undefined,
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

export default AlertingChannelsTreeContainer;
