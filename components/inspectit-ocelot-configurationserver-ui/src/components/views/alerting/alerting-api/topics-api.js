import axiosBearer from '../../../../lib/axios-api';
import { cloneDeep } from 'lodash';
import { fetch } from './common';

export const fetchTopics = () => fetch('/alert/kapacitor/topics');

export const fetchHandlers = (topicName) =>
  fetch(`/alert/kapacitor/topics/${topicName}/handlers`).then((handlers) => handlers.map((handler) => ({ ...handler, topic: topicName })));

export const fetchHandler = (topicName, handlerName) =>
  fetchHandlers(topicName).then((handlers) => {
    if (handlers) {
      let targetHandler = handlers.find((handler) => handler.id === handlerName);
      if (targetHandler) {
        targetHandler.topic = topicName;
        return targetHandler;
      }
    }
    return null;
  });

export const createHandler = (handlerName, topicName, kind) =>
  axiosBearer.post(`/alert/kapacitor/topics/${topicName}/handlers`, { id: handlerName, kind }).then((res) => res.data);

/**
 * Updates the given handler.
 * Attention: the topic cannot be changed with this operation!
 */
export const updateHandler = (topicName, handlerObj, oldId) =>
  axiosBearer.put(`/alert/kapacitor/topics/${topicName}/handlers/${oldId || handlerObj.id}`, handlerObj);

export const deleteHandler = (handlerName, topicName) => axiosBearer.delete(`/alert/kapacitor/topics/${topicName}/handlers/${handlerName}`);

const editOrCopyHandler = (oldName, newName, oldTopicName, newTopicName, isCopy) => {
  return fetchHandler(oldTopicName, oldName).then((handler) => {
    if (handler) {
      let newHandler = cloneDeep(handler);
      newHandler.id = newName;
      delete newHandler.topic;

      if (oldTopicName === newTopicName && !isCopy) {
        return updateHandler(oldTopicName, newHandler, oldName);
      } else {
        return createHandler(newName, newTopicName, newHandler.kind)
          .then(() => updateHandler(newTopicName, newHandler))
          .then(() => !isCopy && deleteHandler(oldName, oldTopicName));
      }
    }
  });
};

export const editHandler = (oldName, newName, oldTopicName, newTopicName) => {
  return editOrCopyHandler(oldName, newName, oldTopicName, newTopicName, false);
};

export const copyHandler = (sourceName, targetName, sourceTopicName, targetTopicName) => {
  return editOrCopyHandler(sourceName, targetName, sourceTopicName, targetTopicName, true);
};
