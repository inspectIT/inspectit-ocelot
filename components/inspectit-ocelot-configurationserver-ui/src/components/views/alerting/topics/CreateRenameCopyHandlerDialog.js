import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import classnames from 'classnames';
import { topicIcon } from '../constants';

/**
 * A dialog that can be used for creating, renaming or copying an alert handler.
 */
const CreateRenameCopyHandlerDialog = ({
  topics,
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
  const [topic, setTopic] = useState(undefined);
  const [handlerType, setHandlerType] = useState(undefined);
  const [isValid, setValidState] = useState(false);
  const [error, setError] = useState(undefined);
  const [invalidNames, setInvalidNames] = useState([]);

  useEffect(() => {
    if (topic !== initialTopic) {
      setTopic(initialTopic);
    }
  }, [initialTopic]);

  useEffect(() => {
    if (reservedNames) {
      setInvalidNames(reservedNames);
    } else if (retrieveReservedNames && topic) {
      retrieveReservedNames(topic, (names) => setInvalidNames(names));
    }
  }, [reservedNames, retrieveReservedNames, topic]);

  useEffect(() => {
    if (!handlerType && handlerTypes && handlerTypes.length > 0) {
      setHandlerType(handlerTypes[0]);
    }
  }, [name, JSON.stringify(handlerTypes)]);

  useEffect(() => {
    const reservedName = invalidNames.some((n) => n === name);
    if (reservedName) {
      setError('An alerting rule with the given name already exists');
    } else {
      setError(undefined);
    }
    setValidState(!reservedName && !!name && !!topic && (intention === 'copy' || intention === 'rename' || !!handlerType));
  }, [name, topic, handlerType, JSON.stringify(invalidNames)]);

  const intentionText = intention === 'create' ? 'Create' : intention === 'copy' ? 'Copy' : 'Rename';
  return (
    <Dialog
      className="this"
      style={{ width: '40rem' }}
      header={intentionText + ' Alert Handler'}
      modal={true}
      visible={visible}
      onHide={onHide}
      onShow={() => {
        setName('');
        setValidState(false);
        setError(undefined);
      }}
      footer={
        <div>
          <Button label={intentionText} disabled={!isValid} onClick={() => onSuccess(name, topic, handlerType)} />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      <div style={{ width: '100%', paddingBottom: '0.5em' }}>{intentionText + 'handler'}:</div>
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
  );
};

CreateRenameCopyHandlerDialog.propTypes = {
  /** List of available topics */
  topics: PropTypes.array,
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

CreateRenameCopyHandlerDialog.defaultProps = {
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

export default CreateRenameCopyHandlerDialog;
