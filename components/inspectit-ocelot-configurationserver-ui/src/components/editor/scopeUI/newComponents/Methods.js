import Heading from "./Heading";
import Item from "./Item";
import GenericJsonWrapper from "./Method";

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

  console.log('itemsK', items)

  return (
    <React.Fragment>
      <div data-optiontype={parentAttribute} style={{  marginBottom: '',  position:'relative', height: '', padding: '0 25px 0 25px' , background: 'white', border: '1px solid lightgrey' , borderRadius: '10px'}}>
        { items.map( (method, index) => 
          <React.Fragment>
            <Heading style={{ color: 'red' }} attributeText={'Methods must fullfill all the the following options'} connectionTypeAndOr={'or'} count={index} /> 
            <div style={{width:'', background:'white', minHeight: '200px',  padding:'35px', border: '1px solid lightgrey', borderRadius:'10px', marginBottom: '25px'}}>
              <h4 style={{marginBottom:'5px'}}> The Method...</h4>
              <GenericJsonWrapper  item={method}  onUpdate={(updateObj) => onUpdateListItem(updateObj, index)}   optionText={'Apfel'} /> 
              </div>
          </React.Fragment>
        )}

      </div>
    </React.Fragment>
  )
}

export default Methods;

 