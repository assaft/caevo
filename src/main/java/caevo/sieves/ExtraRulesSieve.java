package caevo.sieves;

import caevo.*;
import caevo.tlink.EventEventLink;
import caevo.tlink.EventTimeLink;
import caevo.tlink.TLink;
import caevo.tlink.TimeTimeLink;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TypedDependency;

import java.util.ArrayList;
import java.util.List;

public class ExtraRulesSieve implements Sieve {
    @Override
    public List<TLink> annotate(SieveDocument doc, List<TLink> currentTLinks) {
        List<TLink> proposed = new ArrayList<TLink>();

        for (SieveSentence sent : doc.getSentences()) {

            if (sent.timexes().isEmpty() && sent.events().size() > 1) {
                TextEvent previousEvent = null;
                // no time expressions in the sentence, only events
                for (TextEvent event : sent.events()) {
                    if (previousEvent == null) {
                        // first event
                        previousEvent = event;
                    } else {
                        // is this event a dependent of the previous event?
                        for (TypedDependency dependency : sent.getDeps()) {
                            if (dependency.gov().index() == previousEvent.getIndex() &&
                                    dependency.dep().index() == event.getIndex() &&
                                    dependency.reln().getShortName().startsWith("conj")) {
                                // conjunction of events -> earlier event predates later one
                                proposed.add(new EventEventLink(previousEvent.getEiid(), event.getEiid(), TLink.Type.BEFORE));
                            }
                        }
                    }
                }
                continue;
            }
            // timex-event links
            for (Timex timex : sent.timexes()) {
                TextEvent previousEvent = null;
                int timexEndIx = timex.getTokenOffset() + timex.getTokenLength(); // timex ends here
                int eventIx = -1;
                for (TextEvent event : sent.events()) {
                    eventIx++;
                    if (event.getIndex() > timexEndIx) {
                        // event after timex
                        // timex-IN-event ("2 hours after the accident")
                        TLink.Type type = eventPrepTimexType(sent, event);
                        if (type != null) {
                            proposed.add(new EventTimeLink(event.getEiid(), timex.getTid(), type));
                            // check to see if another event governs the current one as an adverbial clause ("2 hours after the accident, [event2]")
                            if (eventIx < sent.events().size() - 1) {
                                // at least one more event -> look for an adverbial clause
                                TextEvent nextEvent = getAdvClauseEvent(sent, event);
                                if (nextEvent != null) {
                                    proposed.add(new EventTimeLink(nextEvent.getEiid(), timex.getTid(), TLink.invertRelation(type)));
                                }
                            }
                        }
                    } else {
                        // event before timex
                        // event-timex-RB/IN ("She started, and I came 2 hours after.") - preposition follows timex, modifies a previous event
                        int lastTokenIX = getLastTokenIx(sent);
                        String lastPos = sent.tokens().get(lastTokenIX).getString(CoreAnnotations.PartOfSpeechAnnotation.class);
                        if (lastPos.equalsIgnoreCase("RB") || lastPos.equalsIgnoreCase("IN")) {
                            TLink.Type type = getEventTimeReversedRelation(sent.tokens().get(lastTokenIX).word());
                            if (type != null) {
                                if (sent.events().size() == 2) {
                                    // multi-event sentence. todo: what happens in a sentence with more than two events? more than one timex?
                                    if (previousEvent == null) {
                                        // reference to first event, will be handled in next iteration
                                        previousEvent = event;
                                    } else {
                                        // add tlinks for both events. The timex is between the events, so if one is before,
                                        // the other is after and vice versa.
                                        proposed.add(new EventTimeLink(previousEvent.getEiid(), timex.getTid(), type));
                                        proposed.add(new EventTimeLink(event.getEiid(), timex.getTid(), TLink.invertRelation(type)));
                                        proposed.add(new EventEventLink(previousEvent.getEiid(), event.getEiid(), type));
                                        String previousEventTimexId = findPreviousEventTimexId(previousEvent, currentTLinks);
                                        if (previousEventTimexId != null) {
                                            // previous event is already linked to another timex, safe to link current timex
                                            // to previous timex with same link type.
                                            proposed.add(new TimeTimeLink(previousEventTimexId, timex.getTid(), type));
                                        }
                                        break;
                                    }
                                } else if (sent.events().size() == 1) {
                                    // single event sentence
                                    // if the timex is after the event, the event is before the timex - need to reverse the type
                                    proposed.add(new EventTimeLink(event.getEiid(), timex.getTid(), TLink.invertRelation(type)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return proposed;
    }

    private TextEvent getAdvClauseEvent(SieveSentence sent, TextEvent event) {
        for (TypedDependency typedDependency : sent.getDeps()) {
            if (typedDependency.dep().index() == event.getIndex() && typedDependency.reln().equals(GrammaticalRelation.valueOf("advcl"))) {
                int govIx = typedDependency.gov().index();
                for (TextEvent eve : sent.events()) {
                    if (eve.getIndex() == govIx) {
                        return eve;
                    }
                }
            }
        }
        return null;
    }

    private TLink.Type eventPrepTimexType(SieveSentence sent, TextEvent event) {
        for (TypedDependency typedDependency : sent.getDeps()) {
            if (typedDependency.gov().index() == event.getIndex()) {
                int depIx = typedDependency.dep().index();
                // remember, tokens() is 0-based, but token offset is 1-based
                String pos = sent.tokens().get(depIx - 1).getString(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (pos.equalsIgnoreCase("IN")) {
                    return getEventTimeReversedRelation(sent.tokens().get(depIx - 1).word());
                }
            }
        }
        return null;
    }

    private String findPreviousEventTimexId(TextEvent previousEvent, List<TLink> currentTLinks) {
        for (TLink link : currentTLinks) {
            if (link instanceof EventTimeLink && link.getId1().equals(previousEvent.getEiid())) {
                return link.getId2();
            }
        }
        return null;
    }

    private int getLastTokenIx(SieveSentence sent) {
        String lastToken = sent.tokens().get(sent.tokens().size() - 1).word();
        if (lastToken.equals("."))
            return sent.tokens().size() - 2;
        return sent.tokens().size() - 1;
    }

    private TLink.Type getEventTimeReversedRelation(String prep) {
        // relations are is the reverse of the preposition, since the text structure is not event-preposition-timex
        if ("in".equalsIgnoreCase(prep) || "on".equalsIgnoreCase(prep) || "during".equalsIgnoreCase(prep) || "over".equalsIgnoreCase(prep) || "within".equalsIgnoreCase(prep)) {
            return TLink.Type.IS_INCLUDED;
        } else if ("for".equalsIgnoreCase(prep) || "at".equalsIgnoreCase(prep) || "throughout".equalsIgnoreCase(prep)) {
            return TLink.Type.SIMULTANEOUS;
        } else if ("until".equalsIgnoreCase(prep) || "before".equalsIgnoreCase(prep)) {
            return TLink.Type.AFTER;
        } else if ("from".equalsIgnoreCase(prep) || "after".equalsIgnoreCase(prep)) {
            return TLink.Type.BEFORE;
        } else if ("through".equalsIgnoreCase(prep)) {
            return TLink.Type.OVERLAP_OR_AFTER;
        }
        return null;
    }

    @Override
    public void train(SieveDocuments infoDocs) {
        // no training - all rules
    }
}