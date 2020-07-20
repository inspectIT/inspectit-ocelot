import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { omit, isEqual, extend } from 'lodash';
import { useSelector, useDispatch } from 'react-redux';
import HandlerEditorToolbar from './HandlerEditorToolbar';
import * as topicsAPI from '../TopicsAPI';
import { alertingActions } from '../../../../../redux/ducks/alerting';
import * as handlerUtils from '../handlerUtils';
import DefaultToolbar from '../../DefaultToolbar';
import useDeepEffect from '../../../../../hooks/use-deep-effect';
import HandlerEditor from './HandlerEditor';

const HandlerEditorContainer = ({ selection, readOnly, availableTopics }) => {
  const dispatch = useDispatch();
  const [savedHandlerContent, setSavedHandlerContent] = useState(undefined);

  // global state variables
  const unsavedHandlerContents = useSelector((state) => state.alerting.unsavedHandlerContents);

  // load saved version of the handler when selection changes
  useDeepEffect(() => {
    if (selection.handler) {
      topicsAPI.fetchHandler(selection.topic, selection.handler, (handler) => setSavedHandlerContent(handler));
    } else {
      setSavedHandlerContent(undefined);
    }
  }, [selection]);

  const uniqueHandlerId = handlerUtils.uniqueHandlerId(selection.handler, selection.topic);
  const selectedHandlerIsUnsaved = unsavedHandlerContents && uniqueHandlerId && uniqueHandlerId in unsavedHandlerContents;
  const handlerContent = selectedHandlerIsUnsaved ? unsavedHandlerContents[uniqueHandlerId] : savedHandlerContent;

  const onContentChanged = (changedContent) => {
    if (selection.handler) {
      if (savedHandlerContent && isEqual(savedHandlerContent, changedContent)) {
        dispatch(alertingActions.handlerContentsChanged(omit({ ...unsavedHandlerContents }, uniqueHandlerId)));
      } else {
        dispatch(alertingActions.handlerContentsChanged(extend({ ...unsavedHandlerContents }, { [uniqueHandlerId]: changedContent })));
      }
    }
  };

  const onSave = () => {
    topicsAPI.updateHandler(handlerContent, (savedHandlerObj) => {
      dispatch(alertingActions.handlerContentsChanged(omit({ ...unsavedHandlerContents }, uniqueHandlerId)));
      setSavedHandlerContent(savedHandlerObj);
    });
  };

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: calc(100vh - 7rem);
          flex-grow: 1;
          align-items: stretch;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          min-width: 760px;
        }
      `}</style>
      {handlerContent ? (
        <HandlerEditorToolbar
          handlerName={handlerContent.id}
          handlerKind={handlerContent.kind}
          topicName={handlerContent.topic}
          readOnly={readOnly}
          isUnsaved={selectedHandlerIsUnsaved}
          onSave={onSave}
        />
      ) : (
        <DefaultToolbar />
      )}
      <HandlerEditor content={handlerContent} onContentChanged={onContentChanged} readOnly={readOnly} availableTopics={availableTopics} />
    </div>
  );
};

HandlerEditorContainer.propTypes = {
  /** Selection object */
  selection: PropTypes.object.isRequired,
  /** Array of available topics */
  availableTopics: PropTypes.array,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
};

HandlerEditorContainer.defaultProps = {
  availableTopics: [],
  readOnly: false,
};

export default HandlerEditorContainer;
