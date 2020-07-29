
function MultipleItemBoxContainer ( props) {
  return (
    <div style={{display:'inline-grid', width:'100%'}}>
      {props.children}
    </div>
  )
}

export default MultipleItemBoxContainer;