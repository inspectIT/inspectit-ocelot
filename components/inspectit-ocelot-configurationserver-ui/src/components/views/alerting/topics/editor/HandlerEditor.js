import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { omit, isEqual, extend, cloneDeep } from 'lodash';
import { connect } from 'react-redux';
import HandlerEditorToolbar from './HandlerEditorToolbar';
import SelectionInformation from '../../../../editor/SelectionInformation';
import ListEditor from '../../../../common/value-editors/ListEditor';
import Section from '../../Section';
import VariableView from '../../rules/editor/VariableView';
import * as topicsAPI from '../TopicsAPI';
import { alertingActions } from '../../../../../redux/ducks/alerting';
import * as handlerUtils from '../handlerUtils';
import * as alertingConstants from '../../constants';

const HandlerEditor = ({
  selectedHandlerName,
  selectedTopicName,
  readOnly,
  availableTopics,
  unsavedHandlerContents,
  unsavedHandlerContentsChanged,
}) => {
  const [selectedObject, setSelectedObject] = useState(undefined);
  const [savedHandlerContent, setSavedHandlerContent] = useState(undefined);

  useEffect(() => {
    if (selectedHandlerName && selectedTopicName) {
      topicsAPI.fetchHandler(selectedTopicName, selectedHandlerName, (handler) => setSavedHandlerContent(handler));
    } else {
      setSavedHandlerContent(undefined);
    }
  }, [selectedHandlerName, selectedTopicName]);

  useEffect(() => {
    if (selectedHandlerName && selectedTopicName) {
      if (handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName) in unsavedHandlerContents) {
        setSelectedObject(unsavedHandlerContents[handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName)]);
      } else if (savedHandlerContent) {
        setSelectedObject(savedHandlerContent);
      }
    } else if (selectedTopicName) {
      topicsAPI.fetchTopic(selectedTopicName, (topic) => setSelectedObject(topic));
    } else {
      setSelectedObject(undefined);
    }
  }, [savedHandlerContent, selectedHandlerName, selectedTopicName, unsavedHandlerContents]);

  const onContentChanged = (changedContent) => {
    const handlerId = handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName);
    if (savedHandlerContent && isEqual(savedHandlerContent, changedContent)) {
      unsavedHandlerContentsChanged(omit(cloneDeep(unsavedHandlerContents), handlerId));
    } else {
      unsavedHandlerContentsChanged(extend(cloneDeep(unsavedHandlerContents), { [handlerId]: changedContent }));
    }
  };

  const selectedHandlerIsUnsaved =
    selectedHandlerName &&
    unsavedHandlerContents &&
    handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName) in unsavedHandlerContents;

  return (
    <div className="this">
      <style jsx>{`
        .this {
          max-height: 100%;
          flex-grow: 1;
          align-items: stretch;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          min-width: 760px;
        }
        .this :global(.entries-view) {
          max-height: 30rem;
        }
        .this :global(.contentContainer) {
          overflow-y: auto;
        }
      `}</style>
      <HandlerEditorToolbar
        selectedHandlerName={selectedHandlerName}
        selectedHandlerKind={selectedHandlerName && selectedObject && selectedObject.kind}
        selectedTopicName={selectedTopicName}
        readOnly={readOnly}
        isUnsaved={selectedHandlerIsUnsaved}
        onSave={() => {
          topicsAPI.updateHandler(selectedObject, (savedHandlerObj) => {
            const handlerId = handlerUtils.uniqueHandlerId(selectedHandlerName, selectedTopicName);
            unsavedHandlerContentsChanged(omit(cloneDeep(unsavedHandlerContents), handlerId));
            setSavedHandlerContent(savedHandlerObj);
          });
        }}
      />
      <div className="contentContainer">
        {!selectedObject && <SelectionInformation hint="Select an alerting handler to start editing." />}
        {!!selectedHandlerName && !!selectedObject && (
          <>
            <HandlerSpecificOptions
              selectedObject={selectedObject}
              readOnly={readOnly}
              availableTopics={availableTopics}
              onContentChanged={onContentChanged}
            />
            <Section title="Advanced Event Matching">
              <VariableView
                key={selectedTopicName + selectedHandlerName + '- matcher'}
                name={'Matcher expression'}
                description={
                  <span>
                    {'The matcher expressions filters events before they are send to this handler.'}
                    <br />
                    {'Options are: level(), changed() and tag matching.'}
                    <br />
                    {'Example: (level() >= WARNING) AND ("\\"host\\" == \'s001.example.com\'")'}
                    <br />
                    {
                      "In this example only events affecting the host 's001.example.com' with severity level WARNING or higher will be sent."
                    }
                  </span>
                }
                value={selectedObject.match || 'Not defined'}
                type={'string'}
                readOnly={readOnly}
                isDefault={!selectedObject.match}
                onVarUpdate={(name, newValue) => {
                  const newObject = cloneDeep(selectedObject);
                  newObject.match = newValue ? newValue : undefined;
                  onContentChanged(newObject);
                }}
              />
            </Section>
          </>
        )}
      </div>
    </div>
  );
};

const HandlerSpecificOptions = ({ selectedObject, readOnly, availableTopics, onContentChanged }) => {
  if (!selectedObject || !selectedObject.kind) {
    return <></>;
  }
  const handlerKind = selectedObject.kind;
  switch (handlerKind) {
    case 'smtp':
      return (
        <Section title="Receipients">
          <ListEditor
            value={selectedObject.options && selectedObject.options.to ? selectedObject.options.to : []}
            disabled={readOnly}
            entryIcon={alertingConstants.handlerIcons(handlerKind)}
            entryWidth="30rem"
            validateEntryFunc={(entry) => {
              const mailformat = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
              const matchResult = entry.match(mailformat);
              return matchResult && matchResult[0] === entry;
            }}
            updateValue={(newValue) => {
              const newObject = cloneDeep(selectedObject);
              if (newObject.options === undefined) {
                newObject.options = {};
              }
              newObject.options.to = newValue;
              onContentChanged(newObject);
            }}
          />
        </Section>
      );
    case 'publish':
      return (
        <Section title="Target Topics">
          <ListEditor
            value={selectedObject.options && selectedObject.options.topics ? selectedObject.options.topics : []}
            options={availableTopics ? availableTopics.map((t) => t.id) : undefined}
            disabled={readOnly}
            entryIcon={alertingConstants.handlerIcons(handlerKind)}
            entryWidth="20rem"
            separators={/;|,/}
            updateValue={(newValue) => {
              const newObject = cloneDeep(selectedObject);
              if (newObject.options === undefined) {
                newObject.options = {};
              }
              newObject.options.topics = newValue;
              onContentChanged(newObject);
            }}
          />
        </Section>
      );
    default:
      return <></>;
  }
};

HandlerEditor.propTypes = {
  /** Name of the selected handler */
  selectedHandlerName: PropTypes.string,
  /** Name of the selected topic */
  selectedTopicName: PropTypes.string,
  /** Array of available topics */
  availableTopics: PropTypes.array,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** A map of handler names to corresponding unsaved contents */
  unsavedHandlerContents: PropTypes.object,
  /** Callback on content update */
  unsavedHandlerContentsChanged: PropTypes.func,
};

HandlerEditor.defaultProps = {
  selectedHandlerName: undefined,
  selectedTopicName: undefined,
  availableTopics: [],
  readOnly: false,
  unsavedHandlerContents: {},
  unsavedHandlerContentsChanged: () => {},
};

const mapStateToProps = (state) => {
  const { unsavedHandlerContents } = state.alerting;

  return {
    unsavedHandlerContents,
  };
};

const mapDispatchToProps = {
  unsavedHandlerContentsChanged: alertingActions.handlerContentsChanged,
};

export default connect(mapStateToProps, mapDispatchToProps)(HandlerEditor);
