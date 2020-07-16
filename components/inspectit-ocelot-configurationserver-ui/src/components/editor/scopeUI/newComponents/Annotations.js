import Name from "./Name";
import deepCopy from "json-deep-copy";

class Annotations extends React.Component {

  onUpdateListItem = ( updatedValue , index ) => {
    let { onUpdate, items } = this.props;
    const updatedItems = items;
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
            <Name text={'... has an anotation' } onUpdate={(updateObj) => this.onUpdateListItem(updateObj, index) } item={annotationItem}  optionType={optionType} />
            )}
      </div>  
    )
  }

}

export default Annotations;