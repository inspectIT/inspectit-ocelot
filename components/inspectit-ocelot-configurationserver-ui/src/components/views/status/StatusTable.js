import React from 'react';
import { connect } from 'react-redux'
import { agentConfigActions } from '../../../redux/ducks/agent-config'

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';

import TimeAgo from 'react-timeago'

const timeFormatter = (time, unit, suffix) => {
    if (unit == "second") {
        const tens = Math.max(10, Math.ceil(time / 10) * 10);
        return "< " + tens + " seconds " + suffix;
    } else {
        return time + " " + unit + (time > 1 ? "s " : " ") + suffix;
    }
}

const AttributesCell = ({ data, onDownload }) => {
    const {attributes} = data
    return(
        <div className='this'>
            <style jsx>{`
                .this :global(.p-button){
                    background: #ddd;
                    border-color: #ddd;
                }
                .this:hover :global(.p-button){
                    
                }
                .this{
                    position: relative;
                }
            `}</style>
            {Object.keys(attributes).sort().map((key) => {
                return (<span key={key}><b>{key}:</b> {attributes[key]}<br /></span>)
            })}
            {showDownloadBtn(data, onDownload)}
        </div>
    )
};

const showDownloadBtn = (data, onDownload) => {
    const {attributes, mappingName} = data
    if(Object.keys(attributes).length <= 0 || mappingName === '<no mapping>'){
        return
    }
    return (
        <Button 
            icon='pi pi-download' 
            onClick={() => onDownload(attributes.attributes ? convertToObj(attributes.attributes) : attributes)} 
            tooltip='Click here to download the configuration file for this mapping'
            style={{width: '1.2rem', height: '1.2rem', position: 'absolute', right: 0, top: 0}}
        />
    )
}

const convertToObj = (string) => {
    let res = {};
    const items = string.split(',');
    items.forEach(pair => {
        const current = pair.split(':');
        res[current[0]] = current[1]
    })
    return res
}

/**
 * The table listing all agent statuses
 */
class StatusTable extends React.Component {

    render() {
        const { agents } = this.props;

        const agentValues = agents.map((agent) => {
            //build a dummy string to allow filtering
            const attributesSearchString =
                Object.keys(agent.attributes).sort().map((key) => {
                    return key + ": " + agent.attributes[key] + "\n"
                });
            return {
                ...agent,
                mappingName: agent.mappingName || "<no mapping>",
                attributesSearchString
            }
        })

        return (
            <DataTable value={agentValues} globalFilter={this.props.filter}>
                <Column header="Attributes" field="attributesSearchString"
                    body={(data) => (<AttributesCell data={data} attributes={data.attributes} onDownload={this.props.downloadConfiguration}/>)} 
                />
                <Column field="mappingName" sortable={true} header="Mapping" />
                <Column field="lastConfigFetch" sortable={true} header="Last Connected" excludeGlobalFilter={true}
                    body={(data) => (<TimeAgo date={data.lastConfigFetch} formatter={timeFormatter} />)}
                />
            </DataTable>
        );
    }
}

function mapStateToProps(state) {
    const { agents } = state.agentStatus;
    return {
        agents
    }
}

const mapDispatchToProps = {
    downloadConfiguration: agentConfigActions.fetchConfigurationFile
}

export default connect(mapStateToProps, mapDispatchToProps)(StatusTable);