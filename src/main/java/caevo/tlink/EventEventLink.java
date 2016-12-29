package caevo.tlink;

import org.jdom.Element;
import org.jdom.Namespace;

import caevo.TextEvent;
import caevo.Timex;

public class EventEventLink extends TLink {

  public EventEventLink(String eiid1, String eiid2, String rel) {
    this(eiid1,eiid2,rel,null);
  }
  public EventEventLink(String eiid1, String eiid2, String rel, String tidM) {
    super(eiid1,eiid2,rel,tidM);
  }
  
  public EventEventLink(String eiid1, String eiid2, TLink.Type rel) {
    this(eiid1,eiid2,rel,null);
  }
  public EventEventLink(String eiid1, String eiid2, TLink.Type rel, String tidM) {
    super(eiid1,eiid2,rel,tidM);
  }

  public EventEventLink(String eiid1, String eiid2, TLink.Type rel, boolean closed) {
    this(eiid1,eiid2,rel,closed,null);
  }
  public EventEventLink(String eiid1, String eiid2, TLink.Type rel, boolean closed, String tidM) {
    super(eiid1,eiid2,rel,closed,tidM);
  }

  public EventEventLink(Element el) {
    super(el);
  }

  public Element toElement(Namespace ns) {
    Element el = super.toElement(ns);
    el.setAttribute(TLink.TLINK_TYPE_ATT, TLink.EVENT_EVENT_TYPE_VALUE);
    return el;
  }
  
  public TextEvent getEvent1() {
  	return this.document.getEventByEiid(this.id1);
  }
  
  public TextEvent getEvent2() {
  	return this.document.getEventByEiid(this.id2);
  }
  
  public Timex getMagnitude() {
  	return this.idM!=null ? this.document.getTimexByTid(this.idM) : null;
  }

}
