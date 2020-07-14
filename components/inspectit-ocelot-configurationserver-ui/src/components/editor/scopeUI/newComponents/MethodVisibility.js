import { cloneDeep } from 'lodash';
import deepCopy from 'json-deep-copy';
import {Checkbox} from 'primereact/checkbox';

class MethodVisibility extends React.Component {
  componentBorderRef= React.createRef();

  onUpdate = (value,e) => {
    const { item, onUpdate } = this.props;
    const updateditem = deepCopy(item);

    if(e.checked)
      updateditem.push(value);
    else
      updateditem.splice(updateditem.indexOf(value), 1);
    onUpdate(updateditem);
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


  render() {
    const background_middleDiv = "white"; 
    const color_elementSchrift = "black";
    const { item, optionText,} = this.props;
    console.log('visiblity_', item)

    const visibilies = ['PUBLIC', 'PRIVATE', 'PROTECTED', 'PACKAGE'];

    return (
      <React.Fragment>
        {/* if item.matcher-mode is not defined the input still got displayed with the last known value, or with a value, thus we check wether a value exist  */}
        { item && ( 
          <div style={{display: 'flex'}}>
            <p style={{ color: color_elementSchrift, marginRight: '15px'}}> ... has any of the following visibilites</p>
            { visibilies.map( value => 
              <React.Fragment >
                <div style={{ marginRight:'10px' , position:'relative', bottom: '-12px'}}>
                  <Checkbox inputId="cb1" value={value} onChange={(e) => this.onUpdate(value, e)} checked={item.indexOf(value) !== -1}></Checkbox>
                  <label htmlFor="cb1" className="p-checkbox-label">{value} </label>
                </div>
              </React.Fragment>  
            )}
          </div>
        )}
      </React.Fragment>
    )
  }

}

export default MethodVisibility;