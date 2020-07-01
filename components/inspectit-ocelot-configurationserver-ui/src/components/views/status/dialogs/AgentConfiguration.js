import React from 'react';
import { Dialog } from 'primereact/dialog';
import { connect } from 'react-redux';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';
import ConfigurationDownload from '../../mappings/ConfigurationDownload';
import axios from '../../../../lib/axios-api';

class AgentConfiguration extends React.Component {

    state = {
        configurationValue: "",
    }

    constructor(props) {
        super(props);
        this.getConfiguration(this.props.attributes);
    }

    configDownload = React.createRef();

    downloadConfiguration = (attributes) => {
        this.configDownload.download(attributes);

    };

    getConfiguration = (attributes) => {
        const requestParams = this.solveRegexValues(attributes);
        if (!requestParams) {
            return;
        }

        axios
            .get('/agent/configuration', {
                params: { ...requestParams },
            })
            .then((res) => {
                this.setState({
                    configurationValue: res.data,
                })
            })
            .catch(() => {
                return "error"
            });
    };


    download = () => {

        var blob = new Blob([this.state.configurationValue], { type: 'text/x-yaml' });
        this.url = window.URL.createObjectURL(blob);

        return this.url;
    }


    solveRegexValues = (obj = {}) => {
        try {
            let res = {};
            Object.keys(obj).map((key) => {
                const randexp = new RandExp(obj[key]);
                randexp.max = randexp.min || 0;
                res[key] = randexp.gen();
            });
            return res;
        } catch {
            this.props.showInfoMessage('Downloading Config File Failed', 'The given attribute values are not a regular expression');
            return null;
        }
    };

    render() {

        return (
            <Dialog
                header={'Agent Configuration'}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={
                    <div>
                        <a href={this.download()} download="agent-config.yml">
                            <Button label="Download" className="p-button-primary" />
                        </a>
                        <Button label="Cancel" className="p-button-secondary" onClick={this.handleClose} />
                    </div>
                }
            >
                <SyntaxHighlighter language="javascript" style={tomorrowNightBlue}>
                    {this.state.configurationValue}
                </SyntaxHighlighter>
                <ConfigurationDownload onRef={(ref) => (this.configDownload = ref)} />
            </Dialog>
        );
    }

    /**
     * Closing dialog.
     */
    handleClose = (success = true) => {
        if (success) {
            this.props.onHide();
        }
    };
}

const mapDispatchToProps = {

};

export default connect(null, mapDispatchToProps)(AgentConfiguration);