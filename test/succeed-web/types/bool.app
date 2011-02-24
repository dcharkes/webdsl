application exampleapp

entity Bla {
  name :: String
  bla :: Bool
  forceTrue :: Bool
  validate(!forceTrue || bla, "force true was enabled, but value was false")
}

define ignore-access-control outputBool1(b : Bool){
  <input 
    type="checkbox"
    if(b){
     checked="true"
    }
    disabled="true" 
    all attributes 
  />
}

  define inputBool1(b:Ref<Bool>){
    var tname := getTemplate().getUniqueId() // regular var is reset when validation fails
    request var errors : List<String> := null // need a var that keeps its value, even when validation fails
    
    if(errors != null && errors.length > 0){
      errorTemplateInput(errors){
        inputBoolInternal(b,tname)[all attributes]  // use same tname so the inputs are updated in both cases
      }
    }
    else{
      inputBoolInternal(b,tname)[all attributes]
    }
    validate{
      errors := b.getValidationErrors();
      errors := handleValidationErrors(errors);
    }
  }

    // if(e) e1 else e2    //java e?e1:e2   //python e1 if e else e2   
define ignore-access-control inputBoolInternal(b : Ref<Bool>,rname:String){
  //var rname := getTemplate().getUniqueId()
  var rnamehidden := rname + "_isinput"
     
  <input type="hidden" name=rname+"_isinput" />
    <input type="checkbox" 
    if(getPage().inLabelContext()) { 
      id=getPage().getLabelString() 
    } 
    name=rname 
    //true when it was submitted as true or it was not submitted but the value was already true
    if(getRequestParameter(rnamehidden)!=null && getRequestParameter(rname)!=null || getRequestParameter(rnamehidden)==null && b){ 
      checked="true"  
    }
    class="inputBool "+attribute("class") 
    all attributes except "class"
  />

  databind{
    //log("databind"+b.getEntity().id+" "+rname);
    var tmp : String := getRequestParameter(rname);
    var tmphidden := getRequestParameter(rnamehidden);
    if(tmphidden != null){
      if(getRequestParameter(rname) != null){
        b := true;     	
      }
      else{
        b := false;
      }
    }
  }
}

var b1 := Bla{ name := "b1" bla := false forceTrue:=false}
var b2 := Bla{ name := "b2" bla := true forceTrue:=true}

define page root(){
    "regular"
    <br />
    test(b1)
    <br />
    <br />
    <br />
    "validated, must be true"
    <br />
    test(b2)
    
    <br />
    navigate testnolabel(){ "no label test" }
}

define test(b:Bla){ 
  output(b.name)
  " defined output"  
  outputBool1(b.bla)[class = "outputelem"+b.name]
  form{
    "defined input"
    label(" CLICK ")[class = "labelelem"+b.name]{
      inputBool(b.bla)[class = "inputelem"+b.name] //@TODO change to inputBool1 when labels+validation is supported
    }
    submit action{} [class = "savebutton"+b.name] {"save"}
  }	
  <br />
  "built-in output (for reference) "
  output(b.bla)[class = "builtinoutputelem"+b.name]
  form{
  "built-in input"
    label(" CLICK ")[class = "builtinlabelelem"+b.name]{
      input(b.bla)[class = "builtininputelem"+b.name]
    }
    submit action{} [class = "builtinsavebutton"+b.name]  {"save"}
  }
}

test booltemplates {
  var d : WebDriver := FirefoxDriver();
  d.get(navigate(root()));
  var input        := d.findElements(SelectBy.className(       "inputelemb1"))[0];
  var builtininput := d.findElements(SelectBy.className("builtininputelemb1"))[0];
  assert(       !input.isSelected());
  assert(!builtininput.isSelected());
  var label        := d.findElements(SelectBy.className(       "labelelemb1"))[0];
  var builtinlabel := d.findElements(SelectBy.className("builtinlabelelemb1"))[0];
  label.click();
  builtinlabel.click();
  assert(input.isSelected());
  assert(builtininput.isSelected());
  var button := d.findElements(SelectBy.className("savebuttonb1"))[0];
  button.click();
  
  var input2        := d.findElements(SelectBy.className(       "inputelemb1"))[0];
  var builtininput2 := d.findElements(SelectBy.className("builtininputelemb1"))[0];
  assert(       input2.isSelected());
  assert(builtininput2.isSelected());
  
  
  //tests with validation
  
  var input3 := d.findElements(SelectBy.className("inputelemb2"))[0];
  input3.toggle();
  assert(!input3.isSelected());
  
  var button3 := d.findElements(SelectBy.className("savebuttonb2"))[0];
  button3.click();
  
  assert(d.getPageSource().split("force true was enabled, but value was false").length == 2, "didn't find a single validation error for defined bool input");
  assert(d.getPageSource().split("CLICK")[3].contains("force true was enabled, but value was false"), "didn't find a single validation error for defined bool input, behind the label");
  
  var input4 := d.findElements(SelectBy.className("builtininputelemb2"))[0];
  input4.toggle();
  assert(!input4.isSelected());
  
  var button4 := d.findElements(SelectBy.className("builtinsavebuttonb2"))[0];
  button4.click();
  
  assert(d.getPageSource().split("force true was enabled, but value was false").length == 2, "didn't find a single validation error for builtin bool input");
  
  testnolabel(d);
  
  d.close();
}


var b4 := Bla{ name := "b4" bla := true forceTrue:=true}

define page testnolabel(){ 
  outputBool1(b4.bla)[class = "outputelem"]
  form{
    inputBool1(b4.bla)[class = "inputelem"]
    submit action{} [class = "savebutton"] {"save"}
  }	
  outputBool(b4.bla)[class = "builtinoutput"]
  form{
    inputBool(b4.bla)[class = "builtininput"]
    submit action{} [class = "builtinsavebutton"] {"save"}
  }	
}

function testnolabel(d:WebDriver) {
  d.get(navigate(testnolabel()));

  var input := d.findElements(SelectBy.className("inputelem"))[0];
  input.toggle();
  assert(!input.isSelected());
  
  var button := d.findElements(SelectBy.className("savebutton"))[0];
  button.click();
  
  assert(d.getPageSource().split("force true was enabled, but value was false").length == 2, "didn't find a single validation error for defined bool input");
  
  var input4 := d.findElements(SelectBy.className("builtininput"))[0];
  input4.toggle();
  assert(!input4.isSelected());
  
  var button4 := d.findElements(SelectBy.className("builtinsavebutton"))[0];
  button4.click();
  
  assert(d.getPageSource().split("force true was enabled, but value was false").length == 2, "didn't find a single validation error for builtin bool input");
}