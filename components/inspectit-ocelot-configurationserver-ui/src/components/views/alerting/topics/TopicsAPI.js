/*
 * This file is just a dummy artifact, as long as the Alerting REST interface is not availeable!
 *
 * IGNORE this file for code review!
 */

import { cloneDeep, remove } from 'lodash';

const topics = [
  {
    id: 'Some Topic',
    level: 'OK',
  },
  {
    id: 'great topic',
    level: 'INFO',
  },
  {
    id: 'Some E-Mail distribution',
    level: 'WARNING',
  },
  {
    id: 'XY-Team',
    level: 'CRITICAL',
  },
];

const handlers = {
  'Some Topic': [
    {
      id: 'Handler A',
      topic: 'Some Topic',
      kind: 'smtp',
    },
    {
      id: 'Handler B',
      topic: 'Some Topic',
      kind: 'logs',
    },
  ],
  'XY-Team': [
    {
      id: 'Handler X',
      topic: 'XY-Team',
      kind: 'smtp',
    },
    {
      id: 'Handler Y',
      topic: 'XY-Team',
      kind: 'logs',
    },
  ],
};

export const fetchTopics = (onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      onSuccess(topics);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const fetchHandlers = (topicName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      setTimeout(() => onSuccess(topicName in handlers ? handlers[topicName] : []), 200);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const fetchSupportedHandlerTypes = (onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      onSuccess(['smtp', 'publish']);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const createHandler = (handlerObj, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    const topicName = handlerObj.topic;
    const handlerName = handlerObj.id;
    if (handlers[topicName] && handlers[topicName].length > 0) {
      remove(handlers[topicName], (h) => h.id === handlerName);
    }
    if (!handlers[topicName]) {
      handlers[topicName] = [];
    }
    if (!topics.some((t) => t.id === topicName)) {
      topics.push({ id: topicName });
    }

    handlers[topicName].push(cloneDeep(handlerObj));

    if (onSuccess) {
      onSuccess(handlerObj);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const deleteHandler = (handlerName, topicName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (handlers[topicName] && handlers[topicName].length > 0) {
      remove(handlers[topicName], (h) => h.id === handlerName);
    }

    if (onSuccess) {
      onSuccess(handlerName);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const renameHandler = (oldName, newName, oldTopicName, newTopicName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (handlers[oldTopicName] && handlers[oldTopicName].length > 0) {
      const handler = cloneDeep(handlers[oldTopicName].find((h) => h.id === oldName));
      handler.id = newName;
      handler.topic = newTopicName;
      deleteHandler(oldName, oldTopicName, () =>
        createHandler(handler, () => {
          if (onSuccess) {
            onSuccess();
          }
        })
      );
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};
