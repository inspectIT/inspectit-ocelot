import React from 'react';
import { connect } from 'react-redux';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';

import TimeAgo from 'react-timeago';
import ConfigurationDownload from '../mappings/ConfigurationDownload';

const timeFormatter = (time, unit, suffix) => {
  if (unit == "second") {
    const tens = Math.max(10, Math.ceil(time / 10) * 10);
    return "< " + tens + " seconds " + suffix;
  } else {
    return time + " " + unit + (time > 1 ? "s " : " ") + suffix;
  }
}

const AttributesCell = ({ attributes, onDownload }) => {
  return (
    <div className='this'>
      <style jsx>{`
                .this :global(.p-button){
                    background: #ddd;
                    border-color: #ddd;
                }
                .this{
                    position: relative;
                    min-height: 1.2rem;
                }
            `}</style>
      {Object.keys(attributes).sort().map((key) => {
        return (<span key={key}><b>{key}:</b> {attributes[key]}<br /></span>)
      })}
      {onDownload ? buildDownloadButton(attributes, onDownload) : ''}
    </div>
  )
};

const buildDownloadButton = (attributes, onDownload) => {
  return (
    <Button
      icon='pi pi-download'
      onClick={() => onDownload(attributes)}
      tooltip='Click here to download the configuration file for this agent'
      style={{ width: '1.2rem', height: '1.2rem', position: 'absolute', right: 0, top: 0 }}
    />
  )
}

/**
 * The table listing all agent statuses
 */
class StatusTable extends React.Component {
  configDownload = React.createRef();

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
      <div>
        <DataTable value={agentValues} globalFilter={this.props.filter}>
          <Column header="Attributes" field="attributesSearchString"
            body={(data) => (<AttributesCell attributes={data.attributes} onDownload={data.mappingName !== '<no mapping>' ? this.configDownload.download : null} />)}
          />
          <Column field="mappingName" sortable={true} header="Mapping" />
          <Column field="lastConfigFetch" sortable={true} header="Last Connected" excludeGlobalFilter={true}
            body={(data) => (<TimeAgo date={data.lastConfigFetch} formatter={timeFormatter} />)}
          />
        </DataTable>
        <ConfigurationDownload onRef={ref => this.configDownload = ref} />
      </div>

    );
  }
}

function mapStateToProps(state) {
  const { agents } = state.agentStatus;
  return {
    agents
  }
}

export default connect(mapStateToProps, null)(StatusTable);