const HANDLER_ID_SEPARATOR = ':*:';

/**
 * Generates a unique handler id from the given topicId and handlerId.
 */
export const uniqueHandlerId = (handlerId, topicId) => topicId + HANDLER_ID_SEPARATOR + handlerId;

/**
 * Checks whether the given handler id is within the given topic (namewise).
 */
export const isHandlerInTopic = (uniqHandlerId, topicId) => {
  const array = uniqHandlerId.split(HANDLER_ID_SEPARATOR);
  return array && array.length > 0 && array[0] === topicId;
};
