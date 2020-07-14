
import {InputText} from 'primereact/inputtext';
import {Dropdown} from 'primereact/dropdown';
import { cloneDeep } from 'lodash';
import deepCopy from 'json-deep-copy';

class MethodName extends React.Component {
  state = { optionTypeText: undefined, }

  onUpdate = (attribute, newValue) => {
    const { item, onUpdate } = this.props;
    const updated_item = deepCopy(item);
    updated_item[attribute] = newValue;
    onUpdate(updated_item);
  }

  deleteItem = (e) => {
    let { item, onUpdate, attributesToDelete } = this.props;
    // Notziz diese Komponente ändert nur 2 Attribute Vorteil 
    let copiedItem = cloneDeep(item);  // alle anderen Attribute noch drinnen haben, annotations aber unberührt 
    // smart deletion of specific keys leaving other in tact (generic)
    delete copiedItem['name'];
    delete copiedItem['matcher-mode'];
    onUpdate(copiedItem)
  }

  render() {
    const background_middleDiv = "white"; 
    const color_elementSchrift = "black";
    const { item, text,} = this.props;

    // dropdown data
    const dropdownOptions = [
      {label: 'EQUALS_FULLY', value: 'EQUALS_FULLY'},
      {label: 'STARTS_WITH', value: 'STARTS_WITH'},
      {label: 'STARTS_WITH_IGNORE_CASE', value: 'STARTS_WITH_IGNORE_CASE'},
      {label: 'CONTAINS', value: 'CONTAINS'},
      {label: 'CONTAINS_IGNORE_CASE', value: 'CONTAINS_IGNORE_CASE'},
      {label: 'ENDS_WITH', value: 'ENDS_WITH'},
      {label: 'ENDS_WITH_IGNORE_CASE', value: 'ENDS_WITH_IGNORE_CASE'},
    ];

    return (
      <React.Fragment>
        {/* if item.matcher-mode is not defined the input still got displayed with the last known value, or with a value, thus we check wether a value exist  */}
        { item['matcher-mode'] && ( 
          <div style={{display: 'inline-grid'}}>
            <div style={{display: 'inline-flex', }}>
              <p style={{ color: color_elementSchrift}}> {text} </p>
              <Dropdown style={{marginLeft:'10px', fontSize: '13px',  position: 'relative', height:'35px', bottom: '-5px'}} value={item['matcher-mode']} options={dropdownOptions} onChange={(e) => this.onUpdate('matcher-mode', e.value)} placeholder="EQUALS_FULLY"/>
              <p style={{  color: color_elementSchrift ,  marginLeft: '10px' }}>the term</p>
              <InputText style={{ textAlign: 'middle', width: '250px', marginLeft: '10px' , position: 'relative',  height:'35px', bottom: '-5px'}} value={item.name} onChange={(e) => this.onUpdate('name', e.target.value)} />
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

}

export default MethodName;