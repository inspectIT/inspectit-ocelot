import UpperHeader from "./UpperHeader";
import Item from "./Item";
import LowerHeader from "./LowerHeader";
import MethodSelector from "./MethodSelector";

// parent attribute = 'interfaces'
function InterfaceListContainer( {items, parentAttribute, onUpdate, selectorContainerIndex} ) {

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
      <div data-optiontype={parentAttribute} style={{  marginBottom: '',  position:'relative', height: '', padding: '25px', background: '#EEEEE', borderRadius: '10px' , border: '1px solid black'}}>
        
        { items.map( (item, index) => 
          <MethodSelector onUpdate={(updateObj) => onUpdateListItem(updateObj, index)}  onUpdate={onUpdate} item={item} parentAttribute={'method'} selectorType={'Class'} selectorContainerIndex={selectorContainerIndex}/>   
        )}

      </div>
    </React.Fragment>
  )
}

export default InterfaceListContainer;