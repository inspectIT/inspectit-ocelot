import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import classNames from 'classnames';
import _, { uniq } from 'lodash';
import PropTypes from 'prop-types';
import { Tree } from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { alertingActions } from '../../../../../redux/ducks/alerting';
import classnames from 'classnames';
import { ruleIcon, templateIcon, topicIcon } from '../../constants';

/**
 * The alerting rules tree.
 */
const AlertingRulesTree = ({ rules, templates, unsavedRules, onSelectionChanged, selection }) => {
  const dispatch = useDispatch();

  // state variables
  const [contextMenu, setContextMenu] = useState(null);
  const [treeIndex, setTreeIndex] = useState({});
  const [treeData, setTreeData] = useState(null);

  // global state variables
  const { groupByTemplates, groupByTopics } = useSelector((state) => state.alerting.ruleGrouping);

  // derived variables
  let currentSelectionKey;
  if (selection.rule) {
    currentSelectionKey = 'rule_' + selection.rule;
  } else if (selection.template) {
    currentSelectionKey = 'template_' + selection.template;
  }

  // updating the tree index
  useEffect(() => {
    const rulesTree = getRulesTree(rules, templates, unsavedRules, groupByTemplates, groupByTopics, setTreeIndex);
    setTreeData(rulesTree);
  }, [rules, templates, unsavedRules, groupByTemplates, groupByTopics]);

  // menu items for the tree's context menu
  const contextMenuItems = [
    {
      label: 'Group by Notification Channel',
      icon: 'pi pi-fw ' + (groupByTopics ? 'pi-check' : ''),
      command: () => dispatch(alertingActions.changeRuleGroupingOptions({ groupByTemplates, groupByTopics: !groupByTopics })),
    },
    {
      label: 'Group by Templates',
      icon: 'pi pi-fw ' + (groupByTemplates ? 'pi-check' : ''),
      command: () => dispatch(alertingActions.changeRuleGroupingOptions({ groupByTemplates: !groupByTemplates, groupByTopics })),
    },
  ];

  // callback when the tree selection is changing
  const onTreeSelection = (event) => {
    const itemKey = event.value;

    const newSelection = { rule: null, template: null };

    const metaData = treeIndex[itemKey];
    if (metaData.type === 'rule') {
      const rule = rules.find((r) => r.id === metaData.label);
      newSelection.rule = metaData.label;
      newSelection.template = rule.template;
    } else if (metaData.type === 'template') {
      newSelection.template = metaData.label;
    }

    onSelectionChanged(newSelection);
  };

  return (
    <>
      <style jsx>{`
        .this {
          overflow: auto;
          flex-grow: 1;
        }
        .this :global(.p-treenode-label) {
          width: 80%;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .green) {
          color: #4caf50;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .grey) {
          color: #9e9e9e;
        }
        .this :global(.p-treenode-content:not(.p-highlight) .red) {
          color: #f44336;
        }
        .this :global(.rule-label) {
          display: flex;
          flex-direction: row;
          justify-content: space-between;
          width: 100%;
          align-items: center;
        }

        .this :global(.context-menu.p-contextmenu) {
          width: 20rem;
        }
      `}</style>

      <div className="this">
        <ContextMenu className="context-menu" model={contextMenuItems} ref={(el) => setContextMenu(el)} />

        <Tree
          onContextMenu={(event) => contextMenu.show(event.originalEvent)}
          filter={true}
          filterBy="label"
          value={treeData}
          nodeTemplate={nodeTemplate}
          selectionMode="single"
          selectionKeys={currentSelectionKey}
          onSelectionChange={onTreeSelection}
        />
      </div>
    </>
  );
};

/**
 * Rendering template for the tree nodes.
 */
const nodeTemplate = (node) => {
  if (node.type === 'rule') {
    var classNames = 'pi ';
    if (node.data.status === 'enabled' && !node.data.error) {
      classNames = classNames + 'pi-circle-on green';
    } else if (node.data.status === 'enabled' && node.data.error) {
      classNames = classNames + 'pi-times-circle red';
    } else {
      classNames = classNames + 'pi-circle-off grey';
    }

    return (
      <div className={classnames('rule-label', { 'undefined-element': node.isUndefinedName })}>
        {node.label + (node.unsaved ? ' *' : '')}
        <i className={classNames} />
      </div>
    );
  } else {
    const style = node.isUndefinedName ? { fontStyle: 'italic', color: '#BDBDBD', fontWeight: 'normal' } : {};
    return <b style={style}>{node.label + (node.unsaved ? ' *' : '')}</b>;
  }
};

/**
 * Returns the loaded rules in a tree structure used by the tree component.
 */
const getRulesTree = (rules, templates, unsavedRules, groupByTemplates, groupByTopics, setTreeIndex) => {
  if (_.isEmpty(rules) || _.isEmpty(templates)) {
    return [];
  }

  const newTreeIndex = {};

  // generate rule nodes
  var treeNodes = rules.map((rule) =>
    toTreeBranch(
      rule.id,
      'rule',
      rule,
      rule.topic,
      rule.template,
      undefined,
      unsavedRules.some((usRuleName) => usRuleName === rule.id),
      newTreeIndex
    )
  );
  const activeTopics = uniq(rules.map((r) => r.topic));
  if (groupByTopics && groupByTemplates) {
    treeNodes = uniq([...activeTopics, undefined]).map((topicName) => {
      const leafNodesFilteredByTopic = treeNodes.filter((node) => node.topic === topicName);
      const activeTemplates = topicName ? uniq(leafNodesFilteredByTopic.map((node) => node.template)) : templates.map((t) => t.id);
      const templateNodes = activeTemplates.map((templateName) => {
        const leafNodesFilteredByTemplate = leafNodesFilteredByTopic.filter((node) => node.template === templateName);
        return toTreeBranch(
          templateName,
          'template',
          templateName,
          topicName,
          templateName,
          leafNodesFilteredByTemplate,
          leafNodesFilteredByTemplate.some((child) => child.unsaved === true),
          newTreeIndex
        );
      });
      return toTreeBranch(
        topicName,
        'topic',
        topicName,
        topicName,
        undefined,
        templateNodes,
        templateNodes.some((child) => child.unsaved === true),
        newTreeIndex
      );
    });
  } else if (groupByTopics) {
    treeNodes = activeTopics.map((topicName) => {
      const children = treeNodes.filter((treeNode) => treeNode.topic === topicName);
      return toTreeBranch(
        topicName,
        'topic',
        topicName,
        topicName,
        undefined,
        children,
        children.some((child) => child.unsaved === true),
        newTreeIndex
      );
    });
  } else if (groupByTemplates) {
    treeNodes = templates
      .map((t) => t.id)
      .map((templateName) => {
        const children = treeNodes.filter((treeNode) => treeNode.template === templateName);
        return toTreeBranch(
          templateName,
          'template',
          templateName,
          undefined,
          templateName,
          children,
          children.some((child) => child.unsaved === true),
          newTreeIndex
        );
      });
  }
  treeNodes.sort((t1, t2) => t1.key.localeCompare(t2.key));

  setTreeIndex(newTreeIndex);

  return treeNodes;
};

const toTreeBranch = (name, type, data, topic, template, children, hasUnsaved, newTreeIndex) => {
  const iconClassNames = classNames('pi', 'pi-fw', {
    [topicIcon]: type === 'topic',
    [templateIcon]: type === 'template',
    [ruleIcon]: type === 'rule',
  });
  if (children && children.length > 0) {
    children.sort((r1, r2) => r1.key.localeCompare(r2.key));
  }

  const isUndefinedName = !name;

  const label = isUndefinedName ? 'UNDEFINED ' + type.toUpperCase() : name;
  const key = type + '_' + label;

  newTreeIndex[key] = {
    label,
    type,
  };

  return {
    key: key,
    type: type,
    label,
    data: data,
    topic: topic,
    template,
    icon: iconClassNames,
    leaf: !children || children.length <= 0,
    children,
    unsaved: hasUnsaved,
    isUndefinedName,
  };
};

AlertingRulesTree.propTypes = {
  /** List of all rules */
  rules: PropTypes.array.isRequired,
  /**  List of all templates */
  templates: PropTypes.array.isRequired,
  /**  List of rules that are unsaved */
  unsavedRules: PropTypes.array.isRequired,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
};

AlertingRulesTree.defaultProps = {
  onSelectionChanged: () => { },
  groupingOptions: {
    groupByTemplates: true,
    groupByTopics: false,
  },
};

export default AlertingRulesTree;
