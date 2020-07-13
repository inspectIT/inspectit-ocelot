
import {InputText} from 'primereact/inputtext';
import {Dropdown} from 'primereact/dropdown';
import { cloneDeep } from 'lodash';
import deepCopy from 'json-deep-copy';

class NameSelector extends React.Component {
  state = { optionTypeText: undefined, }
  componentBorderRef= React.createRef();

  onUpdate = (attribute, newValue) => {
    const { item, onUpdate } = this.props;
    const updated_item = deepCopy(item);
    updated_item[attribute] = newValue;
    onUpdate(updated_item);
  }

  deleteItem = (e) => {
    let { item, onUpdate, optionType, attributesToDelete } = this.props;
    // Notziz diese Komponente ändert nur 2 Attribute Vorteil 
    let copiedItem = cloneDeep(item);  // alle anderen Attribute noch drinnen haben, annotations aber unberührt 
    // smart deletion of specific keys leaving other in tact (generic)
    delete copiedItem['name'];
    delete copiedItem['matcher-mode'];
    onUpdate(copiedItem)
  }

  handleMouseOver = (e) => {
    const element = this.componentBorderRef.current
    element.style.boxShadow = '0 0 0 3px red';
  } 

  handleMouseLeave = (e) => {
    const element = this.componentBorderRef.current
    element.style.boxShadow = '0 0 0 1px lightsteelblue';
  }

  render() {
    const background_middleDiv = "white"; 
    const color_elementSchrift = "black";
    const { item, optionType, } = this.props;

    let optionText;
    optionType === 'interfaces' ? optionText = 'The interface has a name' : optionText = 'has a name';
    
    
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
            <div  ref={this.componentBorderRef}  style={{display: 'inline-flex',  marginBottom: '5px', position:'relative', background: background_middleDiv, padding: '10px 30px 0px 10px', borderRadius:'10px'}}>
              <p style={{ color: color_elementSchrift}}> ... {optionText}, that </p>
              <Dropdown style={{marginLeft:'10px', fontSize: '13px',  position: 'relative', height:'35px', bottom: '-5px'}} value={item['matcher-mode']} options={dropdownOptions} onChange={(e) => this.onUpdate('matcher-mode', e.value)} placeholder="EQUALS_FULLY"/>
              <p style={{  color: color_elementSchrift ,  marginLeft: '10px' }}>the term</p>
              <InputText style={{ textAlign: 'middle', width: '250px', marginLeft: '10px' , position: 'relative',  height:'35px', bottom: '-5px'}} value={item.name} onChange={(e) => this.onUpdate('name', e.target.value)} />
              <i onMouseOver={this.handleMouseOver} onMouseLeave={this.handleMouseLeave} onClick={this.deleteItem} style={{ position: 'absolute', bottom:'0px', right: '0px', fontSize:'30px',  color: 'red', opacity:'0.8'}} className="pi pi-times-circle"/>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

}

export default NameSelector;