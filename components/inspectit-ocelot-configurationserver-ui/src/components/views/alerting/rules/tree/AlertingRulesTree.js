import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Tree } from 'primereact/tree';

/**
 * The alerting rules tree.
 */
const AlertingRulesTree = ({ rules, templates, unsavedRules, selectedRuleName, selectedTemplateName, onSelectionChanged }) => {
  const [expandedKeys, setExpandedKeys] = useState({});

  useEffect(() => {
    if (selectedRuleName && selectedTemplateName && !(selectedTemplateName in expandedKeys)) {
      setExpandedKeys({ ...expandedKeys, ...{ [selectedTemplateName]: true } });
    }
  }, [selectedRuleName, selectedTemplateName]);

  const rulesTree = getRulesTree(rules, templates, unsavedRules);
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
    if (node.enabled && !node.data.error) {
      classNames = classNames + 'pi-circle-on green';
    } else if (node.enabled && node.data.error) {
      classNames = classNames + 'pi-circle-off red';
    } else {
      classNames = classNames + 'pi-circle-off grey';
    }

    return (
      <div className="rule-label">
        {node.label}
        <i className={classNames} />
      </div>
    );
  } else {
    return <b>{node.label}</b>;
  }
};

/**
 * Returns the loaded rules in a tree structure used by the tree component.
 */
const getRulesTree = (rules, templates, unsavedRules) => {
  if (!rules || !templates) {
    return [];
  }

  var treeBranches = templates.map((template) => templateToTreeBranch(template, rules, unsavedRules));
  treeBranches.sort((t1, t2) => t1.key.localeCompare(t2.key));
  return treeBranches;
};

/**
 * Creates a tree branch for the given template and associated rules.
 * @param {*} template template to create the tree branch for
 * @param {*} rules list of all known rules
 * @param {*} unsavedRules list of unsaved rules
 */
const templateToTreeBranch = (template, rules, unsavedRules) => {
  const filteredRules = rules.filter((r) => r.template === template.id);

  const leaf = !filteredRules || filteredRules.length <= 0;
  var children = [];
  if (!leaf) {
    children = filteredRules.map((rule) => {
      const key = rule.id;
      const unsaved = unsavedRules.some((ruleName) => ruleName === key);
      const label = key + (unsaved ? ' *' : '');

      return {
        key,
        type: 'rule',
        label,
        data: rule,
        enabled: rule.status === 'enabled',
        icon: 'pi pi-fw pi-bell',
        leaf: true,
        unsaved,
      };
    });
  }
  children.sort((r1, r2) => r1.key.localeCompare(r2.key));
  return {
    key: template.id,
    type: 'template',
    label: template.id + (children.some((c) => c.unsaved) ? ' *' : ''),
    data: template,
    icon: 'pi pi-fw pi-briefcase',
    leaf,
    children,
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
  /**  List of rules that are unsaved */
  unsavedRules: PropTypes.array.isRequired,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
};

AlertingRulesTree.defaultProps = {
  onSelectionChanged: () => {},
};

export default AlertingRulesTree;
