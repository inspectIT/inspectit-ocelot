// andIndex display an AND-Connection between the Attributes
// orIndex to display an OR-Connection between the Attributes
// attribute
function UpperHeader ({ attributeText, count, connectionTypeAndOr,style })  {

    const background_uberSchriftDiv = "white";

    const divStyle = { padding: '5px 10px 0 10px' ,height:'30px',  ...style, background: background_uberSchriftDiv, width: 'fit-content', outline:'', marginBottom: '10px', marginTop: '30px', borderRadius: '10px' };
    const pStyle = { fontWeight: 'bold', marginTop: '0px' };
    
    return (
      <div style={{...divStyle}}>
        {count === 0 && <h4 style={{ ...pStyle}}>The {attributeText} ...</h4>}
        {count >0 && <h4 style={{ ...pStyle}}>{connectionTypeAndOr} the {attributeText} ...</h4> }
      </div>  
    )
}

export default UpperHeader;