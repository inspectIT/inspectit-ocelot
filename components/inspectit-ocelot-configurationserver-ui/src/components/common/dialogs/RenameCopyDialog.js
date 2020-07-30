import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Message } from 'primereact/message';

/**
 * A generic Dialog for renaming or copying the given element.
 */
const RenameCopyDialog = ({ name, reservedNames, retrieveReservedNames, visible, onHide, text, onSuccess, intention, validateName }) => {
  const [newName, setName] = useState('');
  const [isValid, setValidState] = useState(false);
  const [error, setError] = useState(null);
  const [invalidNames, setInvalidNames] = useState([]);

  useEffect(() => {
    if (reservedNames) {
      setInvalidNames(reservedNames);
    } else if (retrieveReservedNames) {
      retrieveReservedNames((names) => setInvalidNames(names));
    }
  }, [reservedNames, retrieveReservedNames]);

  useEffect(() => {
    const reservedName = invalidNames.some((n) => n === newName);
    let errorMessage;
    if (reservedName) {
      errorMessage = 'An element with the given name already exists!';
    }

    const validName = !!newName && validateName(newName);
    if (!validName) {
      errorMessage = !newName ? 'Name must not be empty!' : 'Name contains invalid characters!';
    }

    if (errorMessage) {
      setError(errorMessage);
    } else if (error !== null) {
      setError(null);
    }
    setValidState(!reservedName && validName && name !== newName);
  }, [newName]);

  return (
    <Dialog
      className="this"
      style={{ width: '400px' }}
      header={(intention === 'copy' ? 'Copy' : 'Rename') + ' Element'}
      modal={true}
      visible={visible}
      onHide={onHide}
      onShow={() => {
        setName(name);
        setValidState(false);
        setError(undefined);
      }}
      footer={
        <div>
          <Button label={intention === 'copy' ? 'Copy' : 'Rename'} disabled={!isValid} onClick={() => onSuccess(name, newName)} />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      <div style={{ width: '100%', paddingBottom: '0.5em' }}>{text}</div>
      <div className="p-grid">
        <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
          <span className="p-inputgroup-addon">
            <i className="pi pi-pencil"></i>
          </span>
          <InputText
            style={{ width: '100%' }}
            onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, newName)}
            value={newName}
            autoFocus
            placeholder="New Name"
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

RenameCopyDialog.propTypes = {
  /** The name of the element to delete */
  name: PropTypes.string,
  /** A list of reserved name (optional) */
  reservedNames: PropTypes.array,
  /** A function returning a list of reserved names as alternative to passing the reserved names. */
  retrieveReservedNames: PropTypes.func,
  /** The text to show in the dialog */
  text: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** The intention of the dialog (copy or rename) */
  intention: PropTypes.string,
  /** Callback on deletion success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  validateName: PropTypes.func,
};

RenameCopyDialog.defaultProps = {
  text: 'Delete element',
  visible: true,
  onSuccess: () => {},
  onHide: () => {},
  validateName: () => true,
};

export default RenameCopyDialog;
