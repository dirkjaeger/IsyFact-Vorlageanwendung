package de.msg.terminfindung.gui.awkwrapper.impl;

/*
 * #%L
 * Terminfindung
 * %%
 * Copyright (C) 2015 - 2016 Bundesverwaltungsamt (BVA), msg systems ag
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import de.msg.terminfindung.common.exception.TerminfindungBusinessException;
import de.msg.terminfindung.common.konstanten.FehlerSchluessel;
import de.msg.terminfindung.core.erstellung.Erstellung;
import de.msg.terminfindung.core.teilnahme.Teilnahme;
import de.msg.terminfindung.gui.awkwrapper.AwkWrapper;
import de.msg.terminfindung.gui.terminfindung.model.*;
import de.msg.terminfindung.persistence.entity.*;
import org.apache.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementierung des Anwendungskern-Wrappers.
 *
 * @author Dirk Jäger
 */
@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
public class AwkWrapperImpl implements AwkWrapper {

	private static final Logger LOG = Logger.getLogger(AwkWrapperImpl.class);
	
	/**
	 * Die Komponente des Anwendungskerns für die Erstellung von Terminfindungen 
	 */
	private Erstellung erstellung;
	/**
	 * Die Komponente des Anwendungskerns für die Teilnahme an Terminfindungen
	 */
	private Teilnahme teilnahme;
	/**
	 * Der Bean-Mapper für die Abbildung zwischen View-Objekten und Persistenz-Objekten
	 */
	private Mapper beanMapper;
	

	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#erstelleTerminfindung(java.lang.String, java.lang.String, java.util.List)
	 */
	public ViewTerminfindung erstelleTerminfindung(String organisatorName,
			String veranstaltungsName, List<ViewTag> tage) throws TerminfindungBusinessException {

		List<Tag> termine = new ArrayList<>();
		for (ViewTag tag : tage) {

			Tag termin = new Tag(tag.getDatum());

			List<Zeitraum> zeitraeume = new ArrayList<>();
			for (int i = 0; i < 3; i++) {

				Zeitraum zeitraum = new Zeitraum(tag.getZeitraeume().get(i).getBeschreibung());
				zeitraeume.add(zeitraum);
				
			}
			termin.setZeitraeume(zeitraeume);
			termine.add(termin);
		}		
		
		Terminfindung terminfindung = erstellung.erstelleTerminfindung(organisatorName, veranstaltungsName, termine);

		//gib die Terminfindung als Ergebnis zurück
		return map(terminfindung);		
	}
		
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#ladeTerminfindung(long)
	 */
	public ViewTerminfindung ladeTerminfindung(long terminfindungsNr) throws TerminfindungBusinessException {

		Terminfindung tf = erstellung.leseTerminfindung(terminfindungsNr);
		return map(tf);	
	}

	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#setzeVeranstaltungstermin(de.msg.terminfindung.gui.terminfindung.model.ViewTerminfindung, long)
	 */
	public ViewTerminfindung setzeVeranstaltungstermin (ViewTerminfindung viewTerminfindung, long zeitraumNr) throws TerminfindungBusinessException  {
		
		if (viewTerminfindung == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);
		
		Terminfindung terminfindung = erstellung.leseTerminfindung(viewTerminfindung.getTerminfnd_Nr());

		erstellung.setzeVeranstaltungstermin (terminfindung, zeitraumNr);
		
		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}
	
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#bestaetigeTeilnahme(de.msg.terminfindung.gui.terminfindung.model.ViewTerminfindung, de.msg.terminfindung.gui.terminfindung.model.ViewTeilnehmer, java.util.Map)
	 */
	public ViewTerminfindung bestaetigeTeilnahme(ViewTerminfindung viewTerminfindung, ViewTeilnehmer viewTeilnehmer, Map<ViewZeitraum, ViewPraeferenz> viewTerminwahl) throws TerminfindungBusinessException  {
		
		if (viewTerminfindung == null || viewTeilnehmer == null || viewTerminwahl == null) 
			throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

		// Übertrage die Datenstrukturen aus dem View in die Struktur des Anwendungskerns
		// Lese die Terminfindung anhand ihrer Id
		Terminfindung terminfindung = erstellung.leseTerminfindung(viewTerminfindung.getTerminfnd_Nr());
	
		// Der Teilnehmer wird neu erzeugt, der Name wird übertragen
		Teilnehmer teilnehmer = new Teilnehmer();
		teilnehmer.setName(viewTeilnehmer.getName());
		
		// Suche in den gegebenen Zeiträumen der Terminwahl nach den IDs der Zeiträume, die in der Map übergeben wurden
		// Konstruiere daraus die entsprechende Map für den Aufruf des Anwendungskerns
		Map<Zeitraum, Praeferenz> terminwahl = new HashMap <>();
		for (ViewZeitraum viewZeitraum : viewTerminwahl.keySet()) {

			Zeitraum zeitraum = terminfindung.findeZeitraumById(viewZeitraum.getZeitraum_nr());

			// Wenn in der Terminfindung kein Zeitraum mit der gesuchten Id exisistiert ist die Anfrage ungültig
			if (zeitraum == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

			// Bilder den View-Präferenzwert auf den entsprechenden Persistenz-Präferenzwert ab und speichere
			Praeferenz praeferenz = map(viewTerminwahl.get(viewZeitraum));
			terminwahl.put(zeitraum, praeferenz);
		}
		// rufe den Anwendungskern auf
		teilnahme.bestaetigeTeilnahme(terminfindung, teilnehmer, terminwahl);
		
		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}

	
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#loescheZeitraum(de.msg.terminfindung.gui.terminfindung.model.ViewTerminfindung, de.msg.terminfindung.gui.terminfindung.model.ViewZeitraum)
	 */
	public ViewTerminfindung loescheZeitraeume(ViewTerminfindung viewTerminfindung, List<ViewZeitraum> viewZeitraeume) throws TerminfindungBusinessException{

		if (viewTerminfindung == null || viewZeitraeume == null )
			throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

		// Übertrage die Datenstrukturen aus dem View in die Struktur des Anwendungskerns
		// Lese die Terminfindung anhand ihrer Id, Konstruiere die entsprechende Liste für den Aufruf des
		// Anwendungskerns
		List<Zeitraum> zeitraumList = new ArrayList<>();
		Terminfindung terminfindung = erstellung.leseTerminfindung(viewTerminfindung.getTerminfnd_Nr());

		// Hole zu jedem zu löschenden Zeitraum das entsprechende Objekt des Anwendungskerns
		for (ViewZeitraum viewZeitraum : viewZeitraeume) {

			Zeitraum zeitraum = terminfindung.findeZeitraumById(viewZeitraum.getZeitraum_nr());
			// Wenn in der Terminfindung kein Zeitraum mit der gesuchten Id exisistiert ist die Anfrage ungültig
			if (zeitraum == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

			zeitraumList.add(zeitraum);
		}

		// rufe den Anwendungskern auf
		erstellung.loescheZeitraeume(terminfindung, zeitraumList);

		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}

	/**
	 * Kapselt das Mapping zwischen dem Persistenz-Objekt und dem View-Objekt
	 * einer Terminfindung.
	 * Die Methode verwendet intern Dozer für das eigentlichen Mapping.
	 * Zusätzlich werden weitere Datenstrukturen im Zielobjekt initialisiert.
	 * 
	 * @param terminfindung Das Persistenz-Objekt der Terminfindung
	 * @return Das View-Objekt der Terminfindung
	 */
	private ViewTerminfindung map(Terminfindung terminfindung) {

		ViewTerminfindung viewTerminfindung = beanMapper.map(terminfindung, ViewTerminfindung.class);
		initialisierePraeferenzenFuerTeilnehmer(viewTerminfindung);
		return viewTerminfindung;	
	}

	/**
	 * Bildet einen View-Präferenzwert auf einen Persistenz-Präferenzwert ab.
	 * Die Abbildung ist trivial, sie dient aber dazu die Modelle wirksam zu entkoppeln.
	 * Das die numerische Codierung des Enums sowohl in der Datenbank abgelegt wird,
	 * als auch im GUI sichbar ist, scheint es sinnvoll, hier ein Mapping einzuführen,
	 * das sich nicht auf diese Codierung verlässt, sondern einzelne Werte explizit einander
	 * zuordnet. Auf eine Implementierung über Dozer wurde verzichtet, da hierfür
	 * ebenfalls eigener Mapping Code erforderlich wäre (Einzelne Enum-Werte können von Dozer
	 * standardmäßig nicht abgebildet werden.)
	 *
	 * @param viewPraeferenz der View-Präferenzwert
	 * @return der Persistenz-Präferenzwert
	 * @throws TerminfindungBusinessException Wird bei unbekannten View-Präferenzewerten erzeugt
	 */
	private Praeferenz map (ViewPraeferenz viewPraeferenz) throws TerminfindungBusinessException{

		switch (viewPraeferenz) {
			case JA: return Praeferenz.JA;
			case NEIN : return Praeferenz.NEIN;
			case WENN_ES_SEIN_MUSS : return Praeferenz.WENN_ES_SEIN_MUSS;
			default: throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);
		}
	}

	/**
	 * Stellt die Rückwärtsverkettung von Teilnehmern zu ihren Praeferenzen her.
	 * TODO: Das sollte im Persistenzmodell abgebildet werden, dann kann diese
	 * Methode entfallen.
	 * 
	 * @param vtf Die Terminfindung
	 */
	private void initialisierePraeferenzenFuerTeilnehmer(ViewTerminfindung vtf) {
		
		for (ViewZeitraum zeitraum : vtf.getAlleZeitraeume()) {
			for (ViewTeilnehmerZeitraum vtz : zeitraum.getTeilnehmerZeitraeume()) {
				vtz.getTeilnehmer().getPraeferenzen().add(vtz);
			}
		}
	}

	/*--------------------------------------------------------------------------
	 *  Getter und Setter
	 *--------------------------------------------------------------------------*/
	
	public Teilnahme getTeilnahme() {
		return teilnahme;
	}

	public void setTeilnahme(Teilnahme teilnahme) {
		this.teilnahme = teilnahme;
	}

	public Mapper getBeanMapper() {
		return beanMapper;
	}

	public void setBeanMapper(Mapper beanMapper) {
		this.beanMapper = beanMapper;
	}

	public Erstellung getErstellung() {
		return erstellung;
	}

	public void setErstellung(Erstellung erstellung) {
		this.erstellung = erstellung;
	}
}
