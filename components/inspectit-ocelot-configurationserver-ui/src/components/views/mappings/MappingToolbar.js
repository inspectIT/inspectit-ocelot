import React from 'react';
import {Toolbar} from 'primereact/toolbar';
import {InputText} from 'primereact/inputtext';
import { Button } from 'primereact/button';

const MappingToolbar = (props) => {
  const searchFieldTooltipText = 'Enter a mapping name, a source or an attribute key/value pair to filter matching mappings. The filter is not case sensitive.'
  
  return (
    <Toolbar style={{'border': '0', 'background-color': '#eee'}} >
        <style jsx>{`
        .searchbox{
          display: flex;
          height: 2rem;
          align-items: center;
        }
        .searchbox :global(.pi) {
          font-size: 1.75rem;
          color: #aaa;
          margin-right: 1rem;
        }
        `}</style>
        <div className='p-toolbar-group-left'>
        <div className='searchbox'>
          <i className="pi pi-sitemap"></i>
          <h4 style={{'font-weight': 'normal', 'margin-right': '1rem'}}>Agent Mappings</h4>
          <InputText placeholder='Search' value={props.filterValue}  onChange={props.onChangeFilter} tooltip={searchFieldTooltipText}/>
        </div>
        </div>
        <div className='p-toolbar-group-right'>
          <Button icon='pi pi-refresh' onClick={props.onClickRefresh} style={{marginRight:'.25em'}}/>
          <Button icon='pi pi-plus' onClick={props.onAddNewMapping} style={{marginRight:'.25em'}} />
          <Button icon='pi pi-download' label='Config File' onClick={props.onDownload} />
        </div>
      </Toolbar>
  )
}

export default MappingToolbar