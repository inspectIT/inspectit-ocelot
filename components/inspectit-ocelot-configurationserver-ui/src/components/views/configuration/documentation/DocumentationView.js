import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration';
import VersionItem from '../history/VersionItem';
import { VERSION_LIMIT } from '../../../../data/constants';
import { Dropdown } from 'primereact/dropdown';
import { Checkbox } from 'primereact/checkbox';
import axios from '../../../../lib/axios-api';
import ConfigDocumentation from './ConfigDocumentation';

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
      includeDefault: true,
    };
  }

  componentDidMount() {
    let tempAgentMappingNames = [];

    axios
      .get('mappings')
      .then((response) => {
        for (let mapping of response.data) {
          tempAgentMappingNames.push(mapping.name);
        }
        this.setState({ agentMappingNames: tempAgentMappingNames });
      })
      .catch((error) => {
        console.log('Error when retrieving mappings for Dropdown menu:');
        console.log(error);
      });
  }

  newDocumentationForMapping(mappingName, includeDefault) {
    if (mappingName != null) {
      axios
        .get('configuration/documentation/', { params: { 'agent-mapping': mappingName, 'include-default': includeDefault } })
        .then((response) => {
          this.setState({
            configDocumentation: response.data,
            includeDefault: includeDefault,
            selectedAgentMapping: mappingName,
          });
        })
        .catch((error) => {
          console.log(`Error when retrieving configuration documentation for Agent Mapping ${mappingName}:`);
          console.log(error);
          this.setState({
            includeDefault: includeDefault,
            selectedAgentMapping: mappingName,
          });
        });
    } else {
      this.setState({
        includeDefault: includeDefault,
      });
    }
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
            height: 75px
        `}
        </style>
        <div className={'mappingDropdown'}>
          <div>
            Config Docs for{' '}
            <Dropdown
              placeholder="Select an Agent Mapping"
              options={this.state.agentMappingNames}
              value={this.state.selectedAgentMapping}
              onChange={(e) => this.newDocumentationForMapping(e.value, this.state.includeDefault)}
            />
          </div>
          <div>
            <Checkbox
              checked={this.state.includeDefault}
              onChange={(e) => this.newDocumentationForMapping(this.state.selectedAgentMapping, e.checked)}
            />{' '}
            Include Default Configuration
          </div>
        </div>
        <ConfigDocumentation configDocumentation={this.state.configDocumentation} />
      </>
    );
  }
}

export default DocumentationView;
