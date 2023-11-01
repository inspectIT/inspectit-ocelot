import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { Toolbar } from 'primereact/toolbar';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Button } from 'primereact/button';
import PropTypes from 'prop-types';

const searchFieldTooltipText =
  'Enter a mapping name, a source or an attribute key/value pair to filter matching mappings. The filter is not case sensitive.';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
class MappingToolbar extends React.Component {
  componentDidMount = () => {
    this.props.fetchSourceBranch();
  };
  onChange = (event) => {
    const selectedBranch = event.target.value;
    if (selectedBranch) {
      this.props.putSourceBranch(selectedBranch);
    }
  };
  render() {
    const { filterValue, onChangeFilter, onAddNewMapping, onDownload, fetchMappings, readOnly, sourceBranch } = this.props;
    return (
      <div className="this">
        <style jsx>
          {`
            .this :global(.p-toolbar) {
              padding-top: 0.5rem;
              padding-bottom: 0.5rem;
            }
            .searchbox {
              display: flex;
              height: 2rem;
              align-items: center;
            }
            .searchbox :global(.pi) {
              font-size: 1.25rem;
              color: #aaa;
              margin-right: 1rem;
            }
            .dropdown {
              display: flex;
              align-items: center;
              font-size: 1.25rem;
              margin-right: 1rem;
            }
          `}
        </style>
        <Toolbar
          style={{ border: '0', backgroundColor: '#eee' }}
          left={
            <>
              <div className="p-toolbar-group-left">
                <div className="searchbox">
                  <i className="pi pi-sitemap" />
                  <h4 style={{ fontWeight: 'small', marginRight: '1rem' }}>Agent Mappings</h4>
                  <InputText
                    placeholder="Search"
                    value={filterValue}
                    onChange={(e) => onChangeFilter(e.target.value)}
                    tooltip={searchFieldTooltipText}
                  />
                </div>
              </div>
              <div className="p-toolbar-group-right">
                <div className="dropdown">
                  <Dropdown value={sourceBranch} onChange={(e) => this.onChange(e)} options={['WORKSPACE', 'LIVE']} />
                </div>
              </div>
            </>
          }
          right={
            <div className="p-toolbar-group-right">
              <Button icon="pi pi-refresh" onClick={fetchMappings} style={{ marginRight: '.25em' }} />
              <Button icon="pi pi-plus" onClick={() => onAddNewMapping()} style={{ marginRight: '.25em' }} disabled={readOnly} />
              {/** if primereact/dialog is in here, it would be hidden by default -> so it would need to be appended to body/mappingsview or the like */}
              <Button icon="pi pi-download" label="Config File" onClick={onDownload} />
            </div>
          }
        />
      </div>
    );
  }
}

MappingToolbar.propTypes = {
  /** The source branch for the agent mappings file itself */
  sourceBranch: PropTypes.string,
  /** Fetch current source branch */
  fetchSourceBranch: PropTypes.func,
  /** Update current source branch */
  putSourceBranch: PropTypes.func,
};

const mapStateToProps = (state) => {
  let sourceBranch = state.mappings.sourceBranch;
  return {
    sourceBranch: sourceBranch,
  };
};

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
  fetchSourceBranch: mappingsActions.fetchMappingsSourceBranch,
  putSourceBranch: mappingsActions.putMappingsSourceBranch,
};

export default connect(mapStateToProps, mapDispatchToProps)(MappingToolbar);
