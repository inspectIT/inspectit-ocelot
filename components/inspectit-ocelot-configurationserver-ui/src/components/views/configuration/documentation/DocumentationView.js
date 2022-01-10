import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration';
import VersionItem from '../history/VersionItem';
import { VERSION_LIMIT } from '../../../../data/constants';
import {Dropdown} from "primereact/dropdown";
import axios from '../../../../lib/axios-api';
import ConfigDocumentation from "./ConfigDocumentation";

/**
 * The sidebar panel for showing existing versions of the configuration files.
 */
class DocumentationView extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      agentMappingNames: [],
      configDocumentation: null,
      selectedAgentMapping: null,
    };
  }

  componentDidMount() {

    let tempAgentMappingNames = []

    axios
      .get('mappings')
      .then((response) => {
        for (let mapping of response.data) {
          tempAgentMappingNames.push(mapping.name);
        }
        this.setState({ agentMappingNames: tempAgentMappingNames });
      })
      .catch( (error) => {
          console.log('Error when retrieving mappings for Dropdown menu:');
          console.log(error);
      });

  }

  getNewDocumentationForMapping(mappingName) {
    axios
      .get('configuration/documentation/', { params: { 'agent-mapping': mappingName}})
      .then((response) => {
        this.setState({
          configDocumentation: response.data,
          selectedAgentMapping: mappingName,
        });
      })
      .catch( (error) => {
        console.log(`Error when retrieving configuration documentation for Agent Mapping ${mappingName}:`);
        console.log(error);
      });
  }

  render() {
    return (
      <>
      <style>
        {`
          .mappingDropdown {
            background-color: #e0e0e0;
            color: #111;
            padding: 0.5rem 0.5rem 0.5rem;
            height: 50px
        `}
      </style>
        <div className={'mappingDropdown'}>Config Docs for <Dropdown
          placeholder="Select an Agent Mapping" options={this.state.agentMappingNames}
          value={this.state.selectedAgentMapping}
          onChange={(e) => {
            this.getNewDocumentationForMapping(e.value)
          }}/>
        </div>
        <ConfigDocumentation configDocumentation={this.state.configDocumentation}/>
      </>
    );
  };
}

export default DocumentationView;
