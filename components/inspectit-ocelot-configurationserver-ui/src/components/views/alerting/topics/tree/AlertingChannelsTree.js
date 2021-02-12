import React, { useState } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { useSelector } from 'react-redux';
import { includes } from 'lodash';
import { Tree } from 'primereact/tree';
import useDeepEffect from '../../../../../hooks/use-deep-effect';
import * as alertingConstants from '../../constants';
import { uniqueHandlerId, resolveSelection } from '../handlerUtils';
import { supportedHandlerTypes } from '../../constants';

const AlertingChannelsTree = ({ topicNodes, selection, onSelectionChanged }) => {
  // local state variables
  const [expandedKeys, setExpandedKeys] = useState({});

  // global state variables
  const unsavedHandlers = useSelector((state) => Object.keys(state.alerting.unsavedHandlerContents));

  useDeepEffect(() => {
    if (topicNodes) {
      let newExpandedKeys = {};
      Object.keys(expandedKeys)
        .filter((key) => topicNodes.some((topic) => topic.id === key))
        .forEach((key) => {
          newExpandedKeys = { ...newExpandedKeys, ...{ [key]: expandedKeys[key] } };
        });
      if (selection.handler && !(selection.topic in expandedKeys)) {
        newExpandedKeys = { ...newExpandedKeys, ...{ [selection.topic]: true } };
      }
      setExpandedKeys(newExpandedKeys);
    }
  }, [topicNodes]);

  const treeNodes = !topicNodes
    ? []
    : topicNodes.map((topic) => ({
        key: topic.id,
        label: topic.id,
        leaf: !!topic.referencedOnly,
        type: 'topic',
        level: topic.level,
        referencedOnly: topic.referencedOnly,
        icon: classNames('pi pi-fw', alertingConstants.topicIcon),
        children: topic.handlers
          ? topic.handlers.map((handler) => ({
              key: uniqueHandlerId(handler.id, topic.id),
              label: handler.id,
              leaf: true,
              type: 'handler',
              kind: handler.kind,
              icon: classNames('pi pi-fw', alertingConstants.handlerIcons(handler.kind)),
            }))
          : [],
      }));

  return (
    <div className="this">
      <style jsx>{`
        .this {
          overflow: auto;
          flex-grow: 1;
          display: flex;
          flex-direction: column;
          border-right: 1px solid #ddd;
        }
        .this :global(.p-tree) {
          height: 100%;
          border: 0;
          border-radius: 0;
          display: flex;
          flex-direction: column;
          background: 0;
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
        .this :global(.p-treenode-content:not(.p-highlight) .green) {
          color: green;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .orange) {
          color: orange;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .red) {
          color: red;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .blue) {
          color: blue;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .grey) {
          color: grey;
        }
      `}</style>
      <Tree
        filter={true}
        filterBy="label"
        value={treeNodes}
        nodeTemplate={(node) => nodeTemplate(node, unsavedHandlers)}
        selectionMode="single"
        onExpand={(event) => onSelectionChanged(selection, [event.node.key])}
        expandedKeys={expandedKeys}
        selectionKeys={selection.handler ? uniqueHandlerId(selection.handler, selection.topic) : selection.topic}
        onSelectionChange={(e) => {
          const { topic, handler } = resolveSelection(e.value);
          const topicNode = treeNodes.find((tNode) => tNode.key === topic);
          const handlerNode = topicNode && topicNode.children ? topicNode.children.find((hNode) => hNode.label === handler) : null;
          const isSupportedHandlerKind = handlerNode ? includes(supportedHandlerTypes, handlerNode.kind) : false;
          onSelectionChanged({ topic, handler, isSupportedHandlerKind });
        }}
        onToggle={(e) => setExpandedKeys(e.value)}
      />
    </div>
  );
};

/** Template for displaying tree nodes. */
const nodeTemplate = (node, unsavedHandlers) => {
  const classnames = classNames('pi pi-fw', alertingConstants.severityLevelClassNames(node.level, node.referencedOnly));

  let unsaved = false;
  if (node.type === 'handler') {
    unsaved = includes(unsavedHandlers, node.key);
  } else if (node.type === 'topic') {
    unsaved = node.children && node.children.some((handlerNode) => includes(unsavedHandlers, handlerNode.key));
  }

  const style = node.referencedOnly ? { fontStyle: 'italic', fontWeight: 'normal' } : {};

  return (
    <div className="treeNode">
      {node.type === 'topic' && (
        <>
          <b style={style}>{node.label + (unsaved ? ' *' : '')}</b>
          <i className={classnames} />
        </>
      )}
      {node.type === 'handler' && node.label + (unsaved ? ' *' : '')}
    </div>
  );
};

AlertingChannelsTree.propTypes = {
  /** An array of objects denoting the topics tree */
  topicNodes: PropTypes.array,
  /** Current selection */
  selection: PropTypes.object.isRequired,
  /** Callback on selection change */
  onSelectionChanged: PropTypes.func,
};

AlertingChannelsTree.defaultProps = {
  topicNodes: [],
  onSelectionChanged: () => {},
};

export default AlertingChannelsTree;
