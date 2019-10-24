import React from 'react';

import { connect } from 'react-redux';
import { agentConfigActions } from '../../../../redux/ducks/agent-config';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import EditAttributes from '../editComponents/EditAttributes';

import {cloneDeep, isNull} from 'lodash';

/**
 * Dialog for downloading a configuration file.
 */
class DownloadDialog extends React.Component {

    downloadButton = React.createRef();

    state = {
        attributes : {}
    }

	handleDeleteAttribute = (attribute) => {
        let newAttributes = cloneDeep(this.state.attributes);
        delete newAttributes[Object.keys(attribute)[0]];
        this.setState({attributes: newAttributes});
    }

    handleAttributeChange = (oldKey, newKey, newValue) => {
        const {attributes} = this.state;
        let newAttributes = cloneDeep(attributes);
    
        if(oldKey == null && ( newKey || newValue )) {
            if(newKey) {
                newAttributes[newKey] = '';
            } else {
                newAttributes[''] = newValue;
            }
        } else if(!isNull(oldKey)) {
            if(!isNull(newKey)) {
                delete newAttributes[oldKey];
                newAttributes[newKey] = attributes[oldKey];
            } else {
                newAttributes[oldKey] = newValue;
            }
        }
    
        this.setState({attributes: newAttributes});
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
                    onAttributeChange={this.handleAttributeChange}
                    onDeleteAttribute={this.handleDeleteAttribute}
                    attributes={this.state.attributes}
                    maxHeight={`300px`}
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