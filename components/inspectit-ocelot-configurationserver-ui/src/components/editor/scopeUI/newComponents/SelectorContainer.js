import ClassSelector from "./ClassSelector";
import MethodSelectorListContainer from "./MethodSelectorListContainer";

// interface, type, superclass, method ebene
function SelectorContainer({scopeObject, onUpdate, doNotFilter_maybeAsVariable, contextAlias_maybeAsVariable}) {
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

export default SelectorContainer;

// doNotFilter_maybeAsVariable, <SelectorContainer... /> gets the whole scopeObject, but we only want to display a set of the keys, in the context of class