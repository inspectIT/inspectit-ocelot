import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { InputTextarea } from 'primereact/inputtextarea';
import classnames from 'classnames';
import { ruleIcon, templateIcon } from '../../constants';

/**
 * A dialog for creating new alerting rules.
 */
const CreateRuleDialog = ({ templates, initialTemplate, visible, onHide, reservedNames, onSuccess }) => {
  const [name, setName] = useState('');
  const [template, setTemplate] = useState(undefined);
  const [description, setDescription] = useState('');
  const [isValid, setValidState] = useState(false);
  const [error, setError] = useState(undefined);

  useEffect(() => {
    if (template !== initialTemplate) {
      setTemplate(initialTemplate);
    }
  }, [initialTemplate]);

  useEffect(() => {
    const reservedName = reservedNames.some((n) => n === name);
    if (reservedName) {
      setError('An alerting rule with the given name already exists');
    } else {
      setError(undefined);
    }
    setValidState(!reservedName && !!name && !!template);
  }, [name, template, JSON.stringify(reservedNames)]);

  return (
    <Dialog
      className="this"
      style={{ maxWidth: '40rem' }}
      header={'Create Alerting Rule'}
      modal={true}
      visible={visible}
      onHide={onHide}
      onShow={() => {
        setName('');
        setDescription('');
        setValidState(false);
        setError(undefined);
      }}
      footer={
        <div>
          <Button label="Create" disabled={!isValid} onClick={() => onSuccess(name, template, description)} />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      <div style={{ width: '100%', paddingBottom: '0.5em' }}>Create alerting rule:</div>
      <div className="p-grid">
        <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
          <span className="p-inputgroup-addon">
            <i className={classnames('pi', templateIcon)}></i>
          </span>
          <Dropdown
            style={{ width: '100%' }}
            value={template}
            options={templates.map((c) => ({ label: c, value: c }))}
            onChange={(e) => {
              setTemplate(e.value);
            }}
            placeholder={'Template'}
          />
        </div>
        <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
          <span className="p-inputgroup-addon">
            <i className={classnames('pi', ruleIcon)}></i>
          </span>
          <InputText
            style={{ width: '100%' }}
            onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, template, description)}
            value={name}
            placeholder={'Rule name'}
            onChange={(e) => {
              setName(e.target.value);
            }}
          />
        </div>
        <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
          <span className="p-inputgroup-addon">
            <i className="pi pi-align-left"></i>
          </span>
          <InputTextarea
            style={{ width: '100%', height: '8rem' }}
            value={description}
            onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, template, description)}
            autoResize={false}
            placeholder={'Description'}
            onChange={(e) => setDescription(e.target.value)}
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

CreateRuleDialog.propTypes = {
  /** List of template options */
  templates: PropTypes.array,
  /** Initially selected template */
  initialTemplate: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** List of reserved names that cannot be used */
  reservedNames: PropTypes.array,
  /** Callback on creation success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

CreateRuleDialog.defaultProps = {
  templates: [],
  initialTemplate: '',
  visible: true,
  reservedNames: [],
  onSuccess: () => { },
  onHide: () => { },
};

export default CreateRuleDialog;
