import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import classnames from 'classnames';
import { topicIcon } from '../constants';
import useDeepEffect from '../../../../hooks/use-deep-effect';

/**
 * A dialog that can be used for creating, renaming or copying an alert handler.
 */
const CreateEditCopyHandlerDialog = ({
  topics,
  oldName,
  intention,
  initialTopic,
  handlerTypes,
  visible,
  onHide,
  reservedNames,
  retrieveReservedNames,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [topic, setTopic] = useState(null);
  const [handlerType, setHandlerType] = useState(null);
  const [isValid, setValidState] = useState(false);
  const [error, setError] = useState(null);
  const [invalidNames, setInvalidNames] = useState([]);

  useDeepEffect(() => {
    if (topic !== initialTopic) {
      setTopic(initialTopic);
    }
  }, [initialTopic]);

  useDeepEffect(() => {
    if (visible) {
      if (reservedNames) {
        setInvalidNames(reservedNames);
      } else if (retrieveReservedNames && topic) {
        retrieveReservedNames(topic, (names) => setInvalidNames(names));
      }
    }
  }, [reservedNames, retrieveReservedNames, topic]);

  useDeepEffect(() => {
    if (!handlerType && handlerTypes && handlerTypes.length > 0) {
      setHandlerType(handlerTypes[0]);
    }
  }, [name, handlerTypes]);

  useDeepEffect(() => {
    const reservedName = invalidNames.some((n) => n === name);
    let errorMessage;
    if (reservedName) {
      errorMessage = 'An alerting rule with the given name already exists';
    }

    const validName = !!name && validId(name);
    if (!validName) {
      errorMessage = !name ? 'Name must not be empty!' : 'Invalid name! Name must only contain letter, number, _, - or . characters.';
    }

    const validTopic = !!topic && validId(topic);
    if (!validTopic) {
      errorMessage = !topic
        ? 'Topic must not be empty!'
        : 'Invalid topic name! Topic name must only contain letter, number, _, - or . characters.';
    }

    if (errorMessage) {
      setError(errorMessage);
    } else if (error !== null) {
      setError(null);
    }

    const validRenameCondition = !oldName || name !== oldName || initialTopic !== topic;
    const validHandlerTypeCondition = intention !== 'create' || !!handlerType;
    setValidState(!reservedName && validName && validTopic && validRenameCondition && validHandlerTypeCondition);
  }, [name, topic, handlerType, invalidNames]);

  const validId = (str) => {
    const matchResult = str.match(/[\w\-.]*/);
    return matchResult && matchResult[0] === str;
  };

  const intentionText = intention === 'create' ? 'Create' : intention === 'copy' ? 'Copy' : 'Rename';
  return (
    <div className="this">
      <style jsx>{`
        .this :global(.p-dialog-content) {
          overflow-y: visible;
        }
      `}</style>
      <Dialog
        style={{ width: '40rem' }}
        header={intentionText + ' Alert Handler'}
        modal={true}
        visible={visible}
        onHide={onHide}
        onShow={() => {
          setName(oldName || '');
          setValidState(false);
          setError(null);
        }}
        footer={
          <div>
            <Button label={intentionText} disabled={!isValid} onClick={() => onSuccess(name, topic, handlerType)} />
            <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
          </div>
        }
      >
        <div style={{ width: '100%', paddingBottom: '0.5em' }}>{intentionText + ' handler'}:</div>
        <div className="p-grid">
          {intention === 'create' && (
            <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
              <span className="p-inputgroup-addon">
                <i className="pi pi-cog"></i>
              </span>
              <Dropdown
                style={{ width: '100%' }}
                value={handlerType}
                options={handlerTypes && handlerTypes.map((c) => ({ label: c, value: c }))}
                onChange={(e) => {
                  setHandlerType(e.value);
                }}
                placeholder={'Select type'}
              />
            </div>
          )}
          <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
            <span className="p-inputgroup-addon">
              <i className={classnames('pi', topicIcon)}></i>
            </span>
            <Dropdown
              style={{ width: '100%' }}
              value={topic}
              options={topics.map((c) => ({ label: c, value: c }))}
              onChange={(e) => {
                setTopic(e.value);
              }}
              editable={true}
              placeholder={'Select notification channel'}
            />
          </div>
          <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
            <span className="p-inputgroup-addon">
              <i className="pi pi-star"></i>
            </span>
            <InputText
              style={{ width: '100%' }}
              onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, topic, handlerType)}
              value={name}
              placeholder={'Alerting handler name'}
              onChange={(e) => {
                setName(e.target.value);
              }}
            />
          </div>
        </div>
        {error && (
          <div style={{ width: '100%', paddingTop: '0.5em' }}>
            <Message style={{ width: '100%' }} severity="error" text={error}></Message>
          </div>
        )}
      </Dialog>
    </div>
  );
};

CreateEditCopyHandlerDialog.propTypes = {
  /** List of available topics */
  topics: PropTypes.array,
  /** Old name when using the dialog for rename */
  oldName: PropTypes.string,
  /** The intention of this dialog. One of create, rename or copy */
  intention: PropTypes.string,
  /** Initially topic category */
  initialTopic: PropTypes.string,
  /** List of available handler types */
  handlerTypes: PropTypes.array,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** List of reserved names that cannot be used */
  reservedNames: PropTypes.array,
  /** A function that would return a list of reserved names
   *  as an alternative to the plain array. */
  retrieveReservedNames: PropTypes.func,
  /** Callback on creation success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

CreateEditCopyHandlerDialog.defaultProps = {
  topics: [],
  handlerTypes: [],
  initialTopic: '',
  intention: 'create',
  visible: true,
  reservedNames: undefined,
  retrieveReservedNames: undefined,
  onSuccess: () => {},
  onHide: () => {},
};

export default CreateEditCopyHandlerDialog;
