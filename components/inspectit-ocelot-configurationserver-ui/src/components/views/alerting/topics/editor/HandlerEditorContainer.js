import React from 'react';
import PropTypes from 'prop-types';
import { omit, isEqual, extend } from 'lodash';
import { useSelector, useDispatch } from 'react-redux';
import HandlerEditorToolbar from './HandlerEditorToolbar';
import { updateHandler } from '../../alerting-api';
import { alertingActions } from '../../../../../redux/ducks/alerting';
import * as handlerUtils from '../handlerUtils';
import DefaultToolbar from '../../DefaultToolbar';
import HandlerEditor from './HandlerEditor';
import SelectionInformation from '../../../../editor/SelectionInformation';
import { notificationActions } from '../../../../../redux/ducks/notification';

const HandlerEditorContainer = ({ selection, savedHandlerContent, readOnly, availableTopics, onSaved }) => {
  const dispatch = useDispatch();

  // global state variables
  const unsavedHandlerContents = useSelector((state) => state.alerting.unsavedHandlerContents);

  const uniqueHandlerId = handlerUtils.uniqueHandlerId(selection.handler, selection.topic);
  const selectedHandlerIsUnsaved = unsavedHandlerContents && uniqueHandlerId && uniqueHandlerId in unsavedHandlerContents;
  const handlerContent = selectedHandlerIsUnsaved ? unsavedHandlerContents[uniqueHandlerId] : savedHandlerContent;
  const unsupportedHandlerIsSelected = selection.handler && !selection.isSupportedHandlerKind;

  const onContentChanged = (changedContent) => {
    if (selection.handler) {
      if (savedHandlerContent && isEqual(savedHandlerContent, changedContent)) {
        dispatch(alertingActions.handlerContentsChanged(omit({ ...unsavedHandlerContents }, uniqueHandlerId)));
      } else {
        dispatch(alertingActions.handlerContentsChanged(extend({ ...unsavedHandlerContents }, { [uniqueHandlerId]: changedContent })));
      }
    }
  };

  const onSave = async () => {
    try {
      await updateHandler(handlerContent.topic, handlerContent);
      await onSaved();
      dispatch(alertingActions.handlerContentsChanged(omit({ ...unsavedHandlerContents }, uniqueHandlerId)));
    } catch (error) {
      dispatch(notificationActions.showErrorMessage('Failed saving alerting handler', ''));
    }
  };

  let toolbar = <DefaultToolbar />;

  if (unsupportedHandlerIsSelected) {
    toolbar = <HandlerEditorToolbar handlerName={selection.handler} topicName={selection.topic} readOnly={true} isUnsaved={false} />;
  } else if (handlerContent) {
    toolbar = (
      <HandlerEditorToolbar
        handlerName={handlerContent.id}
        handlerKind={handlerContent.kind}
        topicName={handlerContent.topic}
        readOnly={readOnly}
        isUnsaved={selectedHandlerIsUnsaved}
        onSave={onSave}
      />
    );
  }
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
      {toolbar}
      {unsupportedHandlerIsSelected ? (
        <SelectionInformation hint="Selected handler is of an unsupported kind!" />
      ) : (
        <HandlerEditor content={handlerContent} onContentChanged={onContentChanged} readOnly={readOnly} availableTopics={availableTopics} />
      )}
    </div>
  );
};

HandlerEditorContainer.propTypes = {
  /** Selection object */
  selection: PropTypes.object.isRequired,
  /** saved handler content */
  savedHandlerContent: PropTypes.object,
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
