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
import {cloneDeep, isEqual} from 'lodash';

/**
 * Dialog for editing the given mapping.
 */

const defaultState = {
	// includes mapping.name, mapping.sources, mapping.attributes
	name: '',
	sources: [],
	attributes: [],
	isNewMapping: null,
}

class EditMappingDialog extends React.Component {

	constructor(props){
		super(props);
		this.state = defaultState;
	}

	handleAddSource = (newSource) => {
		let sourceCopy = cloneDeep(this.state.sources) || [];
		sourceCopy.unshift(newSource);
		this.setState({ sources: sourceCopy });
  }

	handleUpdateSources = (newSources) => this.setState({ sources: newSources });

	handleChangeAttribute = (newAttributes) => {
		this.setState({attributes: newAttributes});
	}

  render() {
		const {name, sources, attributes, isNewMapping} = this.state;
		const maxHeightFieldset = window.innerHeight * 0.45;

    return (
			<div className='this'>
				<style jsx>{`
					.this :global(.p-dialog-content){
						// padding: 0.25em 0em 0em 0em;
						border-left: 1px solid #ddd;
						border-right: 1px solid #ddd;
					}
				`}</style>
				<Dialog
					header={isNewMapping ? 'Add Mapping' : 'Edit Mapping'}
					modal={true}
					visible={this.props.visible}
					onHide={this.handleClose}
					style={{'max-width': '1100px', 'min-width': '650px'}}
					footer={(
						<div>
							<Button label={isNewMapping ? 'Add' : 'Update'} className="p-button-primary" onClick={this.handleClick} />
							<Button label="Cancel" className="p-button-secondary" onClick={this.handleClose} />
						</div>
					)}
				>
					<span style={{display: 'flex', alignItems: 'center'}}>
						<p style={{width: '9rem'}}>Mapping Name: </p>
						<div className="p-inputgroup" style = {{display: "inline-flex", verticalAlign: "middle", width: '100%' }}>
								<InputText 
									placeholder='Enter new name'
									value={name ? name : ''} 
									onChange={e => this.setState({name: e.target.value}) }
									style={{width: '100%'}} 
								/>
								<span className="pi p-inputgroup-addon pi-pencil" style={{background: 'inherit', 'border-color': '#656565'}}/>
						</div>
					</span>
					<Fieldset legend='Sources' style={{'padding-top': 0, 'max-height': maxHeightFieldset, overflow: 'hidden'}}>
						<EditSources
							visible={this.props.visible}
							sources={sources} 
							onUpdateAllSources={this.handleUpdateSources}
							onAddSource={this.handleAddSource}
							maxHeight={`calc(${maxHeightFieldset}px - 10em)`}
						/> 
					</Fieldset>
					<Fieldset legend='Attributes' style={{'padding-top': 0, 'max-height': maxHeightFieldset, overflow: 'hidden'}}>
					<KeyValueEditor 
						keyValueArray={attributes}
						onChange={this.handleChangeAttribute}
						maxHeight={`calc(${maxHeightFieldset}px - 10em)`}
					/>
					</Fieldset>
				</Dialog>
			</div>
    )
	}
	
	componentWillReceiveProps(nextProps){
		if(!nextProps.mapping.name){
			this.setState({ isNewMapping: true})
		}
		else {
			const newAttributeArray = []
			for (let attKey in nextProps.mapping.attributes) {
				newAttributeArray.push({
					key: attKey,
					value: nextProps.mapping.attributes[attKey]
				})
			}
			this.setState({name: nextProps.mapping.name, sources: cloneDeep(nextProps.mapping.sources), attributes: newAttributeArray, isNewMapping: false});
		}
	}

  handleClick = () => {
		const {name, sources, attributes, isNewMapping} = this.state;

		if(!name  || (this.props.mapping.name !== name && checkIfNameAlreadyExists(this.props.mappings, name)) ){
			const msg = !name ? 'Enter a name for your mapping' : 'A Mapping with this name already exists';
			this.props.showWarningMessage('Mappings Could not be Updated', msg);
			return	
		} 

		const attributesObjToSave = {};
    attributes.forEach(pair => {
			attributesObjToSave[pair.key || ''] = pair.value || '';
		})
		if(Object.keys(attributesObjToSave).length !== attributes.length){
			this.props.showWarningMessage('Invalid Input', 'Certein attribute keys were duplicates and have been omitted for saving.')
		}
		
		if(isNewMapping){
			this.props.addMapping({name: name, sources: sources, attributes: attributesObjToSave}, this.handleClose)
		} else{
			let newMappings = cloneDeep(this.props.mappings);
    	let indexToUpdate;
    	newMappings.forEach((element, index) => {
      	if(isEqual(element, this.props.mapping)) { 
					indexToUpdate = index; 
				}
    	});
			newMappings.splice(indexToUpdate, 1, {name: name, sources: sources, attributes: attributesObjToSave });
			this.props.putMappings(newMappings, this.handleClose)
		}
	}
	
	handleClose = (success = true) => {
		if(success){
			this.props.onHide();
			this.setState({...defaultState});
		}
	}
}

const checkIfNameAlreadyExists = (allMappings, newName) => {
	let res
	allMappings.forEach(mapping => {
		if(mapping.name === newName){
			res = true
		}
	})
	return res
}

function mapStateToProps(state) {
  const {mappings} = state.mappings;
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