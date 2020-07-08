import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { uniq } from 'lodash';
import PropTypes from 'prop-types';
import { Tree } from 'primereact/tree';

/**
 * The alerting rules tree.
 */
const AlertingRulesTree = ({
  rules,
  templates,
  unsavedRules,
  selectedRuleName,
  selectedTemplateName,
  onSelectionChanged,
  groupingOptions,
}) => {
  const [expandedKeys, setExpandedKeys] = useState({});
  const selectedTopic = selectedRuleName ? rules.find((r) => r.id === selectedRuleName).topic : undefined;
  useEffect(() => {
    var keysToAdd = undefined;
    if (selectedRuleName && !(selectedTemplateName in expandedKeys)) {
      keysToAdd = { [selectedTemplateName]: true };
    }
    const rule = selectedRuleName ? rules.find((r) => r.id === selectedRuleName) : undefined;
    const topicName = rule && rule.topic ? rule.topic : 'UNDEFINED TOPIC';
    if (rule && !(topicName in expandedKeys)) {
      keysToAdd = { ...keysToAdd, ...{ [topicName]: true } };
    }
    if (keysToAdd) {
      setExpandedKeys({ ...expandedKeys, ...keysToAdd });
    }
  }, [selectedRuleName, selectedTemplateName, groupingOptions, selectedTopic]);

  const rulesTree = getRulesTree(rules, templates, unsavedRules, groupingOptions.groupByTemplates, groupingOptions.groupByTopics);
  return (
    <div className="this">
      <style jsx>{`
        .this {
          overflow: auto;
          flex-grow: 1;
        }
        .this :global(.p-treenode-label) {
          width: 80%;
        }
        .this :global(.green) {
          color: green;
        }
        .this :global(.grey) {
          color: grey;
        }
        .this :global(.red) {
          color: red;
        }
        .this :global(.rule-label) {
          display: flex;
          flex-direction: row;
          justify-content: space-between;
          width: 100%;
        }
      `}</style>
      <Tree
        filter={true}
        filterBy="label"
        value={rulesTree}
        nodeTemplate={nodeTemplate}
        selectionMode="single"
        selectionKeys={selectedRuleName || selectedTemplateName}
        onSelectionChange={(e) => {
          const selectedValue = e.value;
          const rule = rules.find((r) => r.id === selectedValue);
          if (rule) {
            onSelectionChanged(selectedValue, rule.template);
          } else if (templates.some((t) => t.id === selectedValue)) {
            onSelectionChanged(undefined, selectedValue);
          }
        }}
        expandedKeys={expandedKeys}
        onToggle={(e) => setExpandedKeys(e.value)}
      />
    </div>
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
      classNames = classNames + 'pi-circle-off red';
    } else {
      classNames = classNames + 'pi-circle-off grey';
    }

    return (
      <div className="rule-label">
        {node.label + (node.unsaved ? ' *' : '')}
        <i className={classNames} />
      </div>
    );
  } else {
    return <b>{node.label + (node.unsaved ? ' *' : '')}</b>;
  }
};

/**
 * Returns the loaded rules in a tree structure used by the tree component.
 */
const getRulesTree = (rules, templates, unsavedRules, groupByTemplates, groupByTopics) => {
  if (!rules || !templates) {
    return [];
  }

  var treeNodes = rules.map((rule) =>
    toTreeBranch(
      rule.id,
      'rule',
      rule,
      rule.topic,
      rule.template,
      undefined,
      unsavedRules.some((usRuleName) => usRuleName === rule.id)
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
          leafNodesFilteredByTemplate.some((child) => child.unsaved === true)
        );
      });
      return toTreeBranch(
        topicName,
        'topic',
        topicName,
        topicName,
        undefined,
        templateNodes,
        templateNodes.some((child) => child.unsaved === true)
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
        children.some((child) => child.unsaved === true)
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
          children.some((child) => child.unsaved === true)
        );
      });
  }
  treeNodes.sort((t1, t2) => t1.key.localeCompare(t2.key));
  return treeNodes;
};

const toTreeBranch = (name, type, data, topic, template, children, hasUnsaved) => {
  const iconClassNames = classNames('pi', 'pi-fw', {
    'pi-bars': type === 'topic',
    'pi-briefcase': type === 'template',
    'pi-bell': type === 'rule',
  });
  if (children && children.length > 0) {
    children.sort((r1, r2) => r1.key.localeCompare(r2.key));
  }
  const key = name ? name : 'UNDEFINED ' + type.toUpperCase();
  return {
    key: key,
    type: type,
    label: key,
    data: data,
    topic: topic,
    template,
    icon: iconClassNames,
    leaf: !children || children.length <= 0,
    children,
    unsaved: hasUnsaved,
  };
};

AlertingRulesTree.propTypes = {
  /**  Name of the selected rule */
  selectedRuleName: PropTypes.string.isRequired,
  /**  Name of the selected template (template in the current context) */
  selectedTemplateName: PropTypes.string.isRequired,
  /** List of all rules */
  rules: PropTypes.array.isRequired,
  /**  List of all templates */
  templates: PropTypes.array.isRequired,
  /**  Whether to group by templates and / or topics */
  groupingOptions: PropTypes.object,
  /**  List of rules that are unsaved */
  unsavedRules: PropTypes.array.isRequired,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
};

AlertingRulesTree.defaultProps = {
  onSelectionChanged: () => {},
  groupingOptions: {
    groupByTemplates: true,
    groupByTopics: false,
  },
};

const mapStateToProps = (state) => {
  return {
    groupingOptions: state.alerting.ruleGrouping,
  };
};

export default connect(mapStateToProps, {})(AlertingRulesTree);
