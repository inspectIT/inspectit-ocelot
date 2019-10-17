import React from 'react'

import { connect } from 'react-redux'
import { agentConfigActions } from '../../../../redux/ducks/agent-config'

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import EditAttributes from '../editComponents/EditAttributes';

import {cloneDeep} from 'lodash';

/**
 * Dialog for downloading a configuration file.
 */
class DownloadDialog extends React.Component {

    downloadButton = React.createRef();

    state = {
        attributes : {}
    }

    handleChangeAttributeKey = (oldKey, newKey) => {
		if (oldKey === newKey){
            return
        }
        
        const {attributes} = this.state;
        let newAttributes = {}
        if(!oldKey){
            newAttributes = cloneDeep(attributes)
            if(!newAttributes){
				newAttributes = {}
			}
            newAttributes[newKey] = 'value'
        } else {
            const oldKeys = Object.keys(attributes);
            oldKeys.forEach(key => {
                if(oldKey === key && !newAttributes[newKey]){
                    newAttributes[newKey] = attributes[key]
                } else if(oldKey !== key && !newAttributes[oldKey]){
                    newAttributes[key] = attributes[key]
                }
            })
        }

        this.setState({ attributes: newAttributes });
	}
	
	handleChangeValueOrAddAttribute = (key, value) => this.setState({ attributes: {...this.state.attributes, [key]: value} });

	handleDeleteAttribute = (attribute) => {
        let newAttributes = cloneDeep(this.state.attributes)
        delete newAttributes[Object.keys(attribute)[0]]
        this.setState({attributes: newAttributes})
    }

    render() {
        return (
            <Dialog
                header={'Download Configuration File'}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                style={{'max-width': '900px'}}
                footer={(
                    <div>
                        <Button label="Download" ref={this.downloadButton} className="p-button-primary" onClick={this.handleDownload} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                <p style={{'margin-bottom': '1em'}}>
                    Enter key/value pairs to download the correlating agent configuration. 
                    <br/> You will get a different result depending on the mapping that fits your input.
                </p>
                <EditAttributes 
                    onChValueOrAddAttribute={this.handleChangeValueOrAddAttribute}
                    onChangeAttributeKey={this.handleChangeAttributeKey}
                    onDeleteAttribute={this.handleDeleteAttribute}
                    attributes={this.state.attributes}
                    height={200}
                />
            </Dialog>
        )
    }

    handleDownload = () => {
        this.props.onHide();
        this.props.downloadConfiguration(this.state.attributes);
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.downloadButton.current.element.focus();
        }
    }

}

const mapDispatchToProps = {
    downloadConfiguration: agentConfigActions.fetchConfigurationFile
}

export default connect(null, mapDispatchToProps)(DownloadDialog);