package org.hl7.fhir.definitions.parsers;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.DefinedCode;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.EventDefn;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionDesignationComponent;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Factory;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetUtilities;
import org.hl7.fhir.dstu3.utils.ToolingExtensions;
import org.hl7.fhir.igtools.spreadsheets.CodeSystemConvertor;
import org.hl7.fhir.igtools.spreadsheets.TypeRef;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ValueSetGenerator {

  private Definitions definitions;
  private String version;
  private Calendar genDate; 

  public ValueSetGenerator(Definitions definitions, String version, Calendar genDate) {
    super();
    this.definitions = definitions;
    this.version = version;
    this.genDate = genDate;
  }

  public void check(ValueSet vs) throws Exception {
    if (!vs.hasUrl())
      throw new Exception("Value set with no URL!");

    if (vs.getId().equals("data-types"))
      genDataTypes(vs);
    else if (vs.getId().equals("defined-types"))
      genDefinedTypes(vs, false);
    else if (vs.getId().equals("all-types"))
      genDefinedTypes(vs, true);
    else if (vs.getId().equals("message-events"))
      genMessageEvents(vs);
    else if (vs.getId().equals("resource-types"))
      genResourceTypes(vs);
    else if (vs.getId().equals("abstract-types"))
      genAbstractTypes(vs);
  }

  private void genDataTypes(ValueSet vs) throws Exception {
    if (!vs.hasCompose())
      vs.setCompose(new ValueSetComposeComponent());
    vs.getCompose().addInclude().setSystem("http://hl7.org/fhir/data-types");
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    
    CodeSystem cs = new CodeSystem();
    CodeSystemConvertor.populate(cs, vs);
    cs.setUrl("http://hl7.org/fhir/data-types");
    cs.setVersion(version);
    cs.setCaseSensitive(true);
    cs.setContent(CodeSystemContentMode.COMPLETE);
    definitions.getCodeSystems().put(cs.getUrl(), cs);

    List<String> codes = new ArrayList<String>();
    for (TypeRef t : definitions.getKnownTypes())
      codes.add(t.getName());
    Collections.sort(codes);
    for (String s : codes) {
      if (!definitions.dataTypeIsSharedInfo(s)) {
        ConceptDefinitionComponent c = cs.addConcept();
        c.setCode(s);
        c.setDisplay(s);
        if (definitions.getPrimitives().containsKey(s))
          c.setDefinition(definitions.getPrimitives().get(s).getDefinition());
        else if (definitions.getConstraints().containsKey(s))
          ; // don't add these: c.setDefinition(definitions.getConstraints().get(s).getDefinition());
        else if (definitions.hasElementDefn(s))
          c.setDefinition(definitions.getElementDefn(s).getDefinition());
        else 
          c.setDefinition("...to do...");
      }
    }
    ToolingExtensions.addCSComment(cs.addConcept().setCode("xhtml").setDisplay("XHTML").setDefinition("XHTML format, as defined by W3C, but restricted usage (mainly, no active content)"), "Special case: xhtml can only be used in the narrative Data Type");
  }

  private void genResourceTypes(ValueSet vs) {
    if (!vs.hasCompose())
      vs.setCompose(new ValueSetComposeComponent());
    vs.getCompose().addInclude().setSystem("http://hl7.org/fhir/resource-types");
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    
    CodeSystem cs = new CodeSystem();
    CodeSystemConvertor.populate(cs, vs);
    cs.setUrl("http://hl7.org/fhir/resource-types");
    cs.setVersion(version);
    cs.setCaseSensitive(true);    
    cs.setContent(CodeSystemContentMode.COMPLETE);
    definitions.getCodeSystems().put(cs.getUrl(), cs);
        
    List<String> codes = new ArrayList<String>();
    codes.addAll(definitions.getKnownResources().keySet());
    //codes.addAll(definitions.getBaseResources().keySet());
    Collections.sort(codes);
    for (String s : codes) {
      DefinedCode rd = definitions.getKnownResources().get(s);
      ConceptDefinitionComponent c = cs.addConcept();
      if (rd == null) {
        c.setCode(s);
        c.setDisplay(definitions.getBaseResources().get(s).getName());
        c.setDefinition((definitions.getBaseResources().get(s).isAbstract() ? "--- Abstract Type! ---" : "")+ definitions.getBaseResources().get(s).getDefinition());
      }  else {
        c.setCode(rd.getCode());
        c.setDisplay(rd.getCode());
        c.setDefinition(rd.getDefinition());
      }
    }

  }

  private void genAbstractTypes(ValueSet vs) {
    if (!vs.hasCompose())
      vs.setCompose(new ValueSetComposeComponent());
    vs.getCompose().addInclude().setSystem("http://hl7.org/fhir/abstract-types");
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    
    CodeSystem cs = new CodeSystem();
    cs.setUserData("filename", vs.getUserString("filename").replace("valueset-", "codesystem-"));
    cs.setUserData("path", vs.getUserString("path").replace("valueset-", "codesystem-"));
    CodeSystemConvertor.populate(cs, vs);
    cs.setUrl("http://hl7.org/fhir/abstract-types");
    cs.setVersion(version);
    cs.setCaseSensitive(true);    
    cs.setContent(CodeSystemContentMode.COMPLETE);
    definitions.getCodeSystems().put(cs.getUrl(), cs);

    cs.addConcept().setCode("Type").setDisplay("Type").setDefinition("A place holder that means any kind of data type");
    cs.addConcept().setCode("Any").setDisplay("Any").setDefinition("A place holder that means any kind of resource");
  }

  private void genDefinedTypes(ValueSet vs, boolean doAbstract) throws Exception {
    ValueSetComposeComponent compose = new ValueSetComposeComponent(); 
    vs.setCompose(compose);
    compose.addInclude().setSystem("http://hl7.org/fhir/data-types");
    compose.addInclude().setSystem("http://hl7.org/fhir/resource-types");
    if (doAbstract)
      compose.addInclude().setSystem("http://hl7.org/fhir/abstract-types");
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
  }

  private void genMessageEvents(ValueSet vs) {
    if (!vs.hasCompose())
      vs.setCompose(new ValueSetComposeComponent());
    vs.getCompose().addInclude().setSystem("http://hl7.org/fhir/message-events");
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    
    CodeSystem cs = new CodeSystem();
    CodeSystemConvertor.populate(cs, vs);
    cs.setUserData("filename", vs.getUserString("filename").replace("valueset-", "codesystem-"));
    cs.setUserData("path", vs.getUserString("path").replace("valueset-", "codesystem-"));
    cs.setUrl("http://hl7.org/fhir/message-events");
    cs.setVersion(version);
    cs.setCaseSensitive(true);
    cs.setContent(CodeSystemContentMode.COMPLETE);
    definitions.getCodeSystems().put(cs.getUrl(), cs);

    List<String> codes = new ArrayList<String>();
    codes.addAll(definitions.getEvents().keySet());
    Collections.sort(codes);
    for (String s : codes) {
      ConceptDefinitionComponent c = cs.addConcept();
      EventDefn e = definitions.getEvents().get(s);
      c.setCode(s);
      c.setDisplay(e.getCode());
      c.setDefinition(e.getDefinition());
    }
  }

  public void updateHeader(BindingSpecification bs, ValueSet vs) throws Exception {
    ValueSetUtilities.checkShareable(vs);
    if (!vs.hasId())
      throw new Exception("no id");
    if (!vs.hasUrl())
      throw new Exception("no url");
    if (!vs.hasVersion())
      vs.setVersion(version);
    if (!vs.hasExperimental())
      vs.setExperimental(false);
    if (!vs.hasName())
      vs.setName(bs.getName());
    if (!vs.hasPublisher())
      vs.setPublisher("HL7 (FHIR Project)");
    if (!vs.hasContact()) {
      vs.addContact().getTelecom().add(Factory.newContactPoint(ContactPointSystem.URL, Utilities.noString(bs.getWebSite()) ? "http://hl7.org/fhir" : bs.getWebSite()));
      vs.getContact().get(0).getTelecom().add(Factory.newContactPoint(ContactPointSystem.EMAIL, Utilities.noString(bs.getEmail()) ? "fhir@lists.hl7.org" : bs.getEmail()));
    }
    if (!vs.hasDescription())
      vs.setDescription(Utilities.noString(bs.getDescription()) ? bs.getDefinition() : bs.getDefinition() + "\r\n\r\n" + bs.getDescription());
    if (!vs.hasCopyright())
      vs.setCopyright(bs.getCopyright());

    if (!vs.hasStatus())
      vs.setStatus(bs.getStatus() != null ? bs.getStatus() : PublicationStatus.DRAFT); // until we publish DSTU, then .review
    if (!vs.hasDate())
      vs.setDate(genDate.getTime());
    if (!Utilities.noString(bs.getV2Map()))
      vs.setUserData("v2map", bs.getV2Map());
    if (!Utilities.noString(bs.getV3Map()))
      vs.setUserData("v3map", checkV3Mapping(bs.getV3Map()));
  }
  
  private String checkV3Mapping(String value) {
    if (value.startsWith("http://hl7.org/fhir/ValueSet/v3-"))
      return value.substring("http://hl7.org/fhir/ValueSet/v3-".length());
    else
      return value;
  }


  public void loadOperationOutcomeValueSet(BindingSpecification cd, String folder) throws Exception {
    ValueSet vs = new ValueSet();
    cd.setValueSet(vs);
    cd.setBindingMethod(BindingMethod.ValueSet);
    vs.setId("operation-outcome");
    vs.setUrl("http://hl7.org/fhir/ValueSet/"+vs.getId());
    vs.setName("Operation Outcome Codes");
    vs.setPublisher("HL7 (FHIR Project)");
    
    vs.setUserData("filename", "valueset-"+vs.getId());
    vs.setUserData("committee", "fhir");
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    
    ContactDetail c = vs.addContact();
    c.addTelecom().setSystem(ContactPointSystem.URL).setValue("http://hl7.org/fhir");
    c.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue("fhir@lists.hl7.org");
    vs.setDescription("Operation Outcome codes used by FHIR test servers (see Implementation file translations.xml)");
    vs.setStatus(PublicationStatus.DRAFT);
    if (!vs.hasCompose())
      vs.setCompose(new ValueSetComposeComponent());
    vs.getCompose().addInclude().setSystem("http://hl7.org/fhir/operation-outcome");

    CodeSystem cs = new CodeSystem();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new File(Utilities.path(folder, "..", "..", "implementations", "translations.xml")));
    Element n = XMLUtil.getFirstChild(doc.getDocumentElement());
    cs.setHierarchyMeaning(CodeSystemHierarchyMeaning.ISA);
    while (n != null) {
      if ("true".equals(n.getAttribute("ecode"))) {
        String code = n.getAttribute("id");
        Map<String, String> langs = new HashMap<String, String>();
        Element l = XMLUtil.getFirstChild(n);
        while (l != null) {
          langs.put(l.getAttribute("lang"), l.getTextContent());
          l = XMLUtil.getNextSibling(l);
        }
        if (langs.containsKey("en")) {
          ConceptDefinitionComponent cv = cs.addConcept();
          cv.setCode(code);
          cv.setDisplay(langs.get("en"));
          for (String lang : langs.keySet()) {
            if (!lang.equals("en")) {
              String value = langs.get(lang);
              ConceptDefinitionDesignationComponent dc = cv.addDesignation();
              dc.setLanguage(lang);
              dc.setValue(value);
            }
          }
        }
      }
      n = XMLUtil.getNextSibling(n);
    }
    CodeSystemConvertor.populate(cs, vs);
    cs.setUrl("http://hl7.org/fhir/operation-outcome");
    cs.setVersion(version);
    cs.setCaseSensitive(true);
    cs.setContent(CodeSystemContentMode.COMPLETE);
    definitions.getCodeSystems().put(cs.getUrl(), cs);
  }


}
