package org.hl7.fhir.definitions.generators.specification;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.ProfiledType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.igtools.spreadsheets.TypeRef;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator.Cell;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator.Row;

public class TableGenerator extends BaseGenerator {
  public enum RenderMode {
    DATATYPE,
    RESOURCE,
    LOGICAL
  }

  private final boolean ADD_REFERENCE_TO_TABLE = true;
  
  protected String dest; 
  protected String pageName;
  protected boolean inlineGraphics;
  
  public TableGenerator(String dest, PageProcessor page, String pageName, boolean inlineGraphics) throws Exception {
    super();
    this.dest = dest;
    this.definitions = page.getDefinitions();
    this.page = page;
    this.pageName = pageName;
    this.inlineGraphics = inlineGraphics;
  }

  protected boolean dictLinks() {
    return pageName != null;
  }
  protected Row genElement(ElementDefn e, HierarchicalTableGenerator gen, boolean resource, String path, boolean isProfile, String prefix, RenderMode mode) throws Exception {
    Row row = gen.new Row();

    row.setAnchor(path);
    boolean isProfiledExtension = isProfile && (e.getName().equals("extension") || e.getName().equals("modifierExtension"));
    row.getCells().add(gen.new Cell(null, dictLinks() ? pageName+"#"+path.replace("[", "_").replace("]", "_") : null, e.getName(), path+" : "+e.getDefinition(), null));
    Cell gc = gen.new Cell();
    row.getCells().add(gc);
    if (e.hasMustSupport() && e.isMustSupport()) 
      gc.addImage("mustsupport.png", "This element must be supported", "S", "white", "red");
    if (e.isModifier()) 
      gc.addImage("modifier.png", "This element is a modifier element", "?!", null, null);
    if (e.isSummary()) 
      gc.addImage("summary.png", "This element is included in summaries", "Σ", null, null);
    if (!e.getInvariants().isEmpty() || !e.getStatedInvariants().isEmpty()) 
      gc.addImage("lock.png", "This element has or is affected by some invariants", "I", null, null);
  
    if (resource) {
      row.getCells().add(gen.new Cell()); 
  
      row.setIcon("icon_resource.png", HierarchicalTableGenerator.TEXT_ICON_RESOURCE);
      if (Utilities.noString(e.typeCode()))
        row.getCells().add(gen.new Cell(null, null, "n/a", null, null)); 
      else if ("Logical".equals(e.typeCode()))
        row.getCells().add(gen.new Cell(null, prefix+"structuredefinition.html#logical", e.typeCode(), null, null)); 
      else
        row.getCells().add(gen.new Cell(null, prefix+e.typeCode().toLowerCase()+".html", e.typeCode(), null, null)); 
      // todo: base elements
    } else {
      if (!e.getElements().isEmpty()) {
        row.getCells().add(gen.new Cell(null, null, path.contains(".") ? e.describeCardinality() : "", null, null)); 
        row.setIcon("icon_element.gif", HierarchicalTableGenerator.TEXT_ICON_ELEMENT);
        if (mode == RenderMode.RESOURCE)
          row.getCells().add(gen.new Cell(null, prefix+"backboneelement.html", "BackboneElement", null, null));
        else if (e.getName().equals("Element"))
          row.getCells().add(gen.new Cell(null, null, "n/a", null, null)); 
        else
          row.getCells().add(gen.new Cell(null, prefix+"element.html", "Element", null, null));   
      } else if (e.getTypes().size() == 1) {
        row.getCells().add(gen.new Cell(null, null, path.contains(".") ? e.describeCardinality() : "", null, null)); 
        String t = e.getTypes().get(0).getName();
        Cell c;
        if (t.startsWith("@")) {
          row.setIcon("icon_reuse.png", HierarchicalTableGenerator.TEXT_ICON_REUSE);
          c = gen.new Cell("see ", "#"+t.substring(1), t.substring(t.lastIndexOf(".")+1), t.substring(1), null);
        } else if (t.equals("Reference")) {
          row.setIcon("icon_reference.png", HierarchicalTableGenerator.TEXT_ICON_REFERENCE);
          c = gen.new Cell();
          if (ADD_REFERENCE_TO_TABLE) {
          c.getPieces().add(gen.new Piece(prefix+"references.html", "Reference", null));
          c.getPieces().add(gen.new Piece(null, "(", null));
          }
          boolean first = true;
          for (String rt : e.getTypes().get(0).getParams()) {
            if (!first)
              c.getPieces().add(gen.new Piece(null, " | ", null));
            if (first && isProfile && e.getTypes().get(0).getProfile() != null)
              c.getPieces().add(gen.new Piece(null, e.getTypes().get(0).getProfile(), null));
            else
              c.getPieces().add(gen.new Piece(prefix+findPage(rt)+".html", rt, null));
            first = false;
          }
          if (ADD_REFERENCE_TO_TABLE) 
            c.getPieces().add(gen.new Piece(null, ")", null));
        } else if (definitions.getPrimitives().containsKey(t)) {
          row.setIcon("icon_primitive.png", HierarchicalTableGenerator.TEXT_ICON_PRIMITIVE);
          c = gen.new Cell(null, prefix+"datatypes.html#"+t, t, null, null);
        } else {
          if (t.equals("Extension"))
            row.setIcon("icon_extension_simple.png", HierarchicalTableGenerator.TEXT_ICON_EXTENSION);
          else
            row.setIcon("icon_datatype.gif", HierarchicalTableGenerator.TEXT_ICON_DATATYPE);
          c = gen.new Cell(null, prefix+definitions.getSrcFile(t)+".html#"+t.replace("*", "open"), t, null, null);
        }
        row.getCells().add(c);
      } else {
        row.getCells().add(gen.new Cell(null, null, e.describeCardinality(), null, null));   
        row.setIcon("icon_choice.gif", HierarchicalTableGenerator.TEXT_ICON_CHOICE);
        row.getCells().add(gen.new Cell(null, null, "", null, null));   
      }
    }
      
    Cell cc = gen.new Cell(null, e.getShortDefn() != null && Utilities.isURL(e.getShortDefn()) ? e.getShortDefn() : null, e.getShortDefn(), null, null);
    row.getCells().add(cc);
    
    // constraints
    if (isProfiledExtension) {
      cc.addPiece(gen.new Piece("br"));
      cc.getPieces().add(gen.new Piece(null, e.getTypes().get(0).getProfile(), null));
    }
    
    if (e.hasBinding() && e.getBinding() != null && e.getBinding().getBinding() != BindingMethod.Unbound) {
      if (cc.getPieces().size() == 1)
        cc.addPiece(gen.new Piece("br"));
      cc.getPieces().add(gen.new Piece(getBindingLink(prefix, e), e.getBinding().getValueSet() != null ? e.getBinding().getValueSet().getName() : e.getBinding().getName(), 
            e.getBinding().getDefinition()));
      cc.getPieces().add(gen.new Piece(null, " (", null));
      BindingSpecification b = e.getBinding();
      if (b.hasMax() ) {
        cc.getPieces().add(gen.new Piece(prefix+"terminologies.html#"+b.getStrength().toCode(), b.getStrength().getDisplay(),  b.getStrength().getDefinition()));
        cc.getPieces().add(gen.new Piece(null, " but limited to ", null));
        ValueSet vs = b.getMaxValueSet();
        cc.getPieces().add(gen.new Piece(vs.getUserString("path"), vs.getName(), null));
      }  else
        cc.getPieces().add(gen.new Piece(prefix+"terminologies.html#"+b.getStrength().toCode(), b.getStrength().getDisplay(),  b.getStrength().getDefinition()));
      cc.getPieces().add(gen.new Piece(null, ")", null));
    }
    for (String name : e.getInvariants().keySet()) {
      Invariant inv = e.getInvariants().get(name);
      cc.addPiece(gen.new Piece("br"));
      cc.getPieces().add(gen.new Piece(null, inv.getEnglish(), inv.getId()).setStyle("font-style: italic"));
    }
    if (e.unbounded()) {
      if (cc.getPieces().size() > 0)
        cc.addPiece(gen.new Piece("br"));
      if (Utilities.noString(e.getOrderMeaning())) {
        cc.getPieces().add(gen.new Piece(null, "This repeating element has no defined order", null));
      } else {
        cc.getPieces().add(gen.new Piece(null, "This repeating element order: "+e.getOrderMeaning(), null));
      }
      
    }

    if (mode == RenderMode.LOGICAL) {
      String logical = e.getMappings().get("http://hl7.org/fhir/logical");
      Cell c = gen.new Cell();
      row.getCells().add(c);
      if (logical != null)
        presentLogicalMapping(gen, c, logical, prefix);
    }
      
    if (e.getTypes().size() > 1) {
      // create a child for each choice
      for (TypeRef tr : e.getTypes()) {
        Row choicerow = gen.new Row();
        String t = tr.getName();
        if (t.equals("Reference")) {
          choicerow.getCells().add(gen.new Cell(null, null, e.getName().replace("[x]",  "Reference"), null, null));
          choicerow.getCells().add(gen.new Cell());
          choicerow.getCells().add(gen.new Cell(null, null, "", null, null));
          choicerow.setIcon("icon_reference.png", HierarchicalTableGenerator.TEXT_ICON_REFERENCE);
          Cell c = gen.new Cell();
          choicerow.getCells().add(c);
          if (ADD_REFERENCE_TO_TABLE) {
            c.getPieces().add(gen.new Piece(prefix+"references.html", "Reference", null));
            c.getPieces().add(gen.new Piece(null, "(", null));
          }
          boolean first = true;
          for (String rt : tr.getParams()) {
            if (!first)
              c.getPieces().add(gen.new Piece(null, " | ", null));
            c.getPieces().add(gen.new Piece(prefix+findPage(rt)+".html", rt, null));
            first = false;
          }
          if (ADD_REFERENCE_TO_TABLE) 
            c.getPieces().add(gen.new Piece(null, ")", null));
          
        } else if (definitions.getPrimitives().containsKey(t)) {
          choicerow.getCells().add(gen.new Cell(null, null, e.getName().replace("[x]",  Utilities.capitalize(t)), definitions.getPrimitives().get(t).getDefinition(), null));
          choicerow.getCells().add(gen.new Cell());
          choicerow.getCells().add(gen.new Cell(null, null, "", null, null));
          choicerow.setIcon("icon_primitive.png", HierarchicalTableGenerator.TEXT_ICON_PRIMITIVE);
          choicerow.getCells().add(gen.new Cell(null, prefix+"datatypes.html#"+t, t, null, null));
        } else if (definitions.getConstraints().containsKey(t)) {
          ProfiledType pt = definitions.getConstraints().get(t);
          choicerow.getCells().add(gen.new Cell(null, null, e.getName().replace("[x]", Utilities.capitalize(pt.getBaseType())), definitions.getTypes().containsKey(t) ? definitions.getTypes().get(t).getDefinition() : null, null));
          choicerow.getCells().add(gen.new Cell());
          choicerow.getCells().add(gen.new Cell(null, null, "", null, null));
          choicerow.setIcon("icon_datatype.gif", HierarchicalTableGenerator.TEXT_ICON_DATATYPE);
          choicerow.getCells().add(gen.new Cell(null, definitions.getSrcFile(t)+".html#"+t.replace("*", "open"), t, null, null));
        } else {
          choicerow.getCells().add(gen.new Cell(null, null, e.getName().replace("[x]",  Utilities.capitalize(t)), definitions.getTypes().containsKey(t) ? definitions.getTypes().get(t).getDefinition() : null, null));
          choicerow.getCells().add(gen.new Cell());
          choicerow.getCells().add(gen.new Cell(null, null, "", null, null));
          choicerow.setIcon("icon_datatype.gif", HierarchicalTableGenerator.TEXT_ICON_DATATYPE);
          choicerow.getCells().add(gen.new Cell(null, definitions.getSrcFile(t)+".html#"+t.replace("*", "open"), t, null, null));
        }
      
        choicerow.getCells().add(gen.new Cell());
//        choicerow.getCells().add(gen.new Cell());
        row.getSubRows().add(choicerow);
      }
    } else
      for (ElementDefn c : e.getElements())
        row.getSubRows().add(genElement(c, gen, false, path+'.'+c.getName(), isProfile, prefix, mode));
    return row;
  }

  private void presentLogicalMapping(HierarchicalTableGenerator gen, Cell c, String logical, String prefix) {
    c.addPiece(gen.new Piece(null, logical, null));
  }

//  private void presentLogicalMappingWord(HierarchicalTableGenerator gen, Cell c, String p, String prefix) {
//    if (p.contains(".") && page.getDefinitions().hasResource(p.substring(0, p.indexOf(".")))) {
//      String rn = p.substring(0, p.indexOf("."));
//      String rp = p.substring(p.indexOf(".")+1);
//      c.addPiece(gen.new Piece(prefix+rn.toLowerCase()+".html", rn, null));
//      c.addPiece(gen.new Piece(null, ".", null));
//      ResourceDefn r;
//      ElementDefn e; 
//      try {
//        r = page.getDefinitions().getResourceByName(rn);
//        e = r.getRoot().getElementForPath(p, page.getDefinitions(), "logical mapping", true);
//      } catch (Exception e1) {
//        r = null;
//        e = null;
//      }
//      if (e == null)
//        c.addPiece(gen.new Piece(null, rp, null));
//      else
//        c.addPiece(gen.new Piece(prefix+rn.toLowerCase()+"-definitions.html#"+p, rp, null));
//    } else if (page.getDefinitions().hasResource(p)) {
//      c.addPiece(gen.new Piece(prefix+p.toLowerCase()+".html", p, null));
//    } else {
//      c.addPiece(gen.new Piece(null, p, null));
//    }
//    
//  }

  private String findPage(String rt) {
    if (rt.equalsIgnoreCase("any"))
      return "resourcelist";
    if (rt.equalsIgnoreCase("binary"))
      return "http";
    return rt.toLowerCase();
  }
}
