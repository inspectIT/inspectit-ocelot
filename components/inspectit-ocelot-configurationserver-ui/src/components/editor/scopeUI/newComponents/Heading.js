// andIndex display an AND-Connection between the Attributes
// orIndex to display an OR-Connection between the Attributes
// attribute
function Heading ({ attribute, text, count, semantic, style })  {

    const background_uberSchriftDiv = "white";

    const divStyle = { padding: '5px 10px 5px 10px' ,  ...style, background: background_uberSchriftDiv, width: 'fit-content', outline:'', marginBottom: '20px', borderRadius: '10px' };
    const pStyle = { fontWeight: 'bold', margin: 0 };

    let attributeText = '';
    attribute === 'superclass' && ( attributeText = 'inherits from a superclass, that  ')
    attribute === 'interfaces' && ( attributeText = 'Interface ')
    attribute === 'superclass' && ( attributeText = 'inherits from a superclass, that  ')
    attribute === 'interfacesHeader' && ( attributeText = 'implements all of the following interfaces. ')


    
    return (
      <div style={{...divStyle}}>
        { semantic && 
        <React.Fragment>
          {count === 0 && <h4 style={{ ...pStyle}}>The {text} {attributeText} ...</h4>}
          {count >0 && <h4 style={{ ...pStyle}}>{semantic} the {text} { attributeText} ...</h4> }          
        </React.Fragment>}

        {/* heading for a list item does not use semantic. example ( The interface... the interface... the interface.... is sufficient inside the context of a list ) */}
        { !semantic && 
        <React.Fragment>
          <h4 style={{ ...pStyle}}>The {text} {attributeText}...</h4>
        </React.Fragment>}
      </div>  
    )
}

export default Heading;