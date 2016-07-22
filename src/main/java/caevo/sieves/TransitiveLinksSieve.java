package caevo.sieves;

import caevo.SieveDocument;
import caevo.SieveDocuments;
import caevo.tlink.EventEventLink;
import caevo.tlink.EventTimeLink;
import caevo.tlink.TLink;
import caevo.tlink.TimeTimeLink;

import java.util.LinkedList;
import java.util.List;

/**
 * This sieve fill in the gaps by looking for transitive relations between events and timexes by examining the tlinks
 * already found by other sieves. For instance, if event e1 has been determined to be 'before' timex t1, and t1 is 'before'
 * timex t2, it follows that e1 is also 'before' t2.
 *
 * @author ymishory
 */
public class TransitiveLinksSieve implements Sieve {

    @Override
    public List<TLink> annotate(SieveDocument doc, List<TLink> currentTLinks) {
        List<TLink> proposed = new LinkedList<TLink>(), combined = new LinkedList<TLink>(currentTLinks);

        // deduce Event-Timex links from Timex-Timex links
        for (TLink tt : currentTLinks) {
            if (tt instanceof TimeTimeLink) {
                for (int i = 0; i < combined.size(); i++) {
                    TLink et = combined.get(i);
                    if (et instanceof EventTimeLink) {
                        if (et.getId2().equals(tt.getId1())) {
                            if (containsETLink(combined, et.getId1(), tt.getId2())) continue;
                            switch (et.getRelation()) {
                                case BEFORE:
                                case BEFORE_OR_OVERLAP:
                                case ENDED_BY:
                                    switch (tt.getRelation()) {
                                        case IS_INCLUDED:
                                        case OVERLAP:
                                        case SIMULTANEOUS:
                                        case DURING:
                                        case BEFORE:
                                        case BEFORE_OR_OVERLAP:
                                        case ENDED_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId2(), TLink.Type.BEFORE);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                    }
                                    break;
                                case IS_INCLUDED:
                                case OVERLAP:
                                case SIMULTANEOUS:
                                case DURING:
                                    switch (tt.getRelation()) {
                                        case AFTER:
                                        case OVERLAP_OR_AFTER:
                                        case BEGUN_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId2(), TLink.Type.AFTER);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                        case OVERLAP:
                                        case DURING:
                                        case SIMULTANEOUS:
                                        case IS_INCLUDED: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId2(), TLink.Type.IS_INCLUDED);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                        case BEFORE:
                                        case BEFORE_OR_OVERLAP:
                                        case ENDED_BY:
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId2(), TLink.Type.BEFORE);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                    }
                                    break;
                                case AFTER:
                                case OVERLAP_OR_AFTER:
                                case BEGUN_BY:
                                    switch (tt.getRelation()) {
                                        case IS_INCLUDED:
                                        case OVERLAP:
                                        case DURING:
                                        case SIMULTANEOUS:
                                        case AFTER:
                                        case OVERLAP_OR_AFTER:
                                        case BEGUN_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId2(), TLink.Type.AFTER);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                    }
                                    break;
                            }
                        } else if (et.getId2().equals(tt.getId2())) {
                            if (containsETLink(combined, et.getId1(), tt.getId1())) continue;
                            switch (et.getRelation()) {
                                case BEFORE:
                                case BEFORE_OR_OVERLAP:
                                case ENDED_BY:
                                    switch (tt.getRelation()) {
                                        case IS_INCLUDED:
                                        case OVERLAP:
                                        case SIMULTANEOUS:
                                        case DURING:
                                        case AFTER:
                                        case OVERLAP_OR_AFTER:
                                        case BEGUN_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId1(), TLink.Type.BEFORE);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                    }
                                    break;
                                case IS_INCLUDED:
                                case SIMULTANEOUS:
                                case DURING:
                                case OVERLAP: {
                                    switch (tt.getRelation()) {
                                        case IS_INCLUDED:
                                        case SIMULTANEOUS:
                                        case DURING:
                                        case OVERLAP: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId1(), TLink.Type.IS_INCLUDED);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                        case BEFORE:
                                        case BEFORE_OR_OVERLAP:
                                        case ENDED_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId1(), TLink.Type.BEFORE);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                        case AFTER:
                                        case OVERLAP_OR_AFTER:
                                        case BEGUN_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId1(), TLink.Type.BEFORE);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                    }
                                }
                                case AFTER:
                                case OVERLAP_OR_AFTER:
                                case BEGUN_BY: {
                                    switch (tt.getRelation()) {
                                        case IS_INCLUDED:
                                        case OVERLAP:
                                        case SIMULTANEOUS:
                                        case DURING:
                                        case BEFORE:
                                        case BEFORE_OR_OVERLAP:
                                        case ENDED_BY: {
                                            EventTimeLink newET = new EventTimeLink(et.getId1(), tt.getId1(), TLink.Type.AFTER);
                                            proposed.add(newET);
                                            combined.add(newET);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // deduce EE links from ET links
        for (int i = 0; i < combined.size(); i++) {
            TLink et1 = combined.get(i);
            if (et1 instanceof EventTimeLink) {
                for (int i1 = 0; i1 < combined.size(); i1++) {
                    TLink et2 = combined.get(i1);
                    if (et2 != et1 && et2 instanceof EventTimeLink &&
                            et1.getId2().equals(et2.getId2())) {
                        if (containsEELink(combined, et1.getId1(), et2.getId1())) continue;
                        switch (et1.getRelation()) {
                            case BEFORE:
                            case BEFORE_OR_OVERLAP:
                            case ENDED_BY: {
                                switch (et2.getRelation()) {
                                    case IS_INCLUDED:
                                    case DURING:
                                    case SIMULTANEOUS:
                                    case OVERLAP:
                                    case AFTER:
                                    case OVERLAP_OR_AFTER:
                                    case BEGUN_BY: {
                                        EventEventLink newEEL = new EventEventLink(et1.getId1(), et2.getId1(), TLink.Type.BEFORE);
                                        proposed.add(newEEL);
                                        combined.add(newEEL);
                                        break;
                                    }
                                }
                                break;
                            }
                            case IS_INCLUDED:
                            case OVERLAP:
                            case DURING:
                            case SIMULTANEOUS: {
                                switch (et2.getRelation()) {
                                    case BEFORE:
                                    case BEFORE_OR_OVERLAP:
                                    case ENDED_BY: {
                                        EventEventLink newEEL = new EventEventLink(et1.getId1(), et2.getId1(), TLink.Type.AFTER);
                                        proposed.add(newEEL);
                                        combined.add(newEEL);
                                        break;
                                    }
                                    case IS_INCLUDED:
                                    case OVERLAP:
                                    case SIMULTANEOUS:
                                    case DURING: {
                                        EventEventLink newEEL = new EventEventLink(et1.getId1(), et2.getId1(), TLink.Type.IS_INCLUDED);
                                        proposed.add(newEEL);
                                        combined.add(newEEL);
                                        break;
                                    }
                                    case AFTER:
                                    case OVERLAP_OR_AFTER:
                                    case BEGUN_BY: {
                                        EventEventLink newEEL = new EventEventLink(et1.getId1(), et2.getId1(), TLink.Type.BEFORE);
                                        proposed.add(newEEL);
                                        combined.add(newEEL);
                                        break;
                                    }
                                }
                                break;
                            }
                            case AFTER:
                            case OVERLAP_OR_AFTER:
                            case BEGUN_BY: {
                                switch (et2.getRelation()) {
                                    case IS_INCLUDED:
                                    case OVERLAP:
                                    case DURING:
                                    case SIMULTANEOUS:
                                    case BEFORE:
                                    case BEFORE_OR_OVERLAP:
                                    case ENDED_BY: {
                                        EventEventLink newEEL = new EventEventLink(et1.getId1(), et2.getId1(), TLink.Type.AFTER);
                                        proposed.add(newEEL);
                                        combined.add(newEEL);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        return proposed;
    }

    private boolean containsEELink(List<TLink> combined, String event1Id, String event2Id) {
        for (TLink link : combined) {
            if (link instanceof EventEventLink && (
                    link.getId1().equals(event1Id) && link.getId2().equals(event2Id)) || (
                    link.getId2().equals(event2Id) && link.getId1().equals(event2Id))) {
                switch (link.getRelation()) {
                    case VAGUE:
                    case UNKNOWN:
                    case NONE:
                        return false;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    private boolean containsETLink(List<TLink> combined, String eventId, String timeId) {
        for (TLink link : combined) {
            if (link instanceof EventTimeLink && link.getId1().equals(eventId) &&
                    link.getId2().equals(timeId)) {
                switch (link.getRelation()) {
                    case VAGUE:
                    case NONE:
                    case UNKNOWN:
                        return false;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public void train(SieveDocuments infoDocs) {
        return;
    }
}
