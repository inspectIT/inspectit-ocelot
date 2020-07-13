import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { findIndex, cloneDeep } from 'lodash';
import { Tree } from 'primereact/tree';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

import CreateRenameCopyDialog from './CreateRenameCopyDialog';
import DeleteDialog from '../../../common/dialogs/DeleteDialog';

import * as topicsAPI from './TopicsAPI';
import * as handlerUtils from './handlerUtils';
import * as alertingConstants from '../constants';

const AlertingChannelsTree = ({
  availableTopics,
  selectedHandlerName,
  selectedTopicName,
  unsavedHandlers,
  readOnly,
  updateDate,
  onSelectionChanged,
  onRefresh,
}) => {
  const [isDeleteDialogShown, setDeleteDialogShown] = useState(false);
  const [isCreateDialogShown, setCreateDialogShown] = useState(false);
  const [isRenameDialogShown, setRenameDialogShown] = useState(false);
  const [isCopyDialogShown, setCopyDialogShown] = useState(false);
  const [treeNodes, setTreeNodes] = useState(undefined);
  const [treeBranchesToUpdate, setTreeBranchesToUpdate] = useState({});
  const [loading, setLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState({});

  useExpandedKeys(expandedKeys, setExpandedKeys, selectedHandlerName, selectedTopicName);

  useTopicNodes(availableTopics, unsavedHandlers, setTreeNodes);

  useHandlerNodes(treeNodes, treeBranchesToUpdate, unsavedHandlers, setTreeNodes, setLoading, setTreeBranchesToUpdate);

  useUnsavedStatusUpdate(treeNodes, setTreeNodes, unsavedHandlers);

  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-direction: column;
          border-right: 1px solid #ddd;
        }
        .this :global(.p-tree) {
          height: 100%;
          border: 0;
          flex-grow: 1;
          border-radius: 0;
          display: flex;
          flex-direction: column;
          background: 0;
          overflow: auto;
        }
        .this :global(.details) {
          color: #ccc;
          font-size: 0.75rem;
          text-align: center;
          padding: 0.25rem 0;
        }
        .this :global(.p-treenode-label) {
          width: 80%;
        }

        .this :global(.treeNode) {
          display: flex;
          flex-direction: row;
          flex-grow: 1;
          justify-content: space-between;
          width: 100%;
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
      <Toolbar>
        <div className="p-toolbar-group-left">
          <Button
            disabled={readOnly || loading}
            tooltip="New handler"
            icon="pi pi-plus"
            tooltipOptions={tooltipOptions}
            onClick={() => setCreateDialogShown(true)}
          />
          <Button
            disabled={readOnly || loading || !selectedHandlerName}
            tooltip="Rename handler"
            icon="pi pi-pencil"
            tooltipOptions={tooltipOptions}
            onClick={() => setRenameDialogShown(true)}
          />
          <Button
            disabled={readOnly || loading || !selectedHandlerName}
            tooltip="Copy handler"
            icon="pi pi-copy"
            tooltipOptions={tooltipOptions}
            onClick={() => setCopyDialogShown(true)}
          />
          <Button
            disabled={readOnly || loading || !selectedHandlerName}
            tooltip="Delete handler"
            icon="pi pi-trash"
            tooltipOptions={tooltipOptions}
            onClick={() => setDeleteDialogShown(true)}
          />
        </div>
        <div className="p-toolbar-group-right">
          <Button
            disabled={loading}
            onClick={onRefresh}
            tooltip="Reload"
            icon={'pi pi-refresh' + (loading ? ' pi-spin' : '')}
            tooltipOptions={tooltipOptions}
          />
        </div>
      </Toolbar>
      <Tree
        filter={true}
        filterBy="label"
        value={treeNodes}
        nodeTemplate={nodeTemplate}
        selectionMode="single"
        onExpand={(event) => setTreeBranchesToUpdate({ ...treeBranchesToUpdate, ...{ [event.node.key]: true } })}
        loading={loading}
        expandedKeys={expandedKeys}
        selectionKeys={selectedHandlerName ? handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName) : selectedTopicName}
        onSelectionChange={(e) => {
          const { topic, handler } = resolveSelection(e.value, treeNodes);
          onSelectionChanged(topic, handler);
        }}
        onToggle={(e) => setExpandedKeys(e.value)}
      />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateRenameCopyDialog
        visible={isCreateDialogShown}
        intention="create"
        onHide={() => setCreateDialogShown(false)}
        initialTopic={selectedTopicName || ''}
        topics={availableTopics.map((t) => t.id)}
        handlerTypes={alertingConstants.supportedHandlerTypes}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(name, topic, handlerType) => {
          topicsAPI.createHandler({ id: name, topic: topic, kind: handlerType }, () => {
            setTreeBranchesToUpdate({ ...treeBranchesToUpdate, ...{ [topic]: true } });
            onSelectionChanged(topic, name);
            onRefresh();
          });
          setCreateDialogShown(false);
        }}
      />
      <CreateRenameCopyDialog
        visible={isCopyDialogShown}
        intention="copy"
        onHide={() => setCopyDialogShown(false)}
        initialTopic={selectedTopicName || ''}
        topics={availableTopics.map((t) => t.id)}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(name, topic) => {
          const node = getHandlerNode(treeNodes, selectedTopicName, selectedHandlerName);
          if (node) {
            let copyNode = cloneDeep(node);
            copyNode.id = name;
            copyNode.topic = topic;
            topicsAPI.createHandler(copyNode, () => {
              setTreeBranchesToUpdate({ ...treeBranchesToUpdate, ...{ [topic]: true } });
              onSelectionChanged(topic, name);
              onRefresh();
            });
          }

          setCopyDialogShown(false);
        }}
      />
      <CreateRenameCopyDialog
        visible={isRenameDialogShown}
        intention="rename"
        onHide={() => setRenameDialogShown(false)}
        initialTopic={selectedTopicName || ''}
        topics={availableTopics.map((t) => t.id)}
        retrieveReservedNames={(topicName, callback) => {
          topicsAPI.fetchHandlers(topicName, (handlers) => callback(handlers.map((h) => h.id)));
        }}
        onSuccess={(name, topic) => {
          topicsAPI.renameHandler(selectedHandlerName, name, selectedTopicName, topic, () => {
            setTreeBranchesToUpdate({ ...treeBranchesToUpdate, ...{ [selectedTopicName]: true, [topic]: true } });
            onSelectionChanged(topic, name);
            onRefresh();
          });

          setRenameDialogShown(false);
        }}
      />
      <DeleteDialog
        visible={isDeleteDialogShown}
        onHide={() => setDeleteDialogShown(false)}
        name={selectedHandlerName}
        text="Delete Alert Handler"
        onSuccess={(handlerName) =>
          topicsAPI.deleteHandler(handlerName, selectedTopicName, (deletedHandler) => {
            if (deletedHandler === selectedHandlerName) {
              setTreeBranchesToUpdate({ ...treeBranchesToUpdate, ...{ [selectedTopicName]: true } });
              onSelectionChanged(selectedTopicName, undefined);
            }
            onRefresh();
          })
        }
      />
    </div>
  );
};

/** Custom effect to update the expanded keys on selection change. */
const useExpandedKeys = (expandedKeys, setExpandedKeys, selectedHandlerName, selectedTopicName) => {
  useEffect(() => {
    if (selectedHandlerName && !(selectedTopicName in expandedKeys)) {
      setExpandedKeys({ ...expandedKeys, ...{ [selectedTopicName]: true } });
    }
  }, [selectedHandlerName, selectedTopicName]);
};

/** Custom effect to update the first level of the tree, when topics change. */
const useTopicNodes = (availableTopics, unsavedHandlers, setTreeNodes) => {
  useEffect(() => {
    let rootNodes = (availableTopics || []).map((topic) => ({
      key: topic.id,
      label: topic.id,
      leaf: !!topic.referencedOnly,
      type: 'topic',
      level: topic.level,
      referencedOnly: topic.referencedOnly,
      icon: classNames('pi pi-fw', alertingConstants.topicIcon),
      unsaved: unsavedHandlers.some((h) => handlerUtils.isHandlerInTopic(h, topic.id)),
    }));
    setTreeNodes(rootNodes);
  }, [JSON.stringify(availableTopics)]);
};

/** Custom effect to reload second level of the tree if needed. */
const useHandlerNodes = (treeNodes, treeBranchesToUpdate, unsavedHandlers, setTreeNodes, setLoading, setTreeBranchesToUpdate) => {
  useEffect(() => {
    if (treeNodes) {
      const topicNode = treeNodes.find((tNode) => treeBranchesToUpdate[tNode.key] === true);
      if (topicNode) {
        setLoading(true);

        topicsAPI.fetchHandlers(topicNode.key, (handlers) => {
          let node = { ...topicNode };
          node.children = [];

          handlers.forEach((h) => {
            node.children.push({
              key: handlerUtils.uniqueHandlerId(h.id, topicNode.key),
              label: h.id,
              leaf: true,
              type: 'handler',
              kind: h.kind,
              icon: classNames('pi pi-fw', alertingConstants.handlerIcons(h.kind)),
              unsaved: unsavedHandlers.some((unsavedH) => unsavedH === handlerUtils.uniqueHandlerId(h.id, topicNode.key)),
            });
          });

          node.unsaved = unsavedHandlers.some((unsavedH) => handlerUtils.isHandlerInTopic(unsavedH, topicNode.key));
          let nodes = [...treeNodes];
          const idx = findIndex(nodes, { key: topicNode.key });
          nodes[idx] = node;
          setTreeNodes(nodes);
          setLoading(false);
          let copyBranchesToUpadte = { ...treeBranchesToUpdate };
          delete copyBranchesToUpadte[topicNode.key];
          setTreeBranchesToUpdate(copyBranchesToUpadte);
        });
      }
    }
  }, [treeBranchesToUpdate, treeNodes]);
};

/** Custom effect to update the unsaved status in the tree. */
const useUnsavedStatusUpdate = (treeNodes, setTreeNodes, unsavedHandlers) => {
  useEffect(() => {
    if (treeNodes) {
      let nodes = [...treeNodes];
      nodes.forEach((topicNode) => {
        if (topicNode.children && topicNode.children.length > 0) {
          topicNode.children.forEach((child) => {
            child.unsaved = unsavedHandlers.some((h) => h === child.key);
          });
        }
        topicNode.unsaved = unsavedHandlers.some((unsavedH) => handlerUtils.isHandlerInTopic(unsavedH, topicNode.key));
      });
      setTreeNodes(nodes);
    }
  }, [JSON.stringify(unsavedHandlers)]);
};

/** Template for displaying tree nodes. */
const nodeTemplate = (node) => {
  const classnames = classNames('pi pi-fw', alertingConstants.severityLevelClassNames(node.level, node.referencedOnly));

  return (
    <div className="treeNode">
      {node.type === 'topic' && (
        <>
          <b>{node.label + (node.unsaved ? ' *' : '')}</b>
          <i className={classnames} />
        </>
      )}
      {node.type === 'handler' && node.label + (node.unsaved ? ' *' : '')}
    </div>
  );
};

const resolveSelection = (value, treeNodes) => {
  const rootNode = treeNodes.find((node) => node.key === value);
  if (rootNode) {
    return { topic: rootNode.key, handler: undefined };
  } else {
    const obj = treeNodes
      .map((node) => {
        const child = node.children ? node.children.find((ch) => ch.key === value) : undefined;
        return { topic: node.label, handler: child ? child.label : undefined };
      })
      .find((obj) => obj.handler !== undefined);
    return obj || {};
  }
};

const getHandlerNode = (treeNodes, topicName, handlerName) => {
  const topicNode = treeNodes.find((n) => n.label === topicName);
  if (topicNode) {
    return topicNode.children && topicNode.children.find((n) => n.label === handlerName);
  } else {
    return undefined;
  }
};

AlertingChannelsTree.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** Name of the selected handler */
  selectedHandlerName: PropTypes.string,
  /** Name of the selected topic */
  selectedTopicName: PropTypes.string,
  /** list of unsaved handlers */
  unsavedHandlers: PropTypes.array,
  /** read only mode */
  readOnly: PropTypes.bool,
  /** last update date */
  updateDate: PropTypes.object,
  /** Callback on selection change */
  onSelectionChanged: PropTypes.func,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingChannelsTree.defaultProps = {
  availableTopics: [],
  selectedHandlerName: undefined,
  selectedTopicName: undefined,
  unsavedHandlers: [],
  readOnly: false,
  updateDate: undefined,
  onSelectionChanged: () => { },
  onRefresh: () => { },
};

const mapStateToProps = (state) => {
  const { unsavedHandlerContents } = state.alerting;

  return {
    unsavedHandlers: Object.keys(unsavedHandlerContents),
  };
};

export default connect(mapStateToProps, {})(AlertingChannelsTree);
