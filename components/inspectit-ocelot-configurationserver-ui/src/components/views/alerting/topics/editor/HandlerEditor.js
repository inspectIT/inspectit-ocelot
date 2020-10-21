import React from 'react';
import PropTypes from 'prop-types';
import { cloneDeep, isNil } from 'lodash';
import SelectionInformation from '../../../../editor/SelectionInformation';
import ListEditor from '../../../../common/value-editors/ListEditor';
import Section from '../../Section';
import VariableView from '../../rules/editor/VariableView';
import * as alertingConstants from '../../constants';

const HandlerEditor = ({ content, onContentChanged, readOnly, availableTopics }) => {
  if (!content) {
    return <SelectionInformation hint="Select an alerting handler to start editing." />;
  }

  return (
    <div className="this">
      <style jsx>{`
        .this {
          overflow-y: auto;
        }
      `}</style>
      <HandlerSpecificOptions content={content} readOnly={readOnly} availableTopics={availableTopics} onContentChanged={onContentChanged} />
      <Section title="Advanced Event Matching">
        <VariableView
          key={content.topic + '-' + content.id + '- matcher'}
          name={'Matcher expression'}
          description={
            <span>
              {'The matcher expressions filters events before they are send to this handler.'}
              <br />
              {'Options are: level(), changed() and tag matching.'}
              <br />
              {'Example: (level() >= WARNING) AND ("host" == \'s001.example.com\')'}
              <br />
              {"In this example only events affecting the host 's001.example.com' with severity level WARNING or higher will be sent."}
            </span>
          }
          value={content.match || 'Not defined'}
          type={'string'}
          readOnly={readOnly}
          isDefault={!content.match}
          onVarUpdate={(name, newValue) => {
            const newObject = cloneDeep(content);
            newObject.match = newValue ? newValue : '';
            onContentChanged(newObject);
          }}
        />
      </Section>
    </div>
  );
};

const HandlerSpecificOptions = ({ content, readOnly, availableTopics, onContentChanged }) => {
  if (!content.kind) {
    return <></>;
  }
  const handlerKind = content.kind;
  switch (handlerKind) {
    case 'smtp':
      return (
        <Section title="Receipients">
          <ListEditor
            compId={content.topic + '-' + content.id}
            value={content.options && content.options.to ? content.options.to : []}
            disabled={readOnly}
            entryIcon={alertingConstants.handlerIcons(handlerKind)}
            entryWidth="30rem"
            validateEntryFunc={(entry) => {
              const mailformat = /^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/;
              const matchResult = entry.match(mailformat);
              return matchResult && matchResult[0] === entry;
            }}
            updateValue={(newValue) => {
              const newObject = cloneDeep(content);
              if (isNil(newObject.options)) {
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
            compId={content.topic + '-' + content.id}
            value={content.options && content.options.topics ? content.options.topics : []}
            options={availableTopics ? availableTopics.map((t) => t.id) : null}
            disabled={readOnly}
            entryIcon={alertingConstants.handlerIcons(handlerKind)}
            entryWidth="20rem"
            separators={/;|,/}
            updateValue={(newValue) => {
              const newObject = cloneDeep(content);
              if (isNil(newObject.options)) {
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
  /** Content of the selected handler */
  content: PropTypes.object,
  onContentChanged: PropTypes.func,
  /** Array of available topics */
  availableTopics: PropTypes.array,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
};

HandlerEditor.defaultProps = {
  content: undefined,
  onContentChanged: () => {},
  availableTopics: [],
  readOnly: false,
};

export default HandlerEditor;
