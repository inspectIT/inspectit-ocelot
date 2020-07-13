import NameSelector from "./NameSelector";

class AnnotationContainer extends React.Component {
  state = { optionTypeText: undefined };

  componentWillMount() {

    const { optionType } = this.props;
    switch(optionType) {
      // class option types
        case 'type':
          this.setState({ optionTypeText: ''});
          break;
        case 'interfaces':
          this.setState({optionTypeText: 'The interface'}) 
          break;
        case 'superclass':
          this.setState({optionTypeText: 'superclass'}) 
          break;
  
      // method option types
        case 'name':
          this.setState({ optionTypeText: 'method name'})
          break;
        case 'visibility':
          this.setState({optionTypeText: 'visibility'}) 
          break;
        // TODO: case annotation ? oder annotations plural
        case 'annotation':
          this.setState({ optionTypeText: 'annotation'})
          break;
        case 'arguments':
          this.setState({ optionTypeText: 'arguments'})
          break;
      }
  }

  onUpdateListItem = ( updatedValue , index ) => {
    let { onUpdate, items } = this.props;
    let updatedItems = items;

    if ( Object.keys(updatedValue).length == 0) { // the { } updatedValue is empty, thus it can be removed from the array 
      updatedItems = updatedItems.filter( (item, filterIndex ) => {
        if (filterIndex !== index ) return item;
      })
    } else { // updatedValue is not empty, and must be modified within the index postion in the array
    }
    onUpdate(updatedItems);
  }

  render() {
    const { items, optionType, selectorType  } = this.props;
    const { optionTypeText } = this.state;

    console.log(this.props);
    
    return (
      <React.Fragment>
        {items.map( (annotationItem, index) => 
          <NameSelector onUpdate={(updateObj) => this.onUpdateListItem(updateObj, index) } item={annotationItem}  optionText={`${optionTypeText} has an annotation`}  optionType={optionType} />
        )}
      </React.Fragment>
    )
  }

}

export default AnnotationContainer;