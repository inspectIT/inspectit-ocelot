import React, { useState } from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { InputTextarea } from 'primereact/inputtextarea';

/**
 * A generic dialog for creating new items.
 * Optionally a category for the items can be used. The category options will be listed in a dropdown box.
 */
const CreateDialog = ({
  categories,
  initialCategory,
  useDescription,
  title,
  categoryTitle,
  elementTitle,
  text,
  visible,
  onHide,
  categoryIcon,
  targetElementIcon,
  reservedNames,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [category, setCategory] = useState(undefined);
  const [description, setDescription] = useState('');
  const [isValid, setValidState] = useState(false);
  const [error, setError] = useState(undefined);

  if (!category && initialCategory) {
    setCategory(initialCategory);
  }

  const validate = () => {
    const reservedName = reservedNames.some((r) => r.id === name);
    if (reservedName) {
      setError('An alerting rule with the given name already exists');
    } else {
      setError(undefined);
    }
    setValidState(!reservedName && !!name && !!category);
  };

  return (
    <Dialog
      className="this"
      style={{ width: '400px' }}
      header={title}
      modal={true}
      visible={visible}
      onHide={onHide}
      onShow={() => {
        setName('');
        setDescription('');
        setCategory(undefined);
      }}
      footer={
        <div>
          <Button label="Create" disabled={!isValid} onClick={() => onSuccess(name, category, description)} />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      <div style={{ width: '100%', paddingBottom: '0.5em' }}>{text}</div>
      <div className="p-grid">
        {categories && (
          <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
            <span className="p-inputgroup-addon">
              <i className={classNames('pi', { [categoryIcon]: categoryIcon !== undefined })}></i>
            </span>
            <Dropdown
              style={{ width: '100%' }}
              value={category}
              options={categories.map((c) => ({ label: c, value: c }))}
              onChange={(e) => {
                setCategory(e.value);
                validate();
              }}
              placeholder={categoryTitle}
            />
          </div>
        )}
        <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
          <span className="p-inputgroup-addon">
            <i className={classNames('pi', { [targetElementIcon]: targetElementIcon !== undefined })}></i>
          </span>
          <InputText
            style={{ width: '100%' }}
            onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, category, description)}
            value={name}
            placeholder={elementTitle}
            onChange={(e) => {
              setName(e.target.value);
              validate();
            }}
          />
        </div>
        {useDescription && (
          <div className="p-inputgroup p-col-12" style={{ width: '100%' }}>
            <span className="p-inputgroup-addon">
              <i className="pi pi-align-left"></i>
            </span>
            <InputTextarea
              style={{ width: '100%', height: '8rem' }}
              value={description}
              onKeyPress={(e) => e.key === 'Enter' && isValid && onSuccess(name, category, description)}
              autoResize={false}
              placeholder={'Description'}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
        )}
      </div>
      {error && (
        <div style={{ width: '100%', paddingTop: '0.5em' }}>
          <Message style={{ width: '100%' }} severity="error" text={error}></Message>
        </div>
      )}
    </Dialog>
  );
};

CreateDialog.propTypes = {
  /** List of category options */
  categories: PropTypes.array,
  /** Initially selected category */
  initialCategory: PropTypes.string,
  /** Whether the description field should be used */
  useDescription: PropTypes.bool,
  /** The title of the dialog */
  title: PropTypes.string.isRequired,
  /** The title for category selection */
  categoryTitle: PropTypes.string,
  /** The title for element definition */
  elementTitle: PropTypes.string.isRequired,
  /** The text to show in the dialog */
  text: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** The prime react icon name for the category */
  categoryIcon: PropTypes.string,
  /** The prime react icon name for the element */
  targetElementIcon: PropTypes.string,
  /** List of reserved names that cannot be used */
  reservedNames: PropTypes.array,
  /** Callback on creation success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

CreateDialog.defaultProps = {
  categories: [],
  initialCategory: '',
  useDescription: false,
  categoryTitle: 'Category',
  text: 'Create element',
  visible: true,
  categoryIcon: 'pi-circle-on',
  targetElementIcon: 'pi-circle-on',
  reservedNames: [],
  onSuccess: () => {},
  onHide: () => {},
};

export default CreateDialog;
