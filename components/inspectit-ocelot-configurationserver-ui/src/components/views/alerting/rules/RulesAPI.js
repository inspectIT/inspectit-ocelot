import { remove, cloneDeep } from 'lodash';
/*
 * This file is just a dummy artifact, as long as the Alerting REST interface is not availeable!
 *
 * IGNORE this file for code review!
 */

const templates = [
  {
    id: 'MyGreat Template',
    created: '2006-01-02T15:04:05Z07:00',
    modified: '2006-01-02T15:04:05Z07:00',
  },
  {
    id: 'Error Rate Rules',
    created: '2006-01-02T15:04:05Z07:00',
    modified: '2006-01-02T15:04:05Z07:00',
  },
  {
    id: 'CPU JVM Rules',
    created: '2006-01-02T15:04:05Z07:00',
    modified: '2006-01-02T15:04:05Z07:00',
  },
];

var rules = [
  {
    id: 'ANOTHER_TASK_ID',
    status: 'enabled',
    executing: false,
    error: 'This an error description',
    template: 'MyGreat Template',
    topic: 'BLUB Topic',
  },
];

var ruleContents = [
  {
    id: 'ANOTHER_TASK_ID',
    template: 'MyGreat Template',
    status: 'enabled',
    description:
      'This is a rule description a great test, rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test rule description a great test ,  very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very very, very long',
    executing: false,
    error: 'This an error description',
    created: '2006-01-02T15:04:05+07:00',
    modified: '2006-01-02T15:04:05+07:00',
    'last-enabled': '2006-01-03T15:04:05Z07:00',
    vars: [
      {
        name: 'x',
        value: 5,
        type: 'int',
        description:
          'threshold value with a quite very long text that describes the vriable to show a measurement with a great value showing the template and combining it with another value',
      },
    ],
    topic: 'BLUB Topic',
  },
];

var templateContents = [
  {
    id: 'MyGreat Template',
    description: 'This the Great Template description!',
    created: '2006-01-02T15:04:05+07:00',
    modified: '2006-01-02T15:04:05+07:00',
    vars: [
      {
        name: 'x',
        value: 5,
        type: 'int',
        description:
          'threshold value with a quite very long text that describes the vriable to show a measurement with a great value showing the template and combining it with another value',
      },
      {
        name: 'interval',
        value: '5s',
        type: 'duration',
        description: 'This is an interval',
      },
      {
        name: 'my-name',
        value: '.*',
        type: 'regex',
        description: 'The name of the measurement',
      },
      {
        name: 'my-name2',
        value: 'hallo abc',
        type: 'string',
        description: 'The name of the measurement',
      },
      {
        name: 'my-name3',
        value: 2.3,
        type: 'float',
        description: 'The name of the measurement',
      },
      {
        name: 'my-name4',
        value: true,
        type: 'bool',
        description: 'The name of the measurement',
      },
    ],
  },
  {
    id: 'Error Rate Rules',
    description: 'This the Error Rate Template description!',
    created: '2006-01-02T15:04:05+07:00',
    modified: '2006-01-02T15:04:05+07:00',
    vars: [
      {
        name: 'interval',
        value: '5h',
        type: 'duration',
        description: 'This is an interval',
      },
      {
        name: 'error-rate-measurement',
        value: 'some.measurement',
        type: 'regex',
        description: 'The name of the measurement',
      },
    ],
  },
  {
    id: 'CPU JVM Rules',
    description: 'This the CPU JVM Template description!',
    created: '2006-01-02T15:04:05+07:00',
    modified: '2006-01-02T15:04:05+07:00',
    vars: [
      {
        name: 'someInteger',
        value: 5,
        type: 'int',
        description: 'threshold value',
      },
      {
        name: 'interval',
        value: '12m',
        type: 'duration',
        description: 'This is an interval',
      },
      {
        name: 'my-name',
        value: '.*',
        type: 'regex',
        description: 'The name of the measurement',
      },
    ],
  },
];

/**
 * Fetches all existing alerting templates.
 *
 * @param {string} newSelectionOnSuccess - If not empty, this rule will be selected on successful fetch.
 */
export const fetchAlertingTemplates = (onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      setTimeout(() => onSuccess(templates), 200);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

/**
 * Fetches all existing alerting rules.
 *
 * @param {string} newSelectionOnSuccess - If not empty, this rule will be selected on successful fetch.
 */
export const fetchAlertingRules = (onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      setTimeout(() => onSuccess(rules), 200);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

/**
 * Fetches the content of the given rule.
 */
export const fetchRule = (ruleName) => {
  return new Promise((resolve, reject) => {
    const success = true;
    if (success) {
      const content = ruleContents.find((r) => r.id === ruleName);
      setTimeout(() => resolve(content), 200);
    } else {
      reject();
    }
  });
};

/**
 * Fetches the content of the given template.
 */
export const fetchTemplate = (templateName) => {
  return new Promise((resolve, reject) => {
    const success = true;
    if (success) {
      var content = templateContents.find((t) => t.id === templateName);
      resolve(content);
    } else {
      reject();
    }
  });
};

/**
 * Attempts to delete the given rule.
 */
export const deleteRule = (ruleName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    rules = rules.filter((r) => r.id !== ruleName);
    ruleContents = ruleContents.filter((r) => r.id !== ruleName);
    if (onSuccess) {
      onSuccess(ruleName);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

/**
 * Attempts to update a rule.
 *
 * @param {string} rule - the rule object to update
 * @param {boolean} fetchRulesOnSuccess - if true, the rule tree be refeteched on a successful write
 * @param {boolean} selectRuleOnSuccess - if true, the newly created rule will be selected on success.
 *                                        Requires fetchRulesOnSuccess to be true
 */
export const updateRule = (ruleContent, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    remove(rules, (r) => r.id === ruleContent.id);
    remove(ruleContents, (r) => r.id === ruleContent.id);
    rules.push(cloneDeep(ruleContent));
    ruleContents.push(cloneDeep(ruleContent));

    if (onSuccess) {
      onSuccess(ruleContent);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

/**
 * Attempts to create a new alerting rule for the selected template.
 *
 * @param {string} rule - the rule object to save
 * @param {boolean} fetchRulesOnSuccess - if true, the rule tree be refeteched on a successful write
 * @param {boolean} selectRuleOnSuccess - if true, the newly created rule will be selected on success.
 *                                        Requires fetchRulesOnSuccess to be true
 */
export const createRule = (ruleName, templateName, description, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    const ruleMetaInfo = {
      id: ruleName,
      description: description,
      status: 'disabled',
      executing: false,
      error: '',
      template: templateName,
    };

    remove(rules, (r) => r.id === ruleName);
    remove(ruleContents, (r) => r.id === ruleName);
    rules.push(cloneDeep(ruleMetaInfo));
    ruleContents.push(cloneDeep(ruleMetaInfo));

    if (onSuccess) {
      onSuccess(ruleMetaInfo);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

/**
 * Attempts to rename the given rule.
 */
export const renameRule = (oldName, newName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    rules.find((r) => r.id === oldName).id = newName;
    ruleContents.find((r) => r.id === oldName).id = newName;
    if (onSuccess) {
      onSuccess();
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const copyRule = (srcName, targetName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    let ruleObj = cloneDeep(rules.find((r) => r.id === srcName));
    ruleObj.id = targetName;
    rules.push(ruleObj);
    ruleContents.push(ruleObj);

    if (onSuccess) {
      onSuccess(ruleObj);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};
