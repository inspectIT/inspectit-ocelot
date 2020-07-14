import MethodName from "./MethodName";
import deepCopy from "json-deep-copy";

class MethodAnnotations extends React.Component {

  onUpdateListItem = ( updatedValue , index ) => {
    let { onUpdate, items } = this.props;
    const updatedItems = deepCopy(items);
    // let updatedItems = items;

    if ( Object.keys(updatedValue).length == 0) {  
      updatedItems = updatedItems.filter( (item, filterIndex ) => {
        if (filterIndex !== index ) return item;
      })
    } else {  
      updatedItems[index] = updatedValue;
    }
    onUpdate(updatedItems);
  }

  render() {
    const { items, optionType,   } = this.props;
    
    return (
      <div style={{display: 'inline-block'}}> 
        {items.map( (annotationItem, index) => 
            <MethodName text={'... has an anotation' } onUpdate={(updateObj) => this.onUpdateListItem(updateObj, index) } item={annotationItem}  optionType={optionType} />
            )}
      </div>  
    )
  }

}

export default MethodAnnotations;