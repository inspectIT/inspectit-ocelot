import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { useSelector } from 'react-redux';
import { sortBy, includes, findIndex } from 'lodash';
import { Tree } from 'primereact/tree';
import useDeepEffect from '../../../../../hooks/use-deep-effect';

import * as topicsAPI from '../TopicsAPI';
import * as alertingConstants from '../../constants';
import { uniqueHandlerId, resolveSelection } from '../handlerUtils';

const AlertingChannelsTree = ({ topics, selection, onSelectionChanged }) => {
  // local state variables
  const [treeNodes, setTreeNodes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState({});

  // global state variables
  const unsavedHandlers = useSelector((state) => Object.keys(state.alerting.unsavedHandlerContents));

  const prevTopicContext = useRef(null);

  // load tree nodes on topic or selection change
  useDeepEffect(() => {
    const reloadTree = async () => {
      let newNodes = [];

      for (let index = 0; index < topics.length; index++) {
        const topic = topics[index];
        let topicNode = {
          key: topic.id,
          label: topic.id,
          leaf: !!topic.referencedOnly,
          type: 'topic',
          level: topic.level,
          referencedOnly: topic.referencedOnly,
          icon: classNames('pi pi-fw', alertingConstants.topicIcon),
        };

        if (topic.id === prevTopicContext.current || topic.id === selection.topic) {
          topicNode.children = await loadTopicChildren(topic.id);
        } else {
          const existingNode = treeNodes.find((treeNode) => treeNode.key === topic.id);
          topicNode.children = existingNode ? existingNode.children : [];
        }

        newNodes.push(topicNode);
      }

      setTreeNodes(newNodes);
    };
    reloadTree();

    if (selection.handler && !(selection.topic in expandedKeys)) {
      setExpandedKeys({ ...expandedKeys, ...{ [selection.topic]: true } });
    }
  }, [topics, selection]);

  prevTopicContext.current = selection.topic;

  const loadSubTreeOnExpand = async (topicId) => {
    setLoading(true);

    let nodes = [...treeNodes];

    const topicNode = nodes.find((tNode) => tNode.key === topicId);
    if (topicNode) {
      let node = { ...topicNode };
      node.children = await loadTopicChildren(topicId);
      const idx = findIndex(nodes, { key: topicId });
      nodes[idx] = node;
    }

    setTreeNodes(nodes);
    setLoading(false);
  };

  const loadTopicChildren = async (topicId) => {
    const handlers = await topicsAPI.fetchHandlers(topicId);

    return !handlers
      ? []
      : sortBy(handlers, ['id']).map((handler) => ({
          key: uniqueHandlerId(handler.id, topicId),
          label: handler.id,
          leaf: true,
          type: 'handler',
          kind: handler.kind,
          icon: classNames('pi pi-fw', alertingConstants.handlerIcons(handler.kind)),
        }));
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
        onExpand={(event) => loadSubTreeOnExpand(event.node.key)}
        loading={loading}
        expandedKeys={expandedKeys}
        selectionKeys={selection.handler ? uniqueHandlerId(selection.handler, selection.topic) : selection.topic}
        onSelectionChange={(e) => onSelectionChanged(resolveSelection(e.value))}
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
  /** An array of strings denoting the available notification topics */
  topics: PropTypes.array,
  /** Current selection */
  selection: PropTypes.object.isRequired,
  /** Callback on selection change */
  onSelectionChanged: PropTypes.func,
};

AlertingChannelsTree.defaultProps = {
  topics: [],
  onSelectionChanged: () => {},
};

export default AlertingChannelsTree;
