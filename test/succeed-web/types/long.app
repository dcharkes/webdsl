application exampleapp

entity Ent {
  i :: Long
  validate(i > 10, "must be greater than 10")
}

define ignore-access-control outputLong1(i : Long){
  text(i.toString())
}

  define ignore-access-control inputLong1(i:Ref<Long>){
    var tname := getTemplate().getUniqueId()
    var req := getRequestParameter(tname)

    request var errors : List<String> := null
    
    if(errors != null){
      errorTemplateInput(errors){
        inputLongLongernal(i,tname)[all attributes]
      }
    }
    else{
      inputLongLongernal(i,tname)[all attributes]
    }
    validate{
      if(req != null){
        if(/-?\d+/.match(req)){
          if(req.parseLong() == null){
            errors := ["Outside of possible number range"];
          }
        }
        else{
          errors := ["Not a valid number"];
        }
      }
      if(errors == null){ // if no wellformedness errors, check datamodel validations
        errors := i.getValidationErrors();
      }
      errors := handleValidationErrors(errors);    
    }
  }

define ignore-access-control inputLongLongernal(i : Ref<Long>, tname : String){
  //var rname := getTemplate().getUniqueId()
  var req := getRequestParameter(tname)
  <input 
    if(getPage().inLabelContext()) { 
      id=getPage().getLabelString() 
    } 
    name=tname 
    if(req != null){ 
      value = req 
    }
    else{
      value = i 
    }
    class="inputLong "+attribute("class") 
    all attributes except "class"
  />

  databind{
    if(req != null){
      i := req.parseLong();
    }
  }
}

var e1 := Ent{ i := 5L }

define page root(){
    test(e1)
}

define test(e:Ent){ 
  " defined output"  
  outputLong1(e.i)
  form{
    "defined input"
    label(" CLICK ")[class = "label-elem"]{
      inputLong1(e.i)[class = "input-elem"]
    }
    submit action{}[class = "button-elem"]{"save"}
  }	
  <br />
  "built-in output"
  output(e.i)
  form{
  "built-in input"
    label(" CLICK ")[class = "built-in-label-elem"]{
      input(e.i)[class = "built-in-input-elem"]
    }
    submit action{}[class = "built-in-button-elem"]{"save"}
  }
}

var e2 := Ent{ i := 5L }

define page nolabel(){
  testnolabel(e2)
}

define testnolabel(e:Ent){ 
  " defined output"  
  outputLong1(e.i)
  form{
    "defined input"
    inputLong1(e.i)[class = "input-elem"]
    submit action{}[class = "button-elem"]{"save"}
  }	
  <br />
  "built-in output"
  output(e.i)
  form{
    "built-in input"
    input(e.i)[class = "built-in-input-elem"]
    submit action{}[class = "built-in-button-elem"]{"save"}
  }
}

test inttemplates {
  var d : WebDriver := FirefoxDriver();
  d.get(navigate(root()));
  
  var input     :WebElement   := d.findElements(SelectBy.className(         "input-elem"))[0];
  var builtininput := d.findElements(SelectBy.className("built-in-input-elem"))[0];
  var label        := d.findElements(SelectBy.className(         "label-elem"))[0];
  var builtinlabel := d.findElements(SelectBy.className("built-in-label-elem"))[0];
  assert(input.getAttribute("id")==label.getAttribute("for"));
  assert(builtininput.getAttribute("id")==builtinlabel.getAttribute("for"));
  
  commonTest(d);
  
  d.get(navigate(nolabel()));
  commonTest(d);
  
  d.close();
}
  
function commonTest(d:WebDriver){  
  var input     :WebElement   := d.findElements(SelectBy.className(         "input-elem"))[0];
  var builtininput := d.findElements(SelectBy.className("built-in-input-elem"))[0];
  assert(       input.getValue()=="5");
  assert(builtininput.getValue()=="5");
 
  //add an 8 in the defined input to make 58
  //defined input
  inputDefinedCheck(d,"58","defined output58");
  //built-in input
  inputBuiltinCheck(d,"546","built-in output546");
  
  //trigger validation error for invalid number
  //defined input
  inputDefinedCheck(d,"ffd","Not a valid number");
  //built-in input
  inputBuiltinCheck(d,"ffd","Not a valid number");
  
  //trigger validation error for too large number
  //defined input
  inputDefinedCheck(d,"999999999999999999999999999999999999999999999999999999999","Outside of possible number range");
  //built-in input
  inputBuiltinCheck(d,"999999999999999999999999999999999999999999999999999999999","Outside of possible number range");
  
  //trigger entity validation error 
  //defined input
  inputDefinedCheck(d,"5","must be greater than 10");
  //built-in input
  inputBuiltinCheck(d,"4","must be greater than 10");

}

function inputBuiltinCheck(d:WebDriver, input:String, error:String){
  inputCheck(d, input, error, "built-in-");
}
function inputDefinedCheck(d:WebDriver, input:String, error:String){
  inputCheck(d, input, error, "");
}
function inputCheck(d:WebDriver, input:String, error:String, builtin:String){
  var inputelem := d.findElements(SelectBy.className(builtin+"input-elem"))[0];
  inputelem.clear();
  inputelem.sendKeys(input);
  var button := d.findElements(SelectBy.className(builtin+"button-elem"))[0];
  button.click();
  assert(d.getPageSource().contains(error));
}