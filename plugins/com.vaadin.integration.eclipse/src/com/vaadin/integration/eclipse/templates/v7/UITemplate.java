package com.vaadin.integration.eclipse.templates.v7;

import com.vaadin.integration.eclipse.templates.*;

/*
 * JET GENERATED do not edit!
 * The source templates are in the templates folder (note: not package).
 *
 * The JET source templates can be edited. They are then transformed into java
 * template classes by the JET plugin. To use the generated java templates, no 
 * dependencies are required.
 */
public class UITemplate {   
  protected static String nl;
  public static synchronized UITemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    UITemplate result = new UITemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL;
  protected final String TEXT_3 = NL + "import com.vaadin.annotations.Theme;";
  protected final String TEXT_4 = NL + "import com.vaadin.server.VaadinRequest;" + NL + "import com.vaadin.ui.Button;" + NL + "import com.vaadin.ui.Button.ClickEvent;" + NL + "import com.vaadin.ui.Label;" + NL + "import com.vaadin.ui.UI;" + NL + "import com.vaadin.ui.VerticalLayout;" + NL + "" + NL + "/**" + NL + " * Main UI class" + NL + " */" + NL + "@SuppressWarnings(\"serial\")";
  protected final String TEXT_5 = NL + "@Theme(\"";
  protected final String TEXT_6 = "\")";
  protected final String TEXT_7 = NL + "public class ";
  protected final String TEXT_8 = " extends UI {" + NL + "" + NL + "    @Override" + NL + "    protected void init(VaadinRequest request) {" + NL + "        final VerticalLayout layout = new VerticalLayout();" + NL + "        layout.setMargin(true);" + NL + "        setContent(layout);" + NL + "" + NL + "        Button button = new Button(\"Click Me\");" + NL + "        button.addClickListener(new Button.ClickListener() {" + NL + "            public void buttonClick(ClickEvent event) {" + NL + "                layout.addComponent(new Label(\"Thank you for clicking\"));" + NL + "            }" + NL + "        });" + NL + "        layout.addComponent(button);" + NL + "    }" + NL + "" + NL + "}";

    public String generate(String applicationPackage, String applicationName,
        String uiOrApplicationClass, String uiTheme)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
     if(uiTheme != null) {
    stringBuffer.append(TEXT_3);
     } 
    stringBuffer.append(TEXT_4);
     if(uiTheme != null) {
    stringBuffer.append(TEXT_5);
    stringBuffer.append( uiTheme );
    stringBuffer.append(TEXT_6);
     } 
    stringBuffer.append(TEXT_7);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_8);
    return stringBuffer.toString();
  }
}