const HANDLER_ID_SEPARATOR = ':*:';

/**
 * Generates a unique handler id from the given topicId and handlerId.
 */
export const uniqueHandlerId = (handlerId, topicId) => topicId + HANDLER_ID_SEPARATOR + handlerId;

export const resolveSelection = (selectionKey) => {
  if (!selectionKey) {
    return { topic: null, handler: null };
  }
  const array = selectionKey.split(HANDLER_ID_SEPARATOR);

  if (array && array.length === 2) {
    return { topic: array[0], handler: array[1] };
  } else if (array && array.length === 1) {
    return { topic: array[0], handler: null };
  } else {
    return { topic: null, handler: null };
  }
};
