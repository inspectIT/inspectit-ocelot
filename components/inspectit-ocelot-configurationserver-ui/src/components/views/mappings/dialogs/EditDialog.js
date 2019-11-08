import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../../redux/ducks/mappings';
import { notificationActions } from '../../../../redux/ducks/notification';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Fieldset } from 'primereact/fieldset';

import EditSources from '../editComponents/EditSources'
import KeyValueEditor from '../editComponents/KeyValueEditor';
import { isEqual, findIndex, cloneDeep, isEqualWith } from 'lodash';

const defaultState = {
  name: '',
  sources: [],
  attributes: [],
  isNewMapping: null,
}

/**
 * Dialog for editing the given mapping/creating a new mapping.
 */
class EditMappingDialog extends React.Component {

  state = defaultState;

  handleChangeSources = (newSources) => {
    this.setState({ sources: newSources });
  }

  handleChangeAttribute = (newAttributes) => {
    this.setState({ attributes: newAttributes });
  }

  render() {
    const { name, sources, attributes, isNewMapping } = this.state;
    const heightFieldset = window.innerHeight * 0.35;

    return (
      <div className='this'>
        <style jsx>{`
					.this :global(.p-dialog-content){
						border-left: 1px solid #ddd;
						border-right: 1px solid #ddd;
					}
				`}</style>
        <Dialog
          header={isNewMapping ? 'Add Mapping' : 'Edit Mapping'}
          modal={true}
          visible={this.props.visible}
          onHide={this.handleClose}
          style={{ 'max-width': '1100px', 'min-width': '650px' }}
          footer={(
            <div>
              <Button label={isNewMapping ? 'Add' : 'Update'} className="p-button-primary" onClick={this.handleSave} />
              <Button label="Cancel" className="p-button-secondary" onClick={this.handleClose} />
            </div>
          )}
        >
          <span style={{ display: 'flex', alignItems: 'center' }}>
            <p style={{ width: '9rem' }}>Mapping Name: </p>
            <div className="p-inputgroup" style={{ display: "inline-flex", verticalAlign: "middle", width: '100%' }}>
              <InputText
                placeholder='Enter new name'
                value={name ? name : ''}
                onChange={e => this.setState({ name: e.target.value })}
                style={{ width: '100%' }}
              />
              <span className="pi p-inputgroup-addon pi-pencil" style={{ background: 'inherit', 'border-color': '#656565' }} />
            </div>
          </span>
          <Fieldset legend='Sources' style={{ 'padding-top': 0, 'height': heightFieldset, overflow: 'hidden' }}>
            <EditSources
              visible={this.props.visible}
              sources={sources}
              onChange={this.handleChangeSources}
              maxHeight={`calc(${heightFieldset}px - 3.5em)`}
            />
          </Fieldset>
          <Fieldset legend='Attributes' style={{ 'padding-top': 0, 'height': heightFieldset, overflow: 'hidden' }}>
            <KeyValueEditor
              keyValueArray={attributes}
              onChange={this.handleChangeAttribute}
              maxHeight={`calc(${heightFieldset}px - 6em)`}
            />
          </Fieldset>
        </Dialog>
      </div>
    )
  }

	/**
	 * declares whether a new or old mapping is edited
	 * and converts mapping.attributes into a format used by KeyValueEditor
	 * 
	 * @param {*} nextProps 
	 */
  componentWillReceiveProps(nextProps) {
    if (!nextProps.mapping) {
      this.setState({ isNewMapping: true });
    }
    else {
      this.setState(buildStateObject(nextProps.mapping));
    }
  }

  handleSave = () => {
    if (!this.validateUserInput()) {
      return
    }
    const newMapping = buildMappingObject(this.state);

    if (this.state.isNewMapping) {
      this.props.addMapping(newMapping, this.handleClose);
    } else {
      let newMappings = this.props.mappings.map(mapping => {
        if (isEqualWith(mapping, this.props.mapping, areMappingsEqual)) {
          return newMapping;
        } else {
          return mapping;
        }
      })
      this.props.putMappings(newMappings, this.handleClose);
    }
  }

  /**
   * callback on saving action or closing dialog without saving
   */
  handleClose = (success = true) => {
    if (success) {
      this.props.onHide();
      this.setState({ ...defaultState });
    }
  }

  /**
   * shows a warning and returns false when the user input should not be saved/ dialog not closed
   */
  validateUserInput = () => {
    const { name, attributes } = this.state;
    const { showWarningMessage } = this.props;

    if (!name) {
      showWarningMessage('Mappings Could not be Updated', 'Please enter a name for this mapping');
      return false;
    }

    if (this.props.mapping && this.props.mapping.name !== name && findIndex(this.props.mappings, (mapping) => mapping.name === name) !== -1) {
      showWarningMessage('Mappings Could not be Updated', 'A Mapping with this name already exists');
      return false;
    }

    const keys = attributes.map(pair => pair.key || null).sort()
    for (let i = 0; i < keys.length; i++) {
      if (!keys[i]) {
        showWarningMessage('Mappings Could not be Updated', 'Attribute keys should not be empty');
        return false;
      }
      if (keys[i - 1] === keys[i]) {
        showWarningMessage('Mappings Could not be Updated', 'Attribute keys should be unique');
        return false;
      }
    }

    return true;
  }
}

function mapStateToProps(state) {
  const { mappings } = state.mappings;
  return {
    mappings
  }
}

const mapDispatchToProps = {
  addMapping: mappingsActions.putMapping,
  putMappings: mappingsActions.putMappings,
  showWarningMessage: notificationActions.showWarningMessage,
}

export default connect(mapStateToProps, mapDispatchToProps)(EditMappingDialog);

/**
 * expects two mappings to compare name
 * returns true/false depending on equality
 * ~ needed since this.props.mappings will be retrieved from mappingsView/mappingsTable
 * and mappingsTable modifies the mapping (to enable global filtering of primereact/table) before sending it
 * 
 * @param {mapping} mapping1 
 * @param {mapping} mapping2 
 */
const areMappingsEqual = (mapping1, mapping2) => {
  return isEqual(mapping1.name, mapping2.name)
}

/**
 * expects a mapping object and returns an object usable by this component
 * only attributes are beeing changed - to fit for Key/ValueComponent
 * 
 * @param {object} mapping
 */
export const buildStateObject = (mapping) => {
  const attributeArray = [];
  for (let attKey in mapping.attributes) {
    attributeArray.push(
      {
        key: attKey,
        value: mapping.attributes[attKey]
      }
    )
  }
  return {
    name: mapping.name,
    sources: cloneDeep(mapping.sources),
    attributes: attributeArray,
    isNewMapping: false
  }
}

/**
 * revers funktion to buildStateObject - 
 * expects an object with name,sources & attributes and returns a mapping
 * 
 * @param {object} obj - edit dialog state with name, sources, attributes
 */
export const buildMappingObject = (obj) => {
  if (!obj.name || !obj.attributes || !Array.isArray(obj.attributes)) {
    return;
  }
  let res = {
    name: obj.name,
    sources: cloneDeep(obj.sources) || [],
    attributes: {}
  }
  try {
    obj.attributes.forEach(pair => {
      res.attributes[pair.key || ''] = pair.value || '';
    })
  } finally {
    return res;
  }
}