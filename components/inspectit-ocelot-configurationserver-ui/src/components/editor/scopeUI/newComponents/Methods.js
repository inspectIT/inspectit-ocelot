import Heading from "./Heading";
import Item from "./Item";
import Method from "./Method";
import { Button } from "primereact/button";

// parent attribute = 'interfaces'
function Methods( {items, parentAttribute, onUpdate,} ) {

  const onUpdateListItem = ( updatedValue , index ) => {
    let updatedItems = items;

    if ( Object.keys(updatedValue).length == 0) { 
      // the { } updatedValue is empty, thus it can be removed from the array 
      updatedItems = updatedItems.filter( (item, filterIndex ) => {
        if (filterIndex !== index ) return item;
      })
    } else { 
      // updatedValue is not empty, and must be modified within the index postion in the array
      updatedItems[index] = updatedValue;
    }
    onUpdate(updatedItems);
  }



  return (
    <React.Fragment>
      { items.map( (method, index) => 
        <React.Fragment>
          <Heading style={{ color: 'red' }} text={'Methods must fullfill all the the following options'} semantic={'or'} count={index} /> 
          <Method  item={method}  onUpdate={(updateObj) => onUpdateListItem(updateObj, index)}   optionText={'Apfel'} /> 
        </React.Fragment>
      )}
    </React.Fragment>
  )
}

export default Methods;

 