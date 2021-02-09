import React, { useState } from 'react';
import PropTypes from 'prop-types';
import CreateEditCopyHandlerDialog from '../CreateEditCopyHandlerDialog';
import AlertingChannelsTree from './AlertingChannelsTree';
import AlertingChannelsTreeToolbar from './AlertingChannelsTreeToolbar';
import DeleteDialog from '../../../../common/dialogs/DeleteDialog';
import { fetchHandlers, createHandler, editHandler, copyHandler, deleteHandler } from '../../alerting-api';
import * as alertingConstants from '../../constants';
import { notificationActions } from '../../../../../redux/ducks/notification';
import { useDispatch } from 'react-redux';

const AlertingChannelsTreeContainer = ({ topicNodes, selection, readOnly, updateDate, onSelectionChanged, onRefresh }) => {
  const dispatch = useDispatch();

  const [isDeleteDialogShown, setDeleteDialogShown] = useState(false);
  const [isCreateDialogShown, setCreateDialogShown] = useState(false);
  const [isEditDialogShown, setEditDialogShown] = useState(false);
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
        onShowEditDialog={() => setEditDialogShown(true)}
        onShowCopyDialog={() => setCopyDialogShown(true)}
        onRefresh={onRefresh}
        handlerSelected={!!selection.handler && selection.isSupportedHandlerKind}
      />
      <AlertingChannelsTree topicNodes={topicNodes} selection={selection} onSelectionChanged={onSelectionChanged} />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateEditCopyHandlerDialog
        visible={isCreateDialogShown}
        intention="create"
        onHide={() => setCreateDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topicNodes.map((t) => t.id)}
        handlerTypes={alertingConstants.supportedHandlerTypes}
        retrieveReservedNames={async (topicName, callback) => {
          try {
            const handlers = await fetchHandlers(topicName);
            callback(handlers.map((h) => h.id));
          } catch (error) {
            callback([]);
          }
        }}
        onSuccess={async (handler, topic, handlerType) => {
          try {
            await createHandler(handler, topic, handlerType);
          } catch (error) {
            dispatch(notificationActions.showErrorMessage('Failed creating alerting handler', ''));
          }
          onSelectionChanged({ topic, handler, isSupportedHandlerKind: true }, [topic]);
          setCreateDialogShown(false);
        }}
      />
      <CreateEditCopyHandlerDialog
        visible={isCopyDialogShown}
        intention="copy"
        onHide={() => setCopyDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topicNodes.map((t) => t.id)}
        retrieveReservedNames={async (topicName, callback) => {
          try {
            const handlers = await fetchHandlers(topicName);
            callback(handlers.map((h) => h.id));
          } catch (error) {
            callback([]);
          }
        }}
        onSuccess={async (handler, topic) => {
          try {
            await copyHandler(selection.handler, handler, selection.topic, topic);
            onSelectionChanged({ topic, handler, isSupportedHandlerKind: true }, [topic]);
          } catch (error) {
            dispatch(notificationActions.showErrorMessage('Failed copying alerting handler', ''));
          }
          setCopyDialogShown(false);
        }}
      />
      <CreateEditCopyHandlerDialog
        visible={isEditDialogShown}
        intention="edit"
        oldName={selection.handler}
        onHide={() => setEditDialogShown(false)}
        initialTopic={selection.topic || ''}
        topics={topicNodes.map((t) => t.id)}
        retrieveReservedNames={async (topicName, callback) => {
          try {
            const handlers = await fetchHandlers(topicName);
            callback(handlers.map((h) => h.id));
          } catch (error) {
            callback([]);
          }
        }}
        onSuccess={async (handler, topic) => {
          try {
            await editHandler(selection.handler, handler, selection.topic, topic);
            onSelectionChanged({ topic, handler, isSupportedHandlerKind: true }, [selection.topic, topic]);
          } catch (error) {
            dispatch(notificationActions.showErrorMessage('Failed editing alerting handler', ''));
          }
          setEditDialogShown(false);
        }}
      />
      <DeleteDialog
        visible={isDeleteDialogShown}
        onHide={() => setDeleteDialogShown(false)}
        name={selection.handler}
        text="Delete Alert Handler"
        onSuccess={async (handlerName) => {
          try {
            await deleteHandler(handlerName, selection.topic);
            onSelectionChanged({ topic: selection.topic, handler: null, isSupportedHandlerKind: false }, [selection.topic]);
          } catch (error) {
            dispatch(notificationActions.showErrorMessage('Failed deleting alerting handler', ''));
          }
        }}
      />
    </div>
  );
};

AlertingChannelsTreeContainer.propTypes = {
  /** An array of objects denoting the topics tree */
  topicNodes: PropTypes.array,
  /** Current selection */
  selection: PropTypes.object.isRequired,
  /** read only mode */
  readOnly: PropTypes.bool,
  /** last update date */
  updateDate: PropTypes.number,
  /** Callback on selection change */
  onSelectionChanged: PropTypes.func,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingChannelsTreeContainer.defaultProps = {
  topicNodes: [],
  readOnly: false,
  updateDate: undefined,
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

export default AlertingChannelsTreeContainer;
