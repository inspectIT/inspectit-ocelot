import ClassSelector from "./ClassSelector";
import MethodSelectorListContainer from "./MethodContainerSelectorListContainer";
import deepCopy from "json-deep-copy";

// obsolete solution, delete component
class GroupAttributesWrapper extends React.Component {
  state={groupedItem = [], usedAttributes = []}

  componentWillMount(){
    this.createGroupedItems();
  }

  // Distribution of responsibility.
  // the following variables are required to be split from the item. This approach can be used within every other component. 
  // [json,json,array]
  // [type,superclass,interface]
  // splitting up the attributes and letting a component handle the onUpdate of the single items. Distribution of responsibility.
  createGroupedItems = (groupType) => {
    const { scopeObject } = this.props;
    const item = scopeObject;
    const copyItem = deepCopy(copyItem);
    
    let groupedClassItems = [];
    let usedClassAttributes = [];
    let classAttributes = [];                        scopeObject, onUpdate, groupType

    if( groupType === 'Class' )  classAttributes = ['type', 'interfaces', 'superclass'];

    Object.keys(copyItem).map( attribute => {
      if (classAttributes.includes(attribute)) { 
        groupedClassItems.push(copyItem[attribute])
        usedClassAttributes.push(attribute);
      }
    })
    this.setState({groupedItem, usedAttributes})
  }

  onGroupUpdate = (updatedValues, group) => {
    const { scopeObject, onUpdate } = this.props;
    const item = scopeObject;
    const updatedItem = deepCopy(item);
    group.map( attribute => {
      updatedItem[attribute] = updatedValues[attribute];
    })
    onUpdate(updatedItem);
  }

  return (
    <React.Fragment>
      { Object.keys(scopeObject).map( (optionType, selectorContainerIndex) => 
        <React.Fragment> 
        {/* HINT: selectorContainerIndex explanation. It is used within the upperheader to visualize the ... and releation between the optionTypes */}
        {contextAlias_maybeAsVariable=== 'classes' && doNotFilter_maybeAsVariable.includes(optionType) && <ClassSelector onUpdate={onUpdate} item={scopeObject} optionType={optionType} selectorType={'Class'} selectorContainerIndex={selectorContainerIndex}/>}
        {contextAlias_maybeAsVariable === 'methods' && doNotFilter_maybeAsVariable.includes(optionType) && <MethodSelectorListContainer  onUpdate={onUpdate} items={scopeObject['methods']} optionType={optionType} selectorType={'Method'} selectorContainerIndex={selectorContainerIndex}/>}
        </React.Fragment>
      )}
    </React.Fragment>
  )
}

export default GroupAttributesWrapper;

// doNotFilter_maybeAsVariable, <SelectorContainer... /> gets the whole scopeObject, but we only want to display a set of the keys, in the context of class