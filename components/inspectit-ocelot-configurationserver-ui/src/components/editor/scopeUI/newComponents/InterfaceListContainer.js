import UpperHeader from "./UpperHeader";
import Item from "./Item";
import LowerHeader from "./LowerHeader";

// parent attribute = 'interfaces'
function InterfaceListContainer( {items, parentAttribute, onUpdate,} ) {

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
      <div data-optiontype={parentAttribute} style={{  marginBottom: '',  position:'relative', height: '', padding: '25px', background: 'white', border: '1px solid lightgrey' , borderRadius: '10px'}}>
        <LowerHeader optionType={parentAttribute} />
        { items.map( (element, index) => 
          <Item onUpdate={(updateObj) => onUpdateListItem(updateObj, index)} index={index } item={element} parentAttribute={parentAttribute} />
        )}

      </div>
    </React.Fragment>
  )
}

export default InterfaceListContainer;