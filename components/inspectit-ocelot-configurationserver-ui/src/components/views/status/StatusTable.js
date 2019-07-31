import React from 'react';
import { connect } from 'react-redux'
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

import TimeAgo from 'react-timeago'

const timeFormatter = (time, unit, suffix) => {
    if (unit == "second") {
        const tens = Math.max(10, Math.ceil(time / 10) * 10);
        return "< " + tens + " seconds " + suffix;
    } else {
        return time + " " + unit + (time > 1 ? "s " : " ") + suffix;
    }
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
                attributesSearchString
            }
        })

        return (
            <DataTable value={agentValues} globalFilter={this.props.filter}>
                <Column header="Attributes" field="attributesSearchString"
                    body={(data) => this.buildAttributesPresentation(data.attributes)} />
                <Column field="mappingName" sortable={true} header="Mapping" />
                <Column field="lastConfigFetch" sortable={true} header="Last Connected" excludeGlobalFilter={true}
                    body={(data) => (<TimeAgo date={data.lastConfigFetch} formatter={timeFormatter} />)}
                />
            </DataTable>
        );
    }

    buildAttributesPresentation = (attributes) => {
        return (
            <div>
                {Object.keys(attributes).sort().map((key) => {
                    return (<span key={key}><b>{key}:</b> {attributes[key]}<br /></span>)
                })}
            </div>
        )
    }
}

function mapStateToProps(state) {
    const { agents } = state.agentStatus;
    return {
        agents
    }
}

export default connect(mapStateToProps)(StatusTable);