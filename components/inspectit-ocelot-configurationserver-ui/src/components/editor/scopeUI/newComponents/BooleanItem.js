import deepCopy from "json-deep-copy";
import {InputSwitch} from 'primereact/inputswitch';


// eigene information an mich, kann übersprungen werden beim review
// diese Komponente ist noch im Bau. Sie gehört zum Methoden Selector

// Either the container displays a single <Item> or an List of <Item>
function BooleanItem({item, attribute, onUpdate  }) {

  const onGenericUpdate = (e) => {
    let updatedItem = deepCopy(item);
    updatedItem[attribute] = e.value
    onUpdate(updatdItem);
  }

  console.log('BooleanItem', item, attribute)

  // distinction between array elements and non array elements
  return (
    item[attribute] && (
      <React.Fragment>
        <h4> the method is {attribute} </h4>
        <InputSwitch checked={item[attribute]} onChange={(e) => onGenericUpdate} />
      </React.Fragment>
    )
  )
}

 
export default BooleanItem;