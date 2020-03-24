import React from 'react';
import { connect } from 'react-redux';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import {OverlayPanel} from 'primereact/overlaypanel';

import TimeAgo from 'react-timeago';
import ConfigurationDownload from '../mappings/ConfigurationDownload';
import dateformat from 'dateformat';
import classnames from 'classnames';
import { linkPrefix } from '../../../lib/configuration';

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

  configDownload = React.createRef();

  downloadConfiguration = (attributes) => {
    this.configDownload.download(attributes);
  }

  nameTemplate = (rowData) => {
    const { metaInformation } = rowData;

    if (metaInformation) {
      return this.agentNameTemplate(rowData);
    } else {
      return <span>-</span>;
    }
  }

  agentNameTemplate = (rowData) => {
    const { metaInformation: { agentId }, attributes: { service }, attributes } = rowData;

    return (
      <div className="this">
        <style jsx>{`
        .agent-id {
          color: gray;
        }
        .this {
          position: relative;
        }
        .this :global(.download-button) {
          width: 1.2rem;
          height: 1.2rem;
          position: absolute;
          right: 0;
          top: 0;
          background: #ddd;
          border-color: #ddd;
        }
        `}</style>
        {service} <span className="agent-id">({agentId})</span>

        <Button
          className="download-button"
          icon='pi pi-download'
          onClick={() => this.configDownload.download(attributes)}
          tooltip='Click here to download the configuration file for this agent'
        />
      </div>
    );
  }

  iconTemplate = (rowData) => {
    const { metaInformation } = rowData;

    let imgColor;
    if (metaInformation) {
      imgColor = "orange";
    } else {
      imgColor = "gray";
    }

    const imgSrc = linkPrefix + "/static/images/inspectit-ocelot-head_" + imgColor + ".svg";
    return (
      <>
        <style jsx>{`
        .icon {
          width: 1.5rem;
          vertical-align: bottom;
          margin-right: 0.5rem;
        }
        `}</style>
        <img className="icon" src={imgSrc} />
      </>
    );
  }

  agentVersionTemplate = (rowData) => {
    const { metaInformation } = rowData;

    return (
      <span>{metaInformation ? metaInformation.agentVersion : "-"}</span>
    );
  }

  javaVersionTemplate = (rowData) => {
    const { metaInformation } = rowData;

    return (
      <span>{metaInformation ? metaInformation.javaVersion : "-"}</span>
    );
  }

  jvmRestartTemplate = (rowData) => {
    const { metaInformation } = rowData;

    let date = "-";
    if (metaInformation) {
      const startTime = new Date(Number(metaInformation.startTime));
      date = dateformat(startTime, "dd/mm/yy HH:MM:ss");
    }
    return (
      <span>{date}</span>
    );
  }

  agentMappingTemplate = ({ mappingName }) => {
    let name;
    let classname;
    if (mappingName) {
      name = mappingName;
    } else {
      name = "no mapping";
      classname = "no-mapping";
    }

    return (
      <div className="this">
        <style jsx>{`
      .no-mapping {
        color: gray;
        font-style: italic;
      }
      .this :global(.attributes-button) {
        width: 1.5rem;
        height: 1.5rem;
        vertical-align: sub;
        margin-right: 0.5rem;
      }
      `}</style>
        <Button className="attributes-button" icon='pi pi-tag' onClick={(e) => this.op.toggle(e)} />
        <span className={classname}>{name}</span>

        <OverlayPanel ref={(el) => this.op = el} showCloseIcon={true} dismissable={true}>
          asdads
        </OverlayPanel>
      </div>
    );
  }

  getAgentName = ({ metaInformation, attributes }) => {
    if (metaInformation) {
      return attributes.service + " (" + metaInformation.agentId + ")";
    } else {
      return null;
    }
  }

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
        name: this.getAgentName(agent),
        attributesSearchString
      }
    })

    return (
      <div className="this">
        <style jsx>{`
        .this :global(.p-datatable) :global(th) {
          border: 0 none;
          text-align: left;
        }

        .this :global(.p-datatable) :global(.p-filter-column) {
          border-top: 1px solid #c8c8c8;
        }

        .this :global(.p-datatable-tbody) :global(td) {
          border: 0 none;
          cursor: auto;
        }  
        `}</style>
        <DataTable value={agentValues} globalFilter={this.props.filter}>
          <Column body={this.iconTemplate} style={{ width: '34px' }} />
          <Column field="name" header="Name" body={this.nameTemplate} sortable style={{ width: '350px' }} />
          <Column field="metaInformation.agentVersion" header="Agent Version" body={this.agentVersionTemplate} sortable style={{ width: '150px' }} />
          <Column field="metaInformation.javaVersion" header="Java Version" body={this.javaVersionTemplate} sortable style={{ width: '150px' }} />
          <Column field="metaInformation.startTime" header="Last JVM Restart" body={this.jvmRestartTemplate} sortable style={{ width: '200px' }} />

          {/*<Column header="Attributes" field="attributesSearchString"
            body={(data) => (<AttributesCell attributes={data.attributes} onDownload={data.mappingName !== '<no mapping>' ? this.configDownload.download : null} />)}
      />*/}
          <Column field="mappingName" body={this.agentMappingTemplate} sortable={true} header="Agent Mapping" />
          <Column field="lastConfigFetch" sortable={true} header="Last Fetch" excludeGlobalFilter={true}
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